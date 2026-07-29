package io.github.leonhardweiler.diffusion.ui.viewmodel

import androidx.lifecycle.ViewModel
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.helper.StoragePermissionHelper
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    // The first sync must not die with the view model that kicked it off.
    private val appScope = MyApp.appModule.appScope


    suspend fun tryInit(): Boolean {

        if (!prefs.isInit.get()) {
            return false
        }

        if (!StoragePermissionHelper.isPermissionGranted()) {
            return false
        }

        val repoPath = try {
            prefs.repoPath()
        } catch (_: Exception) {
            return false
        }

        val storageConfig = StorageConfiguration(repoPath)

        if (!NodeFs.Folder.fromPath(storageConfig.repoPath()).exist()) {
            return false
        }

        gitManager.openRepo(storageConfig.repoPath()).onFailure {
            return false
        }
        prefs.applyGitAuthorDefaults(gitManager.currentSignature())

        // What a previous run left in the working tree is still there — being
        // killed does not commit anything. Nothing carried that over, though:
        // the flag behind the dot starts false in a fresh process and was only
        // ever set by a write or by a sync, so an app that had crashed came
        // back saying everything had been sent. Asking git walks the working
        // tree, so it goes to the app's scope rather than holding up the first
        // frame.
        appScope.launch {
            storageManager.refreshChangeState()
        }

        // Opening the app is one of the two moments a sync does not have to be
        // asked for — what another device wrote is what one opens the app to
        // read. It brings the database in line on the way through, and says
        // nothing if the network is not there. Unless it was turned off: a
        // transfer nobody asked for is not always wanted.
        if (prefs.syncOnOpenAndClose.get()) {
            appScope.launch {
                storageManager.syncWithRemote(announceErrors = false)
            }
        }

        return true
    }

}
