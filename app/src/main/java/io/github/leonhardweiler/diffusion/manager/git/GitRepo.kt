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

/** The one remote this app knows about. */
private const val REMOTE = Constants.DEFAULT_REMOTE_NAME

/**
 * The remote and the local side changed the same lines. Both versions are now in
 * the notes, between markers, for the user to edit down.
 */
internal class MergeConflictException(val paths: List<String>) :
    Exception("merge conflict in: ${paths.joinToString(", ")}")

/**
 * A note left from such a conflict still holds the markers. Committing it would
 * write them into the history, so the sync stops instead.
 */
internal class UnresolvedConflictException(val paths: List<String>) :
    Exception(paths.joinToString(", "))

/** Anything else this layer refuses to do, in words the user is shown. */
internal class GitOperationException(message: String) : Exception(message)

/**
 * Opens the repository standing at [path], and nothing else: the folder has to
 * be one already, the way the setup left it.
 */
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

    // Everything that just landed carries the time of the checkout. Left that
    // way, a repository of years of notes would read as written this minute.
    runCatching { applyCommitTimestamps(git) }
        .onFailure { Log.e(TAG, "applyCommitTimestamps", it) }

    return git
}

/**
 * The branch the working tree stands on. A detached HEAD is not one, and this
 * app has no way to put the user back onto a branch.
 */
internal fun currentBranch(repo: Repository): String {
    val full = repo.fullBranch
    if (full == null || !full.startsWith(Constants.R_HEADS)) {
        throw GitOperationException("unable to determine default branch")
    }

    return full.removePrefix(Constants.R_HEADS)
}

/**
 * Points the repository at [url], adding the remote if it has none.
 *
 * Written into the repository itself, because that is where push and pull look
 * for it — what the app stores in its preferences is a copy for the settings
 * screen, not the thing git reads.
 */
internal fun setRemoteUrl(git: Git, url: String) {
    val uri = URIish(url)

    if (REMOTE in git.repository.config.getSubsections("remote")) {
        git.remoteSetUrl().setRemoteName(REMOTE).setRemoteUri(uri).call()
    } else {
        git.remoteAdd().setName(REMOTE).setUri(uri).call()
    }
}

/**
 * The url the repository pushes to and pulls from, as it is configured. Null when
 * the repository has no remote at all, which a purely local one has not.
 */
internal fun remoteUrl(repo: Repository): String? =
    repo.config.getString("remote", REMOTE, "url")

/** What HEAD points at, or null for a repository that has no commit yet. */
internal fun lastCommit(repo: Repository): String? = repo.resolve(Constants.HEAD)?.name

/**
 * Who this repository says its notes are written by: what is configured, and
 * failing that whoever wrote the commit HEAD stands on.
 */
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

/**
 * Whether the working tree holds anything the repository has not been told about
 * — a note written, renamed or deleted since the last sync.
 */
internal fun isChange(git: Git): Boolean = !git.status().call().isClean

internal fun commitAll(git: Git, author: GitAuthor, fallbackMessage: String) {
    val repo = git.repository

    // A conflict is fixed by editing the note, and the sync after that is what
    // ends the merge. Nothing says the note was edited, though, and this commits
    // the working tree as it stands — so without asking, closing the app would be
    // enough to write "<<<<<<<" and both versions into the history, and the
    // automatic sync means it takes no tap at all.
    val unresolved = unresolvedConflicts(repo)
    if (unresolved.isNotEmpty()) {
        Log.e(TAG, "conflict markers still in: ${unresolved.joinToString(", ")}")
        throw UnresolvedConflictException(unresolved)
    }

    // Takes the working tree as it stands, conflicted paths included: a note
    // whose markers the user edited away is thereby resolved, which is what ends
    // the merge. Twice, because the first pass writes what is there and the
    // second is the only one that records what is gone.
    git.add().addFilepattern(".").call()
    git.add().addFilepattern(".").setUpdate(true).call()

    // Read off the index rather than the working tree: what is about to be
    // committed is exactly what was just staged.
    val message = commitMessage(repo, fallbackMessage)

    git.commit()
        .setIdent(author)
        .setMessage(message)
        .setAllowEmpty(true)
        .call()
}

