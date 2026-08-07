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
        if (repoManager.load().isEmpty()) return null
        if (StoragePermissionHelper.isPermissionGranted()) return null

        return repoManager.active.value.path.ifEmpty { null }
    }

    /** Forgets the repository, for the case the permission is not what is wrong. */
    suspend fun forgetRepo() {
        repoManager.remove(repoManager.active.value.id)
    }

    /**
     * Where the app stands now: on the notes of the repository being looked at,
     * or on the setup when there is none.
     *
     * The repository's id is part of the destination, which is what makes
     * switching to another one a screen that is built again from nothing — the
     * note list, its view model and its scroll position all belong to one
     * repository.
     */
    fun currentDestination(): Destination {
        val active = repoManager.active.value

        return if (active.exists) {
            Destination.App(active.id, AppDestination.Grid)
        } else {
            Destination.Setup(SetupDestination.Main)
        }
    }

    /**
     * Opens the stored repositories and reads the notes of the one being looked
     * at, and says whether that worked.
     *
     * Called at every creation of the activity, which is not the same as every
     * start: a rotation or a theme change builds it again on a process that
     * already has the repositories open, and everything below walks a whole
     * working tree. So an open repository is answered for straight away —
     * [io.github.leonhardweiler.diffusion.manager.GitManager.isRepoInitialized]
     * is the one thing that says whether this process has been through here,
     * and it dies with the process, which is exactly when the work has to
     * happen again.
     */
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

        // Everything that has to read a repository goes to the app's scope, in
        // this order and not in three of their own: this runs on the way to the
        // first frame, and each of them walks a whole working tree.
        appScope.launch {
            // The list is held in memory and there is nothing left of it from
            // the last run, so this is what a start costs now — a few hundred
            // milliseconds behind a screen that is coming up anyway. Only the
            // repository being looked at is read; the others are opened so that
            // they can sync and say so, and read when they are switched to.
            //
            // refreshChangeState is part of it: what a previous run left in the
            // working tree is still there — being killed does not commit
            // anything, and the flag behind the dot starts false in a fresh
            // process, so an app that had crashed came back saying everything
            // had been sent.
            repoManager.active.value.startShowingItsNotes()

            repoManager.openTheRest()

            // Opening the app is one of the two moments a sync does not have to
            // be asked for — what another device wrote is what one opens the
            // app to read. It says nothing if the network is not there. Unless
            // it was turned off: a transfer nobody asked for is not always
            // wanted, and each repository answers that for itself.
            repoManager.syncAllQuietly()
        }

        return true
    }

}
