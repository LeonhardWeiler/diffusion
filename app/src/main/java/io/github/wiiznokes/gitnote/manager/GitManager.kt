package io.github.wiiznokes.gitnote.manager

import android.util.Log
import androidx.annotation.Keep
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.GitAuthor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


enum class GitExceptionType {
    InitLib,
    RepoAlreadyInit,
    RepoNotInit,

    /**
     * A pull that could not be merged. The notes it could not merge now hold
     * both versions between markers, so the caller has to read the files again
     * even though HEAD has not moved.
     */
    MergeConflict,
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

class GitManager {

    companion object {
        private const val TAG = "GitManager"

        /**
         * Returned by the rust side when a pull cannot be merged automatically.
         * Not a libgit2 code, see MERGE_CONFLICT in rust/src/lib.rs.
         */
        private const val MERGE_CONFLICT = -1000

        init {
            Log.d(TAG, "init")
            System.loadLibrary("git_wrapper")
        }
    }

    private val uiHelper = MyApp.appModule.uiHelper

    private val locker = Mutex()
    var isRepoInitialized = false
        private set
    private var isLibInitialized = false

    private suspend fun <T> safelyAccessLibGit2(f: suspend () -> T): Result<T> = locker.withLock {
        try {
            if (!isLibInitialized) {
                val res = initLib()
                Log.d(TAG, "res on init = $res")
                if (res < 0) {
                    throw GitException(GitExceptionType.InitLib)
                }
                isLibInitialized = true
            }
            success(f())
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "libgit2 call failed", e)
            failure(e)
        }
    }

    /**
     * What to show the user for a negative return code. libgit2 writes a usable
     * sentence for most failures, which the rust side keeps around; the bare
     * number is only the fallback when there is nothing.
     */
    private fun errorDetail(res: Int): String = lastErrorMessageLib() ?: res.toString()




    suspend fun openRepo(repoPath: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "open repo: $repoPath")
        if (isRepoInitialized) return@safelyAccessLibGit2

        val res = openRepoLib(repoPath)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_open_repo, errorDetail(res)))
        }
        isRepoInitialized = true
    }

    private var actualCb: ((Int) -> Boolean)? = null

    /**
     * This function is called from native code
     */
    @Keep
    fun progressCb(progress: Int): Boolean {
        return actualCb?.invoke(progress) != false
    }

    suspend fun cloneRepo(
        repoPath: String,
        repoUrl: String,
        cred: Cred?,
        progressCallback: (Int) -> Boolean
    ): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "clone repo: $repoPath, $repoUrl, $cred")

        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        actualCb = progressCallback

        val res = cloneRepoLib(
            repoPath = repoPath,
            remoteUrl = repoUrl,
            cred = cred,
            progressCallback = this
        )

        actualCb = null

        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_clone_repo, errorDetail(res)))
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
    suspend fun lastCommit(): Result<String?> = safelyAccessLibGit2 {
        Log.d(TAG, "last commit")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        lastCommitLib()
    }

    /** What the repository already knows about its remote, if it has one. */
    suspend fun remoteUrl(): String? = safelyAccessLibGit2 {
        Log.d(TAG, "remote url")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        remoteUrlLib()
    }.getOrNull()

    /**
     * Points the repository at [url]. Push and pull read the remote from the
     * repository, not from the preferences, so a url that is only stored in the
     * app would leave them with nothing to talk to.
     */
    suspend fun setRemoteUrl(url: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "set remote url")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        val res = setRemoteUrlLib(url)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_set_remote_url, errorDetail(res)))
        }
    }

    suspend fun commitAll(author: GitAuthor, message: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "commit all: ${author.name}")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        var res = isChangeLib()

        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_commit_file_change, errorDetail(res)))
        }

        if (res == 0) {
            // nothing to commit
            Log.d(TAG, "nothing to commit")
            return@safelyAccessLibGit2
        }

        res = commitAllLib(author.name, author.email, message)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_commit_repo, errorDetail(res)))
        }

    }

    /**
     * Whether the working tree holds anything the repository has not been told
     * about — a note written, renamed or deleted since the last sync.
     */
    suspend fun isChange(): Result<Boolean> = safelyAccessLibGit2 {
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        val res = isChangeLib()
        if (res < 0) {
            throw GitException(
                uiHelper.getString(R.string.error_commit_file_change, errorDetail(res))
            )
        }
        res > 0
    }

    suspend fun currentSignature(): GitAuthor? = safelyAccessLibGit2 {
        Log.d(TAG, "currentSignature")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        currentSignatureLib()
    }.getOrNull()?.let { GitAuthor(name = it.first, email = it.second) }

    suspend fun push(cred: Cred?): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "push: $cred")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        val res = pushLib(cred)

        if (res < 0) {
            throw Exception(uiHelper.getString(R.string.error_push_repo, errorDetail(res)))
        }

    }

    suspend fun pull(cred: Cred?, author: GitAuthor): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "pull: $cred")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        val res = pullLib(cred, author.name, author.email)

        if (res == MERGE_CONFLICT) {
            // the detail names the notes it could not merge, which is the whole
            // of what the user has to go and look at
            throw GitException(
                GitExceptionType.MergeConflict,
                uiHelper.getString(R.string.error_merge_conflict, errorDetail(res))
            )
        }

        if (res < 0) {
            throw Exception(uiHelper.getString(R.string.error_pull_repo, errorDetail(res)))
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
    suspend fun applyCommitTimestamps(): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "applyCommitTimestamps")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        val res = applyCommitTimestampsLib()
        if (res < 0) {
            Log.w(TAG, "applyCommitTimestamps: ${errorDetail(res)}")
        }
    }


    fun closeRepoWithoutLock() {
        if (isRepoInitialized) closeRepoLib()
        isRepoInitialized = false
    }

    suspend fun closeRepo() = safelyAccessLibGit2 {
        closeRepoWithoutLock()
    }

    suspend fun shutdown() = safelyAccessLibGit2 {
        closeRepoWithoutLock()
        if (isLibInitialized) freeLib()
        isLibInitialized = false
    }

}

private external fun initLib(
    homePath: String = MyApp.appModule.context.filesDir.toPath().toString()
): Int


private external fun openRepoLib(repoPath: String): Int

private external fun cloneRepoLib(
    repoPath: String,
    remoteUrl: String,
    cred: Cred?,
    progressCallback: GitManager
): Int


private external fun lastCommitLib(): String?

private external fun remoteUrlLib(): String?

private external fun setRemoteUrlLib(url: String): Int

private external fun commitAllLib(name: String, email: String, message: String): Int
private external fun currentSignatureLib(): Pair<String, String>?
private external fun pushLib(cred: Cred?): Int
private external fun pullLib(cred: Cred?, name: String, email: String): Int

private external fun freeLib()


private external fun closeRepoLib()

private external fun isChangeLib(): Int

/**
 * The message behind the last negative return code, or null when there is none.
 * Reading it clears it on the rust side.
 */
private external fun lastErrorMessageLib(): String?

private external fun applyCommitTimestampsLib(): Int

external fun generateSshKeysLib(): Pair<String, String>

// return true if url is ssh
external fun getUrlInfoLib(url: String): Boolean?

/**
 * The remote url as something a browser can follow, or null if it cannot be
 * read as a repository url at all.
 */
external fun browserUrlLib(url: String): String?

