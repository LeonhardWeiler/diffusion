package io.github.leonhardweiler.diffusion

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.repo.RepoStore
import io.github.leonhardweiler.diffusion.data.repo.SshKeyStore
import io.github.leonhardweiler.diffusion.helper.NetworkMonitor
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.RepoManager
import io.github.leonhardweiler.diffusion.manager.RepoSession


interface AppModule {
    val appScope: CoroutineScope
    val uiHelper: UiHelper
    val repoStore: RepoStore
    val sshKeyStore: SshKeyStore
    val repoManager: RepoManager
    val appPreferences: AppPreferences
    val networkMonitor: NetworkMonitor
    val context: Context

    /**
     * The repository the note list is showing. Never null — see
     * [RepoSession.none] — so that a screen composed while the last repository
     * is being let go of has something to read rather than a null to guard.
     */
    val activeRepo: RepoSession get() = repoManager.active.value
}

class AppModuleImpl(
    override val context: Context
) : AppModule {

    override val uiHelper: UiHelper by lazy {
        UiHelper(context)
    }

    override val appPreferences: AppPreferences by lazy {
        AppPreferences(context)
    }

    override val repoStore: RepoStore by lazy {
        RepoStore(context)
    }

    override val sshKeyStore: SshKeyStore by lazy {
        SshKeyStore(context)
    }

    /** The repositories, one open session each, one of them being looked at. */
    override val repoManager: RepoManager by lazy {
        RepoManager(repoStore, sshKeyStore, appPreferences)
    }

    /** Storage and git writes must not be cancelled when a screen goes away. */
    override val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }
}
