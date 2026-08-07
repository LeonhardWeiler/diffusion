package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "RepoSync"

private const val RETRY_AFTER_NETWORK_FAILURE_MS = 1_500L

/**
 * The half of a repository that reaches the network: commit, pull, push, and
 * the state the cloud button reads. Run under [StorageManager]'s lock, which is
 * why it takes what it needs from the write path as two calls rather than
 * reaching for it.
 */
internal class RepoSync(
    private val repo: RepoSession,
    private val refreshLocalChanges: suspend () -> Unit,
    private val rebuildIndex: suspend () -> Result<Unit>,
) {
    private val uiHelper = MyApp.appModule.uiHelper

    private val networkMonitor = MyApp.appModule.networkMonitor

    private val gitManager: GitManager get() = repo.gitManager

    private val _state: MutableStateFlow<SyncState> = MutableStateFlow(SyncState.Idle)
    val state: StateFlow<SyncState> = _state

    /** Whether a failure of the sync that is running should say so by itself. */
    private var announceErrors = true

    /** Not suspending, so the tap handler itself can set it. */
    fun announceStart() {
        _state.value = SyncState.Starting
    }

    suspend fun run(announceErrors: Boolean): Result<Unit> {
        Log.d(TAG, "sync")

        // the app is stopped several times during the setup, and the failure
        // used to stay on the button of the repository that was then cloned
        if (!gitManager.isRepoInitialized) {
            Log.d(TAG, "sync: no repository open")
            _state.emit(SyncState.Idle)
            return success(Unit)
        }

        this.announceErrors = announceErrors

        gitManager.commitAll(
            repo.gitAuthor(),
            fallbackMessage = "Sync from Diffusion"
        ).onFailure { err ->
            fail(err.message)
            return failure(err)
        }

        return exchange()
    }

    private suspend fun exchange(): Result<Unit> {
        var hasRemote = repo.remoteUrl().isNotEmpty()
        val cred = repo.cred()
        var isError = false

        var pulledFiles = false

        if (hasRemote && !networkMonitor.awaitOnline()) {
            hasRemote = false

            // one that was asked for gets an answer; one that ran by itself
            // says nothing, since the dot already says the notes are still here
            if (announceErrors) {
                fail(uiHelper.getString(R.string.error_no_network))
            } else {
                Log.d(TAG, "sync: no network, and nobody asked")
            }
        }

        if (hasRemote) {
            _state.emit(SyncState.Pull)

            var pulled = gitManager.pull(cred, repo.gitAuthor())

            // the system reports a validated network a moment before this
            // process can resolve a name, so one more try after a breath
            if (pulled.isNetworkFailure()) {
                Log.d(TAG, "pull: no network yet, trying once more")
                delay(RETRY_AFTER_NETWORK_FAILURE_MS)
                pulled = gitManager.pull(cred, repo.gitAuthor())
            }

            pulled.onSuccess {
                pulledFiles = true
            }.onFailure { err ->
                isError = true
                pulledFiles = err is GitException && err.type == GitExceptionType.MergeConflict
                reportFailure(err)
            }
        }

        // only a pull writes the working tree — a conflict among it, since that
        // is written into the notes without HEAD moving — and only the
        // repository on screen has a list worth reading it for
        if (pulledFiles && repo.showsItsNotes) {
            rebuildIndex().onFailure { err ->
                fail(err.message)
                return failure(err)
            }
        }

        // not after a pull that did not go through: the push would be refused
        // for being behind, and its failure would replace the one explanation
        // that says what to do
        if (hasRemote && !isError) {
            _state.emit(SyncState.Push)
            gitManager.push(cred).onFailure { err ->
                isError = true
                reportFailure(err)
            }
        }

        // before Ok is emitted, or the dot stands under a button that has
        // already said the notes went out, for as long as this walk takes
        refreshLocalChanges()

        if (hasRemote && !isError) {
            _state.emit(SyncState.Ok)
        } else if (_state.value is SyncState.Starting) {
            // a repository without a remote, and a button that would otherwise
            // pulse forever
            _state.emit(SyncState.Idle)
        }

        return success(Unit)
    }

    private suspend fun fail(message: String?) {
        message?.let { Log.e(TAG, it) }
        _state.emit(SyncState.Error(message, announce = announceErrors))
    }

    /**
     * A sync nobody asked for that failed because the network was not there
     * leaves no error: the dot already says the notes have not gone out, and
     * the next sync will do it without being asked.
     */
    private suspend fun reportFailure(err: Throwable) {
        val transient = !announceErrors &&
                err is GitException &&
                err.type == GitExceptionType.NetworkUnreachable

        if (transient) {
            Log.d(TAG, "sync: no network, and nobody asked: ${err.message}")
            _state.emit(SyncState.Idle)
            return
        }

        fail(err.message)
    }

    private fun Result<*>.isNetworkFailure(): Boolean =
        (exceptionOrNull() as? GitException)?.type == GitExceptionType.NetworkUnreachable
}
