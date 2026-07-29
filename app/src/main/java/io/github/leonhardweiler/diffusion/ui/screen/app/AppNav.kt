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
    onCloseRepo: () -> Unit,
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
                onCloseRepo = onCloseRepo
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

