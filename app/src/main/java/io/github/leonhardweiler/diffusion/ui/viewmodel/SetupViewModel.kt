package io.github.leonhardweiler.diffusion.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.repo.RepoConfig
import io.github.leonhardweiler.diffusion.data.repo.StoredSshKey
import io.github.leonhardweiler.diffusion.helper.SshKeyValidation
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.Progress
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Result.Companion.failure

private const val TAG = "SetupViewModel"

private const val GIT_DIR = ".git"

class SetupViewModel : ViewModel() {
    private val repoManager = MyApp.appModule.repoManager
    private val keyStore = MyApp.appModule.sshKeyStore
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val appScope = MyApp.appModule.appScope

    private val _initState: MutableStateFlow<InitState> = MutableStateFlow(InitState.Idle)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    private val _storedSshKeys: MutableStateFlow<List<StoredSshKey>> = MutableStateFlow(emptyList())

    val storedSshKeys: StateFlow<List<StoredSshKey>> = _storedSshKeys.asStateFlow()

    init {
        viewModelScope.launch {
            _storedSshKeys.value = keyStore.all()
                .filter { SshKeyValidation.isKeyPair(it.publicKey, it.privateKey) }
        }
    }

    @Volatile
    private var draft: RepoSession? = null

    private suspend fun draftFor(path: String): RepoSession {
        draft?.let { existing ->
            if (existing.path == path) return existing
            existing.close()
        }

        return repoManager.draft(path).also { draft = it }
    }

    @Volatile
    private var repoIsAlreadyOnDevice = false

    @Volatile
    private var shouldCancel = false

    fun cancelClone(): Boolean {
        if (repoIsAlreadyOnDevice) {
            _initState.value = InitState.Idle
            return true
        }

        if (draft?.gitManager?.isRepoInitialized == true) {
            return false
        }
        shouldCancel = true
        return true
    }

    /**
     * Every way out of this has to put [_initState] back, or the screen that
     * shows it keeps spinning where the two buttons should be.
     */
    fun openRepo(
        storageConfig: StorageConfiguration,
        onRemoteFound: (String) -> Unit,
        onNoRemote: () -> Unit,
    ) {
        appScope.launch {
            _initState.emit(InitState.OpeningRepo)

            val folder = NodeFs.Folder.fromPath(storageConfig.repoPath())

            if (!folder.exist()) {
                uiHelper.makeToast(uiHelper.getString(R.string.error_path_not_directory))
                _initState.emit(InitState.Idle)
                return@launch
            }

            if (!NodeFs.Folder.fromPath(folder.path, GIT_DIR).exist()) {
                uiHelper.makeToast(uiHelper.getString(R.string.error_not_a_repository))
                _initState.emit(InitState.Idle)
                return@launch
            }

            val session = draftFor(storageConfig.repoPath())

            session.gitManager.openRepo(session.path).onFailure {
                uiHelper.makeToast(it.message)
                _initState.emit(InitState.Idle)
                return@launch
            }

            session.gitManager.applyCommitTimestamps()

            val remoteUrl = session.gitManager.remoteUrl().orEmpty()
            session.applyGitAuthorDefaults()

            repoIsAlreadyOnDevice = true
            _initState.emit(InitState.Idle)
            withContext(Dispatchers.Main) {
                if (remoteUrl.isEmpty()) onNoRemote() else onRemoteFound(remoteUrl)
            }
        }
    }

    private suspend fun finishSetup(onSuccess: () -> Unit) {
        withContext(Dispatchers.Main) {
            onSuccess()
        }
    }

    fun checkPathForClone(repoPath: String): Result<Unit> {
        repoIsAlreadyOnDevice = false

        val result = NodeFs.Folder.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }

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

        val session = draftFor(storageConfig.repoPath())

        if (!repoIsAlreadyOnDevice) {
            storageConfig.prepareStorageRepoPath().onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }

            NodeFs.Folder.fromPath(storageConfig.repoPath()).isEmptyDirectory().onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }

            _initState.emit(InitState.Cloning(0))

            session.gitManager.cloneRepo(
                repoPath = session.path,
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

        if (repoIsAlreadyOnDevice) {
            session.gitManager.setRemoteUrl(remoteUrl).onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }
        }

        if (repoIsAlreadyOnDevice) {
            syncOnce(session, cred).onFailure {
                _initState.emit(InitState.Error(it.message))
                return
            }
        }

        adopt(session, remoteUrl, cred)
        finishSetup(onSuccess)
    }

    fun finishWithoutRemote(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {
        appScope.launch {
            _initState.emit(InitState.OpeningRepo)

            adopt(draftFor(storageConfig.repoPath()), remoteUrl = "", cred = null)
            finishSetup(onSuccess)
        }
    }

    private suspend fun adopt(session: RepoSession, remoteUrl: String, cred: Cred?) {
        val keyId = (cred as? Cred.Ssh)?.let { keyStore.put(it) }.orEmpty()

        repoManager.adopt(
            session,
            RepoConfig(
                id = session.id,
                path = session.path,
                remoteUrl = remoteUrl,
                sshKeyId = keyId,
            )
        )

        session.applyGitAuthorDefaults()

        session.startShowingItsNotes(progressCb = { announceProgress(it) })

        draft = null
        _initState.emit(InitState.Idle)
    }

    private suspend fun syncOnce(session: RepoSession, cred: Cred?): Result<Unit> {
        _initState.emit(InitState.SyncingRepo)

        val git = session.gitManager

        git.commitAll(
            session.gitAuthor(),
            fallbackMessage = "Sync from Diffusion"
        ).onFailure { return failure(it) }

        git.pull(cred, session.gitAuthor()).onFailure { return failure(it) }

        return git.push(cred)
    }

    private fun announceProgress(progress: Progress) {
        appScope.launch {
            when (progress) {
                is Progress.ReadingRepo ->
                    _initState.emit(InitState.ReadingRepo(progress.path))
            }
        }
    }
}
