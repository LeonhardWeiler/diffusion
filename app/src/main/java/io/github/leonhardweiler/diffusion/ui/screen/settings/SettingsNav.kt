package io.github.leonhardweiler.diffusion.ui.screen.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onAddRepo: () -> Unit,
    onRepoChanged: () -> Unit,
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
                    onAddRepo = onAddRepo,
                    onRepoSettings = { backstack.navigate(SettingsDestination.Repo(it.id)) },
                    onRepoChanged = onRepoChanged,
                    vm = vm
                )
            }

            is SettingsDestination.Repo -> {
                // A repository that is not there anymore is one this screen has
                // nothing to say about: the row it was reached from is gone, so
                // the way back is the only thing left.
                val repo = vm.repoById(it.repoId)

                if (repo == null) {
                    LaunchedEffect(Unit) { backstack.pop() }
                } else {
                    RepoSettingsScreen(
                        repo = repo,
                        onBackClick = { backstack.pop() },
                        // Letting go of a repository that was not the one being
                        // looked at changes nothing but this list; letting go of
                        // the one that was leaves the whole app standing on
                        // another repository, or on the setup.
                        onRemoved = { wasShown ->
                            if (wasShown) onRepoChanged() else backstack.pop()
                        },
                        vm = vm,
                    )
                }
            }
        }
    }
}


