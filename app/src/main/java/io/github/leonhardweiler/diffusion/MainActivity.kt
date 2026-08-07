package io.github.leonhardweiler.diffusion

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
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
                // remember, never rememberSaveable: the open repository and the
                // note index are process state while the backstack survives in
                // the bundle, so a killed process came back to an empty list
                val repoOpened = remember { runBlocking { vm.tryInit() } }

                val startDestination: Destination = remember {
                    when {
                        repoOpened -> vm.currentDestination()

                        else -> runBlocking { vm.repoAwaitingPermission() }
                            ?.let { Destination.MissingPermission(it) }
                            ?: Destination.Setup(SetupDestination.Main)
                    }
                }

                val backstack = rememberBackstack(startDestination)

                LaunchedEffect(Unit) {
                    if (!repoOpened && backstack.current is Destination.App) {
                        backstack.replaceAll(startDestination)
                    }
                }

                NavHost(
                    backstack = backstack,
                    transition = { _, _, _ -> crossFade() },
                ) { destination ->
                    when (destination) {
                        is Destination.Setup -> SetupNav(
                            startDestination = destination.setupDestination,
                            onBackClick = if (backstack.entries.size > 1) {
                                { backstack.pop() }
                            } else {
                                null
                            },
                            onSetupSuccess = {
                                backstack.replaceAll(vm.currentDestination())
                            }
                        )

                        is Destination.App -> AppScreen(
                            appDestination = destination.appDestination,
                            onAddRepo = {
                                backstack.navigate(Destination.Setup(SetupDestination.Main))
                            },
                            onRepoChanged = {
                                backstack.replaceAll(vm.currentDestination())
                            }
                        )

                        is Destination.MissingPermission -> {
                            val scope = rememberCoroutineScope()

                            StoragePermissionScreen(
                                repoPath = destination.repoPath,
                                onGranted = {
                                    scope.launch {
                                        if (vm.tryInit()) {
                                            backstack.replaceAll(vm.currentDestination())
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

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")

        MyApp.appModule.appScope.launch {
            MyApp.appModule.repoManager.syncAllQuietly()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")
    }
}
