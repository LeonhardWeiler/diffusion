package io.github.wiiznokes.gitnote.ui.screen.setup

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.ui.destination.SetupDestination
import io.github.wiiznokes.gitnote.ui.screen.setup.remote.RemoteScreen
import io.github.wiiznokes.gitnote.ui.utils.crossFade
import io.github.wiiznokes.gitnote.ui.utils.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModel

private const val TAG = "SetupNav"

@Composable
fun SetupNav(
    startDestination: SetupDestination,
    onSetupSuccess: () -> Unit,
) {

    val vm: SetupViewModel = viewModel()

    val navController =
        rememberNavController(startDestination = startDestination)

    NavBackHandler(navController)

    AnimatedNavHost(
        controller = navController,
        transitionSpec = InitNavTransitionSpec
    ) { setupDestination ->
        when (setupDestination) {

            SetupDestination.Main -> NewRepoMethodScreen(
                openRepo = vm::openRepo,
                checkPathForClone = vm::checkPathForClone,
                makeToast = vm.uiHelper::makeToast,
                navigate = navController::navigate,
                onSetupSuccess = onSetupSuccess,
            )

            is SetupDestination.Remote -> RemoteScreen(
                vm = vm,
                storageConfig = setupDestination.storageConfig,
                openedRemoteUrl = setupDestination.openedRemoteUrl,
                onInitSuccess = onSetupSuccess,
                onBackClick = {
                    navController.pop()
                }
            )
        }
    }
}

private object InitNavTransitionSpec : NavTransitionSpec<SetupDestination> {

    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: SetupDestination,
        to: SetupDestination
    ): ContentTransform {

        return when (from) {
            SetupDestination.Main -> when (to) {
                SetupDestination.Main -> crossFade()
                is SetupDestination.Remote -> slide()
            }

            is SetupDestination.Remote -> when (to) {
                SetupDestination.Main -> slide(backWard = true)
                is SetupDestination.Remote -> crossFade()
            }
        }
    }
}
