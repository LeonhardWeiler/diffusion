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

    MergeConflict,

    UnresolvedConflict,

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

class GitManager {
    companion object {
        private const val TAG = "GitManager"
    }

    private val uiHelper = MyApp.appModule.uiHelper

    private val locker = Mutex()

    @Volatile
    var isRepoInitialized = false
        private set

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

                if (failure.type == GitExceptionType.NetworkUnreachable) {
                    Log.i(TAG, failure.message ?: "no network")
                } else {
                    Log.e(TAG, failure.message ?: "git call failed", e)
                }
                failure(failure)
            }
        }
    }

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

    private fun detail(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName

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

    suspend fun lastCommit(): Result<String?> = safelyAccessGit {
        Log.d(TAG, "last commit")
        lastCommit(requireGit().repository)
    }

    suspend fun remoteUrl(): String? = safelyAccessGit {
        Log.d(TAG, "remote url")
        remoteUrl(requireGit().repository)
    }.getOrNull()

    suspend fun setRemoteUrl(url: String): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "set remote url")

        try {
            setRemoteUrl(requireGit(), url)
        } catch (e: Exception) {
            if (e is GitException) throw e
            throw GitException(uiHelper.getString(R.string.error_set_remote_url, detail(e)))
        }
    }

    suspend fun commitAll(author: GitAuthor, fallbackMessage: String): Result<Unit> =
        safelyAccessGit {
            Log.d(TAG, "commit all: ${author.name}")
            val git = requireGit()

            if (!changed(git)) {
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

    suspend fun applyCommitTimestamps(): Result<Unit> = safelyAccessGit {
        Log.d(TAG, "applyCommitTimestamps")
        val git = requireGit()

        try {
            applyCommitTimestamps(git)
        } catch (e: Exception) {
            Log.w(TAG, "applyCommitTimestamps: ${detail(e)}")
        }
    }

    private fun closeRepoWithoutLock() {
        git?.close()
        git = null
        isRepoInitialized = false
    }

    suspend fun closeRepo() = safelyAccessGit {
        closeRepoWithoutLock()
    }
}
