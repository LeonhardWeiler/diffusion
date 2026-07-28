package io.github.wiiznokes.gitnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
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
        prefs.applyGitAuthorDefaults(null, gitManager.currentSignature())

        // Opening the app is one of the two moments a sync does not have to be
        // asked for — what another device wrote is what one opens the app to
        // read. It brings the database in line on the way through, and says
        // nothing if the network is not there.
        appScope.launch {
            storageManager.syncWithRemote(announceErrors = false)
        }

        return true
    }

}
