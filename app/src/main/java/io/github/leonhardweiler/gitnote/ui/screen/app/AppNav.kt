package io.github.leonhardweiler.gitnote.ui.screen.app

import android.util.Log
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.leonhardweiler.gitnote.MyApp
import io.github.leonhardweiler.gitnote.ui.destination.AppDestination
import io.github.leonhardweiler.gitnote.ui.destination.EditParams
import io.github.leonhardweiler.gitnote.ui.destination.SettingsDestination
import io.github.leonhardweiler.gitnote.ui.screen.app.edit.EditScreen
import io.github.leonhardweiler.gitnote.ui.screen.app.grid.GridScreen
import io.github.leonhardweiler.gitnote.ui.screen.settings.SettingsNav
import io.github.leonhardweiler.gitnote.ui.utils.crossFade
import io.github.leonhardweiler.gitnote.ui.utils.slide


private const val TAG = "AppScreen"

@Composable
fun AppScreen(
    appDestination: AppDestination,
    onCloseRepo: () -> Unit,
) {

    val initialBackstack: List<AppDestination> = rememberSaveable {
        buildList {
            add(appDestination)

            MyApp.appModule.noteSaver.getSaveState()?.let { saveInfo ->
                Log.d(TAG, "restoring an unsaved edit")
                add(
                    AppDestination.Edit(
                        EditParams.Saved(
                            note = saveInfo.previousNote,
                            editType = saveInfo.editType,
                            name = saveInfo.name,
                            content = saveInfo.content
                        )
                    )
                )
            }
        }
    }


    val navController =
        rememberNavController(initialBackstack)

    NavBackHandler(navController)

    AnimatedNavHost(
        controller = navController,
        transitionSpec = AppNavTransitionSpec
    ) {
        when (it) {

            is AppDestination.Grid -> {
                GridScreen(
                    onSettingsClick = {
                        navController.navigate(
                            AppDestination.Settings(
                                SettingsDestination.Main
                            )
                        )
                    },
                    onEditClick = { note, editType ->
                        navController.navigate(AppDestination.Edit(EditParams.Idle(note, editType)))
                    },
                )
            }

            is AppDestination.Edit -> EditScreen(
                editParams = it.params,
                onFinished = {
                    navController.pop()
                }
            )

            is AppDestination.Settings -> SettingsNav(
                onBackClick = { navController.pop() },
                destination = it.settingsDestination,
                onCloseRepo = onCloseRepo
            )
        }
    }
}

private object AppNavTransitionSpec : NavTransitionSpec<AppDestination> {

    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: AppDestination,
        to: AppDestination
    ): ContentTransform {

        return when (from) {
            is AppDestination.Edit -> crossFade()
            AppDestination.Grid -> {
                if (to is AppDestination.Settings) {
                    slide()
                } else {
                    crossFade()
                }
            }

            is AppDestination.Settings -> slide(backWard = true)
        }
    }
}