/**
 * Who the commit is by, and never nobody.
 *
 * Here rather than where the author comes from, because this is where it
 * reaches git: an empty name or address is written as `author  <>` without a
 * word of complaint, and the repository it lands in is shared with other people
 * and other devices. See [GitAuthor.orFallback].
 */
private fun CommitCommand.setIdent(author: GitAuthor): CommitCommand =
    author.orFallback().let { setAuthor(it.name, it.email).setCommitter(it.name, it.email) }

internal fun push(git: Git, cred: Cred?) {
    val branch = currentBranch(git.repository)

    // A branch with no commit on it is nothing to push, and saying so is the
    // whole of it: the refspec below names a ref that resolves to nothing, and
    // JGit answers that with "Source ref refs/heads/main doesn't resolve to any
    // object" — a failed sync for a repository that is simply still empty. A
    // folder that was `git init`ed and never committed to is one, and so is a
    // clone of a repository nobody has pushed to yet, right up until the first
    // note is written. The sync that follows the first commit is what puts the
    // branch on the remote.
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

    // A push that was refused comes back as a result, not as a failure: without
    // reading it, a remote that says no leaves the button saying the notes went
    // out.
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

    // What the working tree stood at before anything came in. The dating below is
    // the only reason it is kept.
    val before = repo.resolve(Constants.HEAD)

    // Every branch rather than the one being merged: a refspec naming a branch
    // the remote does not have yet is refused outright, and a repository that was
    // set up here before its remote had anything in it is exactly that case.
    git.fetch()
        .setRemote(REMOTE)
        .setRefSpecs(RefSpec("+${Constants.R_HEADS}*:${Constants.R_REMOTES}$REMOTE/*"))
        .setTagOpt(TagOpt.NO_TAGS)
        .setTransportConfigCallback(SshTransportConfig(cred))
        .setTimeout(NETWORK_TIMEOUT_SECONDS)
        .call()

    val fetched = repo.resolve("${Constants.R_REMOTES}$REMOTE/$branch")

    if (fetched == null) {
        // The remote has no such branch, so there is nothing to merge. The push
        // that follows is what puts it there.
        Log.d(TAG, "pull: the remote has no $branch yet")
        return
    }

    merge(git, branch, fetched, author)

    // The merge checked out whatever came in, which dates those notes to now
    // rather than to when they were written on the other device.
    runCatching { datePulledNotes(git, before) }
        .onFailure { Log.e(TAG, "datePulledNotes", it) }
}

private fun merge(git: Git, branch: String, fetched: ObjectId, author: GitAuthor) {
    val repo = git.repository

    if (repo.resolve(Constants.HEAD) == null) {
        // Pulling into a repository that has no commit of its own: there is
        // nothing to merge with, so the branch is simply pointed at what came in.
        Log.d(TAG, "pull: nothing here yet, taking $branch as it is")
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(fetched.name).call()
        return
    }

    // Committed separately below, so that the merge carries the author the app
    // was set up with rather than whatever JGit would make up for this device.
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

        // Each conflicted note now holds both versions between markers, which is
        // something that can be read and fixed in the editor. The merge stays
        // open until that is committed, so finishing it does not throw away where
        // the other side came from and fetch the same conflict for ever after.
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

/**
 * The commit that ends a merge, naming both sides.
 *
 * JGit reads MERGE_HEAD for the second parent itself and clears it afterwards,
 * which is what takes the repository out of the merging state.
 */
private fun commitMerge(git: Git, fetched: ObjectId, author: GitAuthor) {
    val head = git.repository.resolve(Constants.HEAD)

    git.commit()
        .setIdent(author)
        .setMessage("Merge: ${fetched.name} into ${head?.name}")
        .setAllowEmpty(true)
        .call()
}
