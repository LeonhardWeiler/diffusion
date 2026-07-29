package io.github.leonhardweiler.diffusion

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.ui.destination.AppDestination
import io.github.leonhardweiler.diffusion.ui.destination.Destination
import io.github.leonhardweiler.diffusion.ui.destination.SetupDestination
import io.github.leonhardweiler.diffusion.ui.screen.app.AppScreen
import io.github.leonhardweiler.diffusion.ui.screen.setup.SetupNav
import io.github.leonhardweiler.diffusion.ui.screen.setup.StoragePermissionScreen
import io.github.leonhardweiler.diffusion.ui.theme.DiffusionTheme
import io.github.leonhardweiler.diffusion.ui.utils.crossFade
import io.github.leonhardweiler.diffusion.ui.theme.Theme
import io.github.leonhardweiler.diffusion.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContent {

            val vm: MainViewModel = viewModel()

            val theme by vm.prefs.theme.getAsState()

            DiffusionTheme(
                darkTheme = (theme == Theme.SYSTEM && isSystemInDarkTheme()) || theme == Theme.DARK,
            ) {

                // A repository that is set up but cannot be read is not one to
                // set up again: everything about it is stored, and what is
                // missing is the permission to read the files. Being sent back
                // to "open or clone" for that meant picking the same folder
                // again to arrive at what was already written down — which is
                // what a new build installed over the old one used to cost.
                val startDestination: Destination = rememberSaveable {
                    when {
                        runBlocking { vm.tryInit() } -> Destination.App(AppDestination.Grid)

                        else -> runBlocking { vm.repoAwaitingPermission() }
                            ?.let { Destination.MissingPermission(it) }
                            ?: Destination.Setup(SetupDestination.Main)
                    }
                }

                val backstack = rememberBackstack(startDestination)

                NavHost(
                    backstack = backstack,
                    // The setup and the app are not two steps of one path, so
                    // neither slides into the other.
                    transition = { _, _, _ -> crossFade() },
                ) { destination ->
                    when (destination) {
                        is Destination.Setup -> SetupNav(
                            startDestination = destination.setupDestination,
                            onSetupSuccess = {
                                backstack.replaceAll(Destination.App(AppDestination.Grid))
                            }
                        )

                        is Destination.App -> AppScreen(
                            appDestination = destination.appDestination,
                            onCloseRepo = {
                                backstack.replaceAll(
                                    Destination.Setup(SetupDestination.Main)
                                )
                            }
                        )

                        is Destination.MissingPermission -> {
                            val scope = rememberCoroutineScope()

                            StoragePermissionScreen(
                                repoPath = destination.repoPath,
                                onGranted = {
                                    scope.launch {
                                        // asked again with the permission in
                                        // hand, which is what decides whether
                                        // that was all that was missing
                                        if (vm.tryInit()) {
                                            backstack.replaceAll(
                                                Destination.App(AppDestination.Grid)
                                            )
                                        } else {
                                            vm.uiHelper.makeToast(
                                                vm.uiHelper.getString(
                                                    R.string.error_folder_not_found,
                                                    destination.repoPath
                                                )
                                            )
                                        }
                                    }
                                },
                                onGiveUp = {
                                    scope.launch {
                                        vm.forgetRepo()
                                        backstack.replaceAll(
                                            Destination.Setup(SetupDestination.Main)
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * The other half of syncing when the app opens: what was written here goes
     * out when it is left, so that a note does not sit on one device until
     * somebody remembers the button.
     *
     * super first, so that the editor has been told to write what it holds
     * before the sync goes looking for it — the sync itself then waits for that
     * write to finish. It runs in the app's scope, which outlives the activity —
     * being stopped is what starts it, so a scope tied to the activity would end
     * it at the same moment.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")

        MyApp.appModule.appScope.launch {
            if (!MyApp.appModule.appPreferences.syncOnOpenAndClose.get()) return@launch

            MyApp.appModule.storageManager.syncWithRemote(announceErrors = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")
    }
}
