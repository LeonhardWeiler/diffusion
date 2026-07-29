package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.leonhardweiler.diffusion.manager.generateSshKeysLib
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.EnterUrl
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.GenerateNewKeys
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.SelectGenerateNewSshKeys
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import io.github.leonhardweiler.diffusion.ui.screen.settings.LogsScreen
import io.github.leonhardweiler.diffusion.ui.utils.crossFade
import io.github.leonhardweiler.diffusion.ui.utils.slide
import io.github.leonhardweiler.diffusion.ui.viewmodel.SetupViewModel


private const val TAG = "RemoteScreen"


@Composable
fun RemoteScreen(
    vm: SetupViewModel,
    storageConfig: StorageConfiguration,
    onInitSuccess: () -> Unit,
    onBackClick: () -> Unit,
    openedRemoteUrl: String? = null,
) {

    // A repository that is already on the device brings its remote with it, so
    // the questions leading up to a url have nothing left to ask — unless that
    // remote is an https one, which this app cannot use: then it goes through
    // the url screen with the old address in the field, and setting the ssh one
    // writes it into the repository.
    val startDestination = when {
        openedRemoteUrl == null -> EnterUrl()
        isCloneUrlSupported(openedRemoteUrl) -> SelectGenerateNewSshKeys(url = openedRemoteUrl)
        else -> EnterUrl(defaultUrl = openedRemoteUrl)
    }

    val navController: NavController<RemoteDestination> =
        rememberNavController(startDestination = startDestination)

    NavBackHandler(navController)

    // a credential screen can be the first thing shown, and popping the only
    // entry would leave an empty backstack behind
    fun back() {
        if (navController.backstack.entries.size > 1) navController.pop() else onBackClick()
    }

    val initState = vm.initState.collectAsState().value

    BackHandler(
        enabled = initState.isLoading()
    ) {
        // do nothing
    }

    AnimatedNavHost(
        controller = navController,
        transitionSpec = RemoteNavTransitionSpec
    ) { remoteDestination ->

        when (remoteDestination) {
            is EnterUrl -> EnterUrlScreen(
                onBackClick = { back() },
                defaultUrl = remoteDestination.defaultUrl,
                onUrl = { url -> navController.navigate(SelectGenerateNewSshKeys(url = url)) }
            )

            is SelectGenerateNewSshKeys -> SelectGenerateNewSshKeysScreen(
                onBackClick = { back() },
                onGenerate = {
                    navController.navigate(
                        GenerateNewKeys(
                            url = remoteDestination.url
                        )
                    )
                },
                onCustom = {
                    navController.navigate(
                        RemoteDestination.LoadKeysFromDevice(
                            url = remoteDestination.url
                        )
                    )
                }
            )

            is GenerateNewKeys -> GenerateNewSshKeysScreen(
                onBackClick = { navController.pop() },
                cloneState = initState,
                storageConfig = storageConfig,
                url = remoteDestination.url,
                vm = vm,
                generateSshKeys = ::generateSshKeysLib,
                onSuccess = onInitSuccess,
                onClone = { navController.navigate(RemoteDestination.Cloning) }
            )

            is RemoteDestination.LoadKeysFromDevice -> LoadKeysFromDeviceScreen(
                onBackClick = { navController.pop() },
                cloneState = initState,
                storageConfig = storageConfig,
                url = remoteDestination.url,
                vm = vm,
                onSuccess = onInitSuccess,
                onClone = { navController.navigate(RemoteDestination.Cloning) }
            )

            RemoteDestination.Cloning -> CloningScreen(
                cloneState = initState,
                onCancel = {
                    if (vm.cancelClone())
                        navController.pop()
                },
                onShowLogs = {
                    navController.navigate(RemoteDestination.Logs)
                }

            )

            RemoteDestination.Logs -> {
                LogsScreen(
                    onBackClick = {
                        navController.pop()
                    },
                )
            }
        }
    }
}


private object RemoteNavTransitionSpec : NavTransitionSpec<RemoteDestination> {


    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: RemoteDestination,
        to: RemoteDestination
    ): ContentTransform {

        // Navigate and Pop are the two the setup produces itself. The rest —
        // Replace, and the Idle a restored backstack comes back with — has no
        // direction to it, and used to end the app with a NotImplementedError.
        return when (action) {
            is NavAction.Navigate -> slide()
            is NavAction.Pop -> slide(backWard = true)
            else -> crossFade()
        }

    }
}

