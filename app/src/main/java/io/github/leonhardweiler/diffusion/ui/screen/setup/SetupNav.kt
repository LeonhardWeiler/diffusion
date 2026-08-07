package io.github.leonhardweiler.diffusion.ui.screen.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.ui.destination.SetupDestination
import io.github.leonhardweiler.diffusion.ui.screen.setup.remote.RemoteScreen
import io.github.leonhardweiler.diffusion.ui.utils.slide
import io.github.leonhardweiler.diffusion.ui.viewmodel.SetupViewModel

private const val TAG = "SetupNav"

@Composable
fun SetupNav(
    startDestination: SetupDestination,
    onSetupSuccess: () -> Unit,
    /**
     * The way out of a setup that was reached from the settings, for a
     * repository beside the ones there already are. Null for the first one:
     * there is nothing underneath it to go back to.
     */
    onBackClick: (() -> Unit)? = null,
) {

    val vm: SetupViewModel = viewModel()

    val backstack = rememberBackstack(startDestination)

    NavHost(
        backstack = backstack,
        onBack = onBackClick,
        transition = { _, _, wentBack -> slide(backWard = wentBack) },
    ) { setupDestination ->
        when (setupDestination) {

            SetupDestination.Main -> NewRepoMethodScreen(
                openRepo = vm::openRepo,
                checkPathForClone = vm::checkPathForClone,
                finishWithoutRemote = vm::finishWithoutRemote,
                makeToast = vm.uiHelper::makeToast,
                navigate = backstack::navigate,
                onSetupSuccess = onSetupSuccess,
                onBackClick = onBackClick,
                initState = vm.initState.collectAsState().value,
            )

            is SetupDestination.Remote -> RemoteScreen(
                vm = vm,
                storageConfig = setupDestination.storageConfig,
                openedRemoteUrl = setupDestination.openedRemoteUrl,
                alreadyOnDevice = setupDestination.alreadyOnDevice,
                onInitSuccess = onSetupSuccess,
                onBackClick = { backstack.pop() }
            )
        }
    }
}

