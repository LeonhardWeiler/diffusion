package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.manager.git.GitEnvironment
import io.github.leonhardweiler.diffusion.manager.git.MergeConflictException
import io.github.leonhardweiler.diffusion.manager.git.UnresolvedConflictException
import io.github.leonhardweiler.diffusion.manager.git.applyCommitTimestamps
import io.github.leonhardweiler.diffusion.manager.git.cloneRepository
import io.github.leonhardweiler.diffusion.manager.git.commitAll
import io.github.leonhardweiler.diffusion.manager.git.isChange
import io.github.leonhardweiler.diffusion.manager.git.lastCommit
import io.github.leonhardweiler.diffusion.manager.git.openRepository
import io.github.leonhardweiler.diffusion.manager.git.pull
import io.github.leonhardweiler.diffusion.manager.git.push
import io.github.leonhardweiler.diffusion.manager.git.remoteUrl
import io.github.leonhardweiler.diffusion.manager.git.setRemoteUrl
import io.github.leonhardweiler.diffusion.manager.git.signature
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


enum class GitExceptionType {
    RepoAlreadyInit,
    RepoNotInit,

    /**
     * A pull that could not be merged. The notes it could not merge now hold
     * both versions between markers, so the caller has to read the files again
     * even though HEAD has not moved.
     */
    MergeConflict,

    /**
     * A note that came out of such a conflict still holds the markers.
     * Committing it would write them into the history, so the sync stops and
     * says which note has to be edited first.
     */
    UnresolvedConflict,

    /**
     * The far end was never reached: no name to resolve, no route, no socket.
     * Told apart from the rest because it is what an automatic sync runs into
     * all the time — the app is opened and left exactly when a phone comes back
     * from sleep — and that is not something to put an error on a button for.
     */
    NetworkUnreachable,
    Other
}

class GitException(
    val type: GitExceptionType,
    message: String?
) : Exception(getMessage(type, message)) {

    companion object {
        private fun getMessage(
            type: GitExceptionType,
            message: String?
        ): String? =
            message ?: if (type != GitExceptionType.Other) type.name else null

    }

    constructor(message: String) : this(GitExceptionType.Other, message)
    constructor(type: GitExceptionType) : this(type, null)
}

/**
 * Everything this app does to a git repository.
 *
 * The repository itself is JGit's, held open here for as long as the app has one
 * — opening it walks the refs and the config, and the note list asks about it on
 * every sync. One [locker] around all of it: JGit's index and refs are files, and
 * two threads writing them is a repository that has to be repaired by hand.
 */
class GitManager {

    companion object {
        private const val TAG = "GitManager"
    }

    private val uiHelper = MyApp.appModule.uiHelper

    private val locker = Mutex()

    /**
     * Volatile because it is the one thing here that is read without the lock.
     * The sync asks it before it starts, the setup asks it to decide whether a
     * clone can still be cancelled, and neither of those holds [locker] — so
     * without this, the thread that opened the repository and the thread that
     * asks about it can disagree for as long as the jvm likes.
     */
    @Volatile
    var isRepoInitialized = false
        private set

    /** Only ever touched under [locker], where the lock carries it across. */
    private var git: Git? = null

    private fun requireGit(): Git =
        git ?: throw GitException(GitExceptionType.RepoNotInit)

    private suspend fun <T> safelyAccessGit(f: suspend () -> T): Result<T> = locker.withLock {
        withContext(Dispatchers.IO) {
            try {
                GitEnvironment.install(MyApp.appModule.context.filesDir)
                success(f())
            } catch (e: Exception) {
                val failure = asGitException(e)

                // A network that is not there is not a fault to hand a stack
                // trace about: it is the ordinary answer when the app is opened
                // before wifi is back, and twelve of those filled the log.
                if (failure.type == GitExceptionType.NetworkUnreachable) {
                    Log.i(TAG, failure.message ?: "no network")
                } else {
                    Log.e(TAG, failure.message ?: "git call failed", e)
                }
                failure(failure)
            }
        }
    }

    /** What a failure of this layer is, as far as the caller has to care. */
    private fun asGitException(e: Exception): GitException = when {
        e is GitException -> e

        e is MergeConflictException -> GitException(
            GitExceptionType.MergeConflict,
            uiHelper.getString(R.string.error_merge_conflict, e.paths.joinToString(", "))
        )

        e is UnresolvedConflictException -> GitException(
            GitExceptionType.UnresolvedConflict,
            uiHelper.getString(R.string.error_unresolved_conflict, e.paths.joinToString(", "))
        )

        isNetworkFailure(e) -> GitException(GitExceptionType.NetworkUnreachable, detail(e))

        else -> GitException(detail(e))
    }

    /**
     * What to show the user for a failure. JGit writes a usable sentence for most
     * of them; the class name is the fallback for the ones that carry nothing.
     */
    private fun detail(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName

    /**
     * Whether the far end was never reached.
     *
     * Told apart from everything else by the exception underneath rather than by
     * the sentence on top: JGit wraps a name that would not resolve and a key
     * that was refused in the same TransportException, and only one of the two is
     * something to keep quiet about.
     */
    private fun isNetworkFailure(e: Throwable): Boolean {
        var cause: Throwable? = e

        while (cause != null) {
            when (cause) {
                is UnknownHostException,
                is ConnectException,
                is NoRouteToHostException,
                is SocketTimeoutException,
                is SocketException,
                    -> return true
            }
            cause = cause.cause.takeIf { it != cause }
        }

        return false
    }

    suspend fun openRepo(repoPath: String): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "open repo: $repoPath")
        if (isRepoInitialized) return@safelyAccessGit

