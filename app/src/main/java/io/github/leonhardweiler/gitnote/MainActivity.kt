package io.github.leonhardweiler.gitnote

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popAll
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.leonhardweiler.gitnote.ui.destination.AppDestination
import io.github.leonhardweiler.gitnote.ui.destination.Destination
import io.github.leonhardweiler.gitnote.ui.destination.SetupDestination
import io.github.leonhardweiler.gitnote.ui.screen.app.AppScreen
import io.github.leonhardweiler.gitnote.ui.screen.setup.SetupNav
import io.github.leonhardweiler.gitnote.ui.theme.GitNoteTheme
import io.github.leonhardweiler.gitnote.ui.theme.Theme
import io.github.leonhardweiler.gitnote.ui.viewmodel.MainViewModel
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
            val dynamicColor by vm.prefs.dynamicColor.getAsState()
            val pureBlack by vm.prefs.pureBlack.getAsState()


            GitNoteTheme(
                darkTheme = (theme == Theme.SYSTEM && isSystemInDarkTheme()) || theme == Theme.DARK,
                dynamicColor = dynamicColor,
                pureBlack = pureBlack,
            ) {

                val startDestination: Destination = rememberSaveable {
                    if (runBlocking { vm.tryInit() }) {
                        Destination.App(AppDestination.Grid)
                    } else Destination.Setup(SetupDestination.Main)
                }

                val navController =
                    rememberNavController(startDestination = startDestination)

                NavBackHandler(navController)

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Setup -> {
                            SetupNav(
                                startDestination = destination.setupDestination,
                                onSetupSuccess = {
                                    navController.popUpTo(
                                        inclusive = true
                                    ) {
                                        it is Destination.Setup
                                    }
                                    navController.navigate(Destination.App(AppDestination.Grid))
                                }
                            )
                        }


                        is Destination.App -> AppScreen(
                            appDestination = destination.appDestination,
                            onCloseRepo = {
                                navController.popAll()
                                navController.navigate(Destination.Setup(SetupDestination.Main))
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * The other half of syncing when the app opens: what was written here goes
     * out when it is left, so that a note does not sit on one device until
     * somebody remembers the button.
     *
     * super first, so that the editor has been told to write what it holds
     * before the sync goes looking for it — the sync itself then waits for that
     * write to finish. It runs in the app's scope, which outlives the activity —
     * being stopped is what starts it, so a scope tied to the activity would end
     * it at the same moment.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")

        MyApp.appModule.appScope.launch {
            if (!MyApp.appModule.appPreferences.syncOnOpenAndClose.get()) return@launch

            MyApp.appModule.storageManager.syncWithRemote(announceErrors = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")
    }
}
