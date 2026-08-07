package io.github.leonhardweiler.diffusion.ui.viewmodel

import androidx.lifecycle.ViewModel
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.helper.StoragePermissionHelper
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.ui.destination.AppDestination
import io.github.leonhardweiler.diffusion.ui.destination.Destination
import io.github.leonhardweiler.diffusion.ui.destination.SetupDestination
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val repoManager = MyApp.appModule.repoManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val appScope = MyApp.appModule.appScope

    suspend fun repoAwaitingPermission(): String? {
        if (repoManager.load().isEmpty()) return null
        if (StoragePermissionHelper.isPermissionGranted()) return null

        return repoManager.active.value.path.ifEmpty { null }
    }

    suspend fun forgetRepo() {
        repoManager.remove(repoManager.active.value.id)
    }

    fun currentDestination(): Destination {
        val active = repoManager.active.value

        return if (active.exists) {
            Destination.App(active.id, AppDestination.Grid)
        } else {
            Destination.Setup(SetupDestination.Main)
        }
    }

    suspend fun tryInit(): Boolean {
        val repos = repoManager.load()
        if (repos.isEmpty()) return false

        if (repoManager.active.value.gitManager.isRepoInitialized) {
            return true
        }

        if (!StoragePermissionHelper.isPermissionGranted()) {
            return false
        }

        if (!repoManager.openActive()) {
            return false
        }

        appScope.launch {
            repoManager.active.value.startShowingItsNotes()

            repoManager.openTheRest()

            repoManager.syncAllQuietly()
        }

        return true
    }
}
