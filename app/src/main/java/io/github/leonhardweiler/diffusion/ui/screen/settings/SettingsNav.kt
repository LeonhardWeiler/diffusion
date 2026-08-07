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
                val repo = vm.repoById(it.repoId)

                if (repo == null) {
                    LaunchedEffect(Unit) { backstack.pop() }
                } else {
                    RepoSettingsScreen(
                        repo = repo,
                        onBackClick = { backstack.pop() },
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

