package io.github.wiiznokes.gitnote.ui.viewmodel


import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.Progress
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.provider.RepoInfo
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "SetupViewModel"

class SetupViewModel(val authFlow: SharedFlow<String>) : ViewModel(), SetupViewModelI {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    // Setting up a repository must not be cancelled by leaving the screen.
    private val appScope = MyApp.appModule.appScope

    private val _initState: MutableStateFlow<InitState> = MutableStateFlow(InitState.Idle)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    var provider: Provider? = null
        private set

    var repos = listOf<RepoInfo>()
        private set

    var userInfo: UserInfo? = null
        private set

    init {

        viewModelScope.launch {
            authFlow.collect {
                Log.d(TAG, "received $it")
                onReceiveCode(it)
            }
        }
    }

    private var shouldCancel = false
    fun cancelClone(): Boolean {
        if (gitManager.isRepoInitialized) {
            return false
        }
        shouldCancel = true
        return true
    }

    fun setStateToIdle() {
        viewModelScope.launch {
            delay(50)
            _initState.emit(InitState.Idle)
        }
    }


    fun openRepo(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {

        appScope.launch {
            if (!NodeFs.Folder.fromPath(storageConfig.repoPath()).exist()) {
                val msg = uiHelper.getString(R.string.error_path_not_directory)
                uiHelper.makeToast(msg)
                return@launch
            }

            gitManager.openRepo(storageConfig.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            prefs.applyGitAuthorDefaults(userInfo, gitManager.currentSignature())
            prefs.initRepo(storageConfig)

            // the repo has just been opened or cloned, so the database is built
            // from whatever is on disk, committed or not
            storageManager.updateDatabase(force = true)

            finishSetup(onSuccess)
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
        val result = NodeFs.Folder.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }

    override fun launch(f: suspend () -> Unit) {
        viewModelScope.launch { f() }
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

    override fun cloneRepo(
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

        val cloneConfig = storageConfig.withUrlName(remoteUrl)

        cloneConfig.prepareStorageRepoPath().onFailure {
            _initState.emit(InitState.Error(it.message))
            return
        }

        // Checked here and not only in the setup screen, because the check there is
        // skipped when the repo name comes from the url.
        NodeFs.Folder.fromPath(cloneConfig.repoPath()).isEmptyDirectory().onFailure {
            _initState.emit(InitState.Error(it.message))
            return
        }

        _initState.emit(InitState.Cloning(0))

        gitManager.cloneRepo(
            repoPath = cloneConfig.repoPath(),
            repoUrl = remoteUrl,
            cred = cred,
            progressCallback = {
                _initState.tryEmit(InitState.Cloning(it))
                !shouldCancel
            }
        ).onFailure {
            discardPartialClone(cloneConfig)
            _initState.emit(InitState.Error(if (shouldCancel) "Clone canceled" else it.message))
            return
        }
        if (shouldCancel) {
            discardPartialClone(cloneConfig)
            return
        }

        prefs.initRepo(cloneConfig)
        prefs.remoteUrl.update(remoteUrl)

        prefs.updateCred(cred)
        prefs.applyGitAuthorDefaults(userInfo, gitManager.currentSignature())

        storageManager.updateDatabase(
            progressCb = {
                viewModelScope.launch {
                    _initState.emit(
                        when (it) {
                            is Progress.GeneratingDatabase -> InitState.GeneratingDatabase(it.path)
                            Progress.Timestamps -> InitState.CalculatingTimestamps
                        }
                    )
                }
            }
        )

        finishSetup(onSuccess)

    }


    fun setProvider(provider: ProviderType?) {
        this.provider = when (provider) {
            ProviderType.GitHub -> GithubProvider()
            null -> null
        }
    }

    fun getLaunchOAuthScreenIntent(): Intent {
        val authUrl = provider!!.getLaunchOAuthScreenUrl()
        return Intent(Intent.ACTION_VIEW, authUrl.toUri())
    }


    /**
     * The provider talks to the network with blocking calls, so none of them may
     * run on the main thread: Android answers that with a
     * NetworkOnMainThreadException whose message is null, which used to end up as
     * an error nobody could see.
     */
    private suspend fun <T> onProvider(
        what: String,
        f: Provider.() -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            success(provider!!.f())
        } catch (e: Exception) {
            val message = e.message ?: e.toString()
            Log.e(TAG, "$what: $message, $e")
            _initState.emit(InitState.Error(message))
            uiHelper.makeToast(message)
            failure(e)
        }
    }

    fun onReceiveCode(code: String) {

        viewModelScope.launch {

            _initState.emit(InitState.GettingAccessToken)
            val token = onProvider("exchangeCodeForAccessToken") {
                exchangeCodeForAccessToken(code)
            }.getOrElse { return@launch }

            prefs.appAuthToken.update(token)

            fetchInfos(token = token)
        }
    }

    fun fetchInfos(token: String) {
        viewModelScope.launch {

            _initState.emit(InitState.FetchingRepos)

            repos = onProvider("fetchUserRepos") {
                fetchUserRepos(token = token)
            }.getOrElse { return@launch }

            _initState.emit(InitState.GettingUserInfo)

            userInfo = onProvider("getUserInfo") {
                getUserInfo(token = token)
            }.getOrElse { return@launch }

            Log.d(TAG, "emit: Success")
            _initState.emit(InitState.FetchingInfosSuccess)
        }
    }

    override fun cloneRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {
        runCloneJob {
            cloneWithOwnDeployKey(repoName, storageConfig, onSuccess)
        }
    }

    override fun createRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {
        runCloneJob {
            val token = prefs.appAuthToken.get()

            _initState.emit(InitState.CreatingRemoteRepo)
            onProvider("createNewRepo") {
                createNewRepo(token = token, repoName = repoName)
            }.getOrElse { return@runCloneJob }

            cloneWithOwnDeployKey(repoName, storageConfig, onSuccess)
        }
    }

    /**
     * Gives the app a key of its own on the remote and clones over ssh with it,
     * so that nothing the user has to type is kept on the device.
     */
    private suspend fun cloneWithOwnDeployKey(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {
        val (publicKey, privateKey) = generateSshKeysLib()
        val token = prefs.appAuthToken.get()

        _initState.emit(InitState.AddingDeployKey)
        onProvider("addDeployKeyToRepo") {
            addDeployKeyToRepo(
                token = token,
                publicKey = publicKey,
                fullRepoName = repoName
            )
        }.getOrElse { return }

        cloneRepoInternal(
            storageConfig = storageConfig,
            remoteUrl = provider!!.sshCloneUrlFromRepoName(repoName),
            cred = Cred.Ssh(
                publicKey = publicKey,
                privateKey = privateKey,
                passphrase = null
            ),
            onSuccess = onSuccess
        )
    }
}
