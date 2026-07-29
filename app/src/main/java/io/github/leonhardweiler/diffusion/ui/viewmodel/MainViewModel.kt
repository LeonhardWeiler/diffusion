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


    /**
     * The repository this app is holding on to while it cannot reach it, or
     * null when there is nothing to hold on to.
     *
     * Reading files needs the permission to read all files, and that permission
     * can be gone while everything else — the path, the remote, the ssh key,
     * every setting — is still stored. Sending the user through the setup for
     * it meant picking the folder again to arrive at exactly what was already
     * written down.
     */
    suspend fun repoAwaitingPermission(): String? {
        if (!prefs.isInit.get()) return null
        if (StoragePermissionHelper.isPermissionGranted()) return null

        return runCatching { prefs.repoPath() }.getOrNull()?.ifEmpty { null }
    }

    /** Forgets the repository, for the case the permission is not what is wrong. */
    suspend fun forgetRepo() {
        storageManager.closeRepo()
    }

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

        // Everything that has to read the repository goes to the app's scope,
        // in this order and not in three of their own: this runs on the way to
        // the first frame, and each of them walks the whole working tree.
        appScope.launch {
            // The list is held in memory and there is nothing left of it from
            // the last run, so this is what a start costs now — a few hundred
            // milliseconds behind a screen that is coming up anyway.
            storageManager.rebuildIndex()

            // What a previous run left in the working tree is still there —
            // being killed does not commit anything, and the flag behind the
            // dot starts false in a fresh process, so an app that had crashed
            // came back saying everything had been sent.
            storageManager.refreshChangeState()

            // Opening the app is one of the two moments a sync does not have to
            // be asked for — what another device wrote is what one opens the
            // app to read. It says nothing if the network is not there. Unless
            // it was turned off: a transfer nobody asked for is not always
            // wanted.
            if (prefs.syncOnOpenAndClose.get()) {
                storageManager.syncWithRemote(announceErrors = false)
            }
        }

        return true
    }

}
