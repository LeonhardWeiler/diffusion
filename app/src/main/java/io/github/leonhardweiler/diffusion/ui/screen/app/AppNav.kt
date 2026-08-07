package io.github.leonhardweiler.diffusion.ui.screen.app

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.ui.destination.AppDestination
import io.github.leonhardweiler.diffusion.ui.destination.SettingsDestination
import io.github.leonhardweiler.diffusion.ui.screen.app.edit.EditScreen
import io.github.leonhardweiler.diffusion.ui.screen.app.grid.GridScreen
import io.github.leonhardweiler.diffusion.ui.screen.settings.SettingsNav
import io.github.leonhardweiler.diffusion.ui.utils.crossFade
import io.github.leonhardweiler.diffusion.ui.utils.slide



@Composable
fun AppScreen(
    appDestination: AppDestination,
    /** The settings' way into the setup, for a repository beside the ones there are. */
    onAddRepo: () -> Unit,
    /**
     * Another repository is the one being looked at now — switched to, or the
     * one this screen was about having been let go of. Either way this screen is
     * about a repository that is not the current one anymore.
     */
    onRepoChanged: () -> Unit,
) {

    val backstack = rememberBackstack(appDestination)

    NavHost(
        backstack = backstack,
        transition = ::appTransition,
    ) {
        when (it) {

            is AppDestination.Grid -> {
                GridScreen(
                    onSettingsClick = {
                        backstack.navigate(
                            AppDestination.Settings(
                                SettingsDestination.Main
                            )
                        )
                    },
                    onEditClick = { note ->
                        backstack.navigate(AppDestination.Edit(note))
                    },
                )
            }

            is AppDestination.Edit -> EditScreen(
                note = it.note,
                onFinished = {
                    backstack.pop()
                }
            )

            is AppDestination.Settings -> SettingsNav(
                onBackClick = { backstack.pop() },
                destination = it.settingsDestination,
                onAddRepo = onAddRepo,
                onRepoChanged = onRepoChanged,
            )
        }
    }
}

/** Settings slide in from the side; the editor fades, because it is the note. */
private fun appTransition(
    from: AppDestination,
    to: AppDestination,
    wentBack: Boolean,
): ContentTransform = when {
    from is AppDestination.Settings || to is AppDestination.Settings -> slide(backWard = wentBack)
    else -> crossFade()
}

