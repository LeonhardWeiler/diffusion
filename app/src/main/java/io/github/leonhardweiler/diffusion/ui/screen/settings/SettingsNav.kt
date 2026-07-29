package io.github.leonhardweiler.diffusion.ui.screen.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.ui.destination.SettingsDestination
import io.github.leonhardweiler.diffusion.ui.utils.slide
import io.github.leonhardweiler.diffusion.ui.viewmodel.SettingsViewModel


// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/AboutSettingsScreen.kt
// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/LicensesScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNav(
    destination: SettingsDestination,
    onBackClick: () -> Unit,
    onCloseRepo: () -> Unit,
) {

    val backstack = rememberBackstack(destination)

    val vm: SettingsViewModel = viewModel()

    NavHost(
        backstack = backstack,
        onBack = onBackClick,
        transition = { _, _, wentBack -> slide(backWard = wentBack) },
    ) {
        when (it) {

            SettingsDestination.Logs -> {
                LogsScreen(
                    onBackClick = { backstack.pop() },
                )
            }

            SettingsDestination.Main -> {
                SettingsScreen(
                    onBackClick = onBackClick,
                    onShowLogs = { backstack.navigate(SettingsDestination.Logs) },
                    onCloseRepo = onCloseRepo,
                    vm = vm
                )
            }
        }
    }
}


