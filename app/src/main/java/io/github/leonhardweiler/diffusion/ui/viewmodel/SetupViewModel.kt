package io.github.leonhardweiler.diffusion.ui.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.helper.SshKeyValidation
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.Progress
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SetupViewModel"

/** What makes a folder a repository. */
private const val GIT_DIR = ".git"

class SetupViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    // Setting up a repository must not be cancelled by leaving the screen.
    private val appScope = MyApp.appModule.appScope

    private val _initState: MutableStateFlow<InitState> = MutableStateFlow(InitState.Idle)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    private val _storedSshKey: MutableStateFlow<Cred.Ssh?> = MutableStateFlow(null)

    /**
     * The key pair the app already holds, if it holds a usable one.
     *
     * closeRepo() only forgets that a repository was set up; the credentials
     * stay in the store and a repository opened without new ones uses them. But
     * nothing led back to them: setting up again meant either generating a
     * fresh pair or fetching one off the disk, so every attempt cost the
     * repository another deploy key and the old ones piled up there.
     */
    val storedSshKey: StateFlow<Cred.Ssh?> = _storedSshKey.asStateFlow()

    init {
        viewModelScope.launch {
            val cred = prefs.cred()
            _storedSshKey.value = (cred as? Cred.Ssh)
                ?.takeIf { SshKeyValidation.isKeyPair(it.publicKey, it.privateKey) }
        }
    }

    /**
     * Set when a repository that was opened turns out to have a remote. The
     * credential screens are the same ones the clone uses, but there is nothing
     * left to clone — only the credentials for that remote are missing.
     */
    @Volatile
    private var repoIsAlreadyOnDevice = false

    /**
     * Written on the main thread by the cancel button and read from the clone's
     * progress callback, which runs wherever libgit2 is. Volatile, or the loop
     * that is meant to stop reading it can keep reading the value it had when
     * it started.
     */
    @Volatile
    private var shouldCancel = false
    fun cancelClone(): Boolean {
        if (gitManager.isRepoInitialized) {
            return false
        }
        shouldCancel = true
        return true
    }

    /**
     * @param onRemoteFound called with the url when the repository already has a
     * remote, so the setup can go on and ask for credentials for it.
     * @param onNoRemote called when it has none, so the user can be asked whether
     * to give it one.
     */
    fun openRepo(
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit,
        onRemoteFound: (String) -> Unit,
        onNoRemote: () -> Unit,
    ) {

        appScope.launch {
            // Everything below can take a second or two — libgit2 opening the
            // repository, and the whole working tree being read into the
            // database. Until this said so, the tap on the folder looked like it
            // had been ignored.
            _initState.emit(InitState.OpeningRepo)

            val folder = NodeFs.Folder.fromPath(storageConfig.repoPath())

            if (!folder.exist()) {
                uiHelper.makeToast(uiHelper.getString(R.string.error_path_not_directory))
                _initState.emit(InitState.Idle)
                return@launch
            }

            // libgit2 would answer this with "could not find repository", which
            // names the symptom rather than what to do about it
            if (!NodeFs.Folder.fromPath(folder.path, GIT_DIR).exist()) {
                uiHelper.makeToast(uiHelper.getString(R.string.error_not_a_repository))
                _initState.emit(InitState.Idle)
                return@launch
            }

            // A repository may still be open from an earlier attempt in this
            // same setup: the credential screens can be backed out of, and
            // nothing closes what was opened on the way in. openRepoLib returns
            // at once when one is open, so without this the app would have gone
            // on holding the first folder while the preferences moved to the
            // second — notes written into one repository, commits made in the
            // other.
            gitManager.closeRepo()

            gitManager.openRepo(storageConfig.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                _initState.emit(InitState.Idle)
                return@launch
            }

            // Once, for a repository the app is seeing for the first time: it
            // may have been checked out by something else, which would have
            // dated every note to the moment it arrived. A clone and a pull do
            // this themselves, and every later start reads the dates as they
            // now stand on disk.
            gitManager.applyCommitTimestamps()

            // what the repository already knows about itself, rather than
            // asking for it again
            val remoteUrl = gitManager.remoteUrl().orEmpty()

            prefs.applyGitAuthorDefaults(gitManager.currentSignature())
            prefs.initRepo(storageConfig, remoteUrl)

            // the repo has just been opened, so the database is built from
            // whatever is on disk, committed or not
            storageManager.updateDatabase(
                force = true,
                progressCb = { announceProgress(it) }
            )

            // whether it already syncs somewhere or not, the setup goes on from
            // here rather than finishing: a repository without a remote is a
            // choice, not a conclusion
            repoIsAlreadyOnDevice = true
            _initState.emit(InitState.Idle)
            withContext(Dispatchers.Main) {
                if (remoteUrl.isEmpty()) onNoRemote() else onRemoteFound(remoteUrl)
            }
        }

    }

    /**
     * The callback leaves the setup and changes the navigation backstack, which
     * is compose snapshot state. All the callers run on [Dispatchers.IO], so it
     * has to be handed back to the main thread first.
     */
    private suspend fun finishSetup(onSuccess: () -> Unit) {
        withContext(Dispatchers.Main) {
            onSuccess()
        }
    }


    fun checkPathForClone(repoPath: String): Result<Unit> {
        // The one place the clone route is taken, so the one place to take back
        // what an earlier "open" in the same setup decided: it is the flag that
        // says there is nothing left to clone, and left standing it turned a
        // clone into "set a remote url on the folder that is still open".
        repoIsAlreadyOnDevice = false

        val result = NodeFs.Folder.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }

    /**
     * Setting up a repository writes to disk and to the preferences. Leaving the
     * screen must not tear that down half way, so it does not run in
     * viewModelScope. The clone has [cancelClone] for the explicit way out.
     */
    private fun runCloneJob(f: suspend () -> Unit) {
        appScope.launch {
            f()
        }
    }

    fun cloneRepo(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred?,
        onSuccess: () -> Unit
    ) {

        runCloneJob {
            cloneRepoInternal(
                storageConfig = storageConfig,
                remoteUrl = remoteUrl,
                cred = cred,
                onSuccess = onSuccess
            )
        }
    }

    /**
     * Throws away what a canceled or failed clone left in the repo directory.
     * Safe to delete: the directory was verified to be empty right before the
     * clone started, so everything in it now was written by that clone. The
     * empty directory itself is kept, so the next attempt starts out the same
     * way this one did.
     */
    private fun discardPartialClone(storageConfig: StorageConfiguration) {
        val folder = NodeFs.Folder.fromPath(storageConfig.repoPath())

        folder.delete().onFailure {
            Log.e(TAG, "could not discard the partial clone: ${it.message}")
            return
        }
        folder.create().onFailure {
            Log.e(TAG, "could not recreate the repo directory: ${it.message}")
        }
    }

    suspend fun cloneRepoInternal(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred?,
        onSuccess: () -> Unit
    ) {
        shouldCancel = false

        if (!repoIsAlreadyOnDevice) {
            // Same reason as in openRepo: an "open" earlier in this setup can
            // have been backed out of and still holds a repository, and cloning
            // while one is open is refused outright.
            gitManager.closeRepo()

            storageConfig.prepareStorageRepoPath().onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }

            // Checked again here, not only when the folder was chosen: the whole
            // remote setup happens in between and could have filled it.
            NodeFs.Folder.fromPath(storageConfig.repoPath()).isEmptyDirectory().onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }

            _initState.emit(InitState.Cloning(0))

            gitManager.cloneRepo(
                repoPath = storageConfig.repoPath(),
                repoUrl = remoteUrl,
                cred = cred,
                progressCallback = {
                    _initState.tryEmit(InitState.Cloning(it))
                    !shouldCancel
                }
            ).onFailure {
                discardPartialClone(storageConfig)
                _initState.emit(InitState.Error(if (shouldCancel) "Clone canceled" else it.message))
                return
            }
            if (shouldCancel) {
                discardPartialClone(storageConfig)
                return
            }
        }

        // an opened repository may not have had a remote at all, and push reads
        // it from the repository rather than from the preferences
        if (repoIsAlreadyOnDevice) {
            gitManager.setRemoteUrl(remoteUrl).onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }
        }

        prefs.initRepo(storageConfig, remoteUrl)
        prefs.updateCred(cred)
        prefs.applyGitAuthorDefaults(gitManager.currentSignature())

        // force, like the one in openRepo: what was just cloned or just given a
        // remote is a working tree the database has never seen, whatever
        // databaseCommit happens to say about it.
        storageManager.updateDatabase(force = true, progressCb = { announceProgress(it) })

        finishSetup(onSuccess)

    }

    /**
     * Says which folder the database is being built from. In the app's scope
     * rather than this view model's: the setup screen is left while this is
     * still running, and a report that dies with it would take the last state
     * the screen was shown in with it.
     */
    private fun announceProgress(progress: Progress) {
        appScope.launch {
            when (progress) {
                is Progress.GeneratingDatabase ->
                    _initState.emit(InitState.GeneratingDatabase(progress.path))
            }
        }
    }
}
