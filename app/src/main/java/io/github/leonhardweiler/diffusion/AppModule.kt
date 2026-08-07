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

class AppModule(val context: Context) {

    val uiHelper: UiHelper by lazy { UiHelper(context) }

    val appPreferences: AppPreferences by lazy { AppPreferences(context) }

    val repoStore: RepoStore by lazy { RepoStore(context) }

    val sshKeyStore: SshKeyStore by lazy { SshKeyStore(context) }

    val repoManager: RepoManager by lazy {
        RepoManager(repoStore, sshKeyStore, appPreferences)
    }

    /** Storage and git writes must not be cancelled when a screen goes away. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    /** Never null, see [RepoSession.none]. */
    val activeRepo: RepoSession get() = repoManager.active.value
}