        git = try {
            openRepository(File(repoPath))
        } catch (e: IOException) {
            throw GitException(uiHelper.getString(R.string.error_open_repo, detail(e)))
        }

        isRepoInitialized = true
    }

    suspend fun cloneRepo(
        repoPath: String,
        repoUrl: String,
        cred: Cred?,
        progressCallback: (Int) -> Boolean
    ): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "clone repo: $repoPath, $repoUrl, $cred")

        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        git = try {
            cloneRepository(File(repoPath), repoUrl, cred, progressCallback)
        } catch (e: Exception) {
            if (isNetworkFailure(e)) throw e
            throw GitException(uiHelper.getString(R.string.error_clone_repo, detail(e)))
        }

        isRepoInitialized = true
    }

    /**
     * What HEAD points at, or null for a repository that has no commit yet.
     *
     * The failure is kept apart from the null on purpose: a repository that
     * cannot be read at all is not a repository that has nothing to say, and
     * the database used to take the one for the other and stay empty.
     */
    suspend fun lastCommit(): Result<String?> = safelyAccessGit {
        Log.d(TAG, "last commit")
        lastCommit(requireGit().repository)
    }

    /** What the repository already knows about its remote, if it has one. */
    suspend fun remoteUrl(): String? = safelyAccessGit {
        Log.d(TAG, "remote url")
        remoteUrl(requireGit().repository)
    }.getOrNull()

    /**
     * Points the repository at [url]. Push and pull read the remote from the
     * repository, not from the preferences, so a url that is only stored in the
     * app would leave them with nothing to talk to.
     */
    suspend fun setRemoteUrl(url: String): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "set remote url")

        try {
            setRemoteUrl(requireGit(), url)
        } catch (e: Exception) {
            if (e is GitException) throw e
            throw GitException(uiHelper.getString(R.string.error_set_remote_url, detail(e)))
        }
    }

    /**
     * @param fallbackMessage what the commit is called when the notes it holds
     * cannot be worked out — a merge that changed no file, say. Otherwise the
     * notes are named, because the commit is made of them.
     */
    suspend fun commitAll(author: GitAuthor, fallbackMessage: String): Result<Unit> =
        safelyAccessGit {
            Log.d(TAG, "commit all: ${author.name}")
            val git = requireGit()

            if (!changed(git)) {
                // nothing to commit
                Log.d(TAG, "nothing to commit")
                return@safelyAccessGit
            }

            try {
                commitAll(git, author, fallbackMessage)
            } catch (e: Exception) {
                if (e is UnresolvedConflictException || e is GitException) throw e
                throw GitException(uiHelper.getString(R.string.error_commit_repo, detail(e)))
            }
        }

    /**
     * Whether the working tree holds anything the repository has not been told
     * about — a note written, renamed or deleted since the last sync.
     */
    suspend fun isChange(): Result<Boolean> = safelyAccessGit { changed(requireGit()) }

    private fun changed(git: Git): Boolean = try {
        isChange(git)
    } catch (e: Exception) {
        throw GitException(uiHelper.getString(R.string.error_commit_file_change, detail(e)))
    }

    suspend fun currentSignature(): GitAuthor? = safelyAccessGit {
        Log.d(TAG, "currentSignature")
        signature(requireGit().repository)
    }.getOrNull()?.let { GitAuthor(name = it.first, email = it.second) }

    suspend fun push(cred: Cred?): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "push: $cred")
        val git = requireGit()

        try {
            push(git, cred)
        } catch (e: Exception) {
            if (isNetworkFailure(e)) throw e
            throw GitException(uiHelper.getString(R.string.error_push_repo, detail(e)))
        }
    }

    suspend fun pull(cred: Cred?, author: GitAuthor): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "pull: $cred")
        val git = requireGit()

        try {
            pull(git, cred, author)
        } catch (e: Exception) {
            if (isNetworkFailure(e) || e is MergeConflictException) throw e
            throw GitException(uiHelper.getString(R.string.error_pull_repo, detail(e)))
        }
    }

    /**
     * Dates the notes by the commits that wrote them.
     *
     * Only for a repository that was just opened for the first time — a clone
     * and a pull do this themselves, because they are the two things that write
     * files without the user having written them. Best effort: a note that
     * keeps the wrong date is worth less than one that fails to open.
     */
    suspend fun applyCommitTimestamps(): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "applyCommitTimestamps")
        val git = requireGit()

        try {
            applyCommitTimestamps(git)
        } catch (e: Exception) {
            Log.w(TAG, "applyCommitTimestamps: ${detail(e)}")
        }
    }

    fun closeRepoWithoutLock() {
        git?.close()
        git = null
        isRepoInitialized = false
    }

    suspend fun closeRepo() = safelyAccessGit {
        closeRepoWithoutLock()
    }

    suspend fun shutdown() = safelyAccessGit {
        closeRepoWithoutLock()
    }

}
