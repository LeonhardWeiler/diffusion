package io.github.leonhardweiler.diffusion.manager.git

import android.util.Log
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.transport.URIish
import java.io.File

private const val TAG = "GitRepo"

private const val REMOTE = Constants.DEFAULT_REMOTE_NAME

internal class MergeConflictException(val paths: List<String>) :
    Exception("merge conflict in: ${paths.joinToString(", ")}")

internal class UnresolvedConflictException(val paths: List<String>) :
    Exception(paths.joinToString(", "))

internal class GitOperationException(message: String) : Exception(message)

internal fun openRepository(path: File): Git {
    val repo = FileRepositoryBuilder()
        .setGitDir(File(path, Constants.DOT_GIT))
        .setWorkTree(path)
        .setMustExist(true)
        .build()

    return Git(repo)
}

internal fun cloneRepository(
    path: File,
    remoteUrl: String,
    cred: Cred?,
    onProgress: (Int) -> Boolean,
): Git {
    val git = Git.cloneRepository()
        .setURI(remoteUrl)
        .setDirectory(path)
        .setNoTags()
        .setTransportConfigCallback(SshTransportConfig(cred))
        .setTimeout(NETWORK_TIMEOUT_SECONDS)
        .setProgressMonitor(CloneProgress(onProgress))
        .call()

    runCatching { applyCommitTimestamps(git) }
        .onFailure { Log.e(TAG, "applyCommitTimestamps", it) }

    return git
}

internal fun currentBranch(repo: Repository): String {
    val full = repo.fullBranch
    if (full == null || !full.startsWith(Constants.R_HEADS)) {
        throw GitOperationException("unable to determine default branch")
    }

    return full.removePrefix(Constants.R_HEADS)
}

internal fun setRemoteUrl(git: Git, url: String) {
    val uri = URIish(url)

    if (REMOTE in git.repository.config.getSubsections("remote")) {
        git.remoteSetUrl().setRemoteName(REMOTE).setRemoteUri(uri).call()
    } else {
        git.remoteAdd().setName(REMOTE).setUri(uri).call()
    }
}

internal fun remoteUrl(repo: Repository): String? =
    repo.config.getString("remote", REMOTE, "url")

internal fun lastCommit(repo: Repository): String? = repo.resolve(Constants.HEAD)?.name

internal fun signature(repo: Repository): Pair<String, String>? {
    val name = repo.config.getString("user", null, "name").orEmpty()
    val email = repo.config.getString("user", null, "email").orEmpty()

    if (name.isNotEmpty() || email.isNotEmpty()) return name to email

    val head = repo.resolve(Constants.HEAD) ?: return null

    return RevWalk(repo).use { walk ->
        val author = walk.parseCommit(head).authorIdent
        author.name.orEmpty() to author.emailAddress.orEmpty()
    }
}

internal fun isChange(git: Git): Boolean = !git.status().call().isClean

internal fun commitAll(git: Git, author: GitAuthor, fallbackMessage: String) {
    val repo = git.repository

    val unresolved = unresolvedConflicts(repo)
    if (unresolved.isNotEmpty()) {
        Log.e(TAG, "conflict markers still in: ${unresolved.joinToString(", ")}")
        throw UnresolvedConflictException(unresolved)
    }

    // the first add records what is there, the second what is gone
    git.add().addFilepattern(".").call()
    git.add().addFilepattern(".").setUpdate(true).call()

    val message = commitMessage(repo, fallbackMessage)

    git.commit()
        .setIdent(author)
        .setMessage(message)
        .setAllowEmpty(true)
        .call()
}

private fun CommitCommand.setIdent(author: GitAuthor): CommitCommand =
    author.orFallback().let { setAuthor(it.name, it.email).setCommitter(it.name, it.email) }

internal fun push(git: Git, cred: Cred?) {
    val branch = currentBranch(git.repository)

    if (git.repository.resolve("${Constants.R_HEADS}$branch") == null) {
        Log.d(TAG, "push: nothing on $branch to push yet")
        return
    }

    val results = git.push()
        .setRemote(REMOTE)
        .setRefSpecs(RefSpec("${Constants.R_HEADS}$branch:${Constants.R_HEADS}$branch"))
        .setTransportConfigCallback(SshTransportConfig(cred))
        .setTimeout(NETWORK_TIMEOUT_SECONDS)
        .call()

    // a remote saying no comes back as a result rather than as a throw
    val refused = results
        .flatMap { it.remoteUpdates }
        .filter { it.status != RemoteRefUpdate.Status.OK && it.status != RemoteRefUpdate.Status.UP_TO_DATE }

    if (refused.isNotEmpty()) {
        throw GitOperationException(
            refused.joinToString(", ") { update ->
                update.message ?: update.status.name.lowercase().replace('_', ' ')
            }
        )
    }
}

internal fun pull(git: Git, cred: Cred?, author: GitAuthor) {
    val repo = git.repository
    val branch = currentBranch(repo)

    val before = repo.resolve(Constants.HEAD)

    git.fetch()
        .setRemote(REMOTE)
        .setRefSpecs(RefSpec("+${Constants.R_HEADS}*:${Constants.R_REMOTES}$REMOTE/*"))
        .setTagOpt(TagOpt.NO_TAGS)
        .setTransportConfigCallback(SshTransportConfig(cred))
        .setTimeout(NETWORK_TIMEOUT_SECONDS)
        .call()

    val fetched = repo.resolve("${Constants.R_REMOTES}$REMOTE/$branch")

    if (fetched == null) {
        Log.d(TAG, "pull: the remote has no $branch yet")
        return
    }

    merge(git, branch, fetched, author)

    runCatching { datePulledNotes(git, before) }
        .onFailure { Log.e(TAG, "datePulledNotes", it) }
}

private fun merge(git: Git, branch: String, fetched: ObjectId, author: GitAuthor) {
    val repo = git.repository

    if (repo.resolve(Constants.HEAD) == null) {
        Log.d(TAG, "pull: nothing here yet, taking $branch as it is")
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(fetched.name).call()
        return
    }

    // setCommit(false) leaves MERGE_HEAD behind, which is what lets the commit
    // below carry this app's author and still name both parents
    val result = git.merge()
        .include(fetched)
        .setCommit(false)
        .setFastForward(MergeCommand.FastForwardMode.FF)
        .call()

    when (result.mergeStatus) {
        MergeResult.MergeStatus.ALREADY_UP_TO_DATE,
        MergeResult.MergeStatus.FAST_FORWARD,
        MergeResult.MergeStatus.FAST_FORWARD_SQUASHED,
            -> Unit

        MergeResult.MergeStatus.MERGED,
        MergeResult.MergeStatus.MERGED_NOT_COMMITTED,
            -> commitMerge(git, fetched, author)

        MergeResult.MergeStatus.CONFLICTING -> {
            val paths = result.conflicts?.keys?.sorted() ?: conflictedPaths(repo)
            Log.e(TAG, "merge conflict in: ${paths.joinToString(", ")}")
            throw MergeConflictException(paths)
        }

        else -> throw GitOperationException(
            "merge ${result.mergeStatus}" +
                    result.checkoutConflicts.orEmpty().joinToString(
                        prefix = ": ", separator = ", "
                    ).takeIf { result.checkoutConflicts.orEmpty().isNotEmpty() }.orEmpty()
        )
    }
}

private fun commitMerge(git: Git, fetched: ObjectId, author: GitAuthor) {
    val head = git.repository.resolve(Constants.HEAD)

    git.commit()
        .setIdent(author)
        .setMessage("Merge: ${fetched.name} into ${head?.name}")
        .setAllowEmpty(true)
        .call()
}
