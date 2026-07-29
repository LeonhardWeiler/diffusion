package io.github.leonhardweiler.diffusion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.data.platform.openedFilePath
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.ui.destination.AppDestination
import io.github.leonhardweiler.diffusion.ui.destination.Destination
import io.github.leonhardweiler.diffusion.ui.destination.EditParams
import io.github.leonhardweiler.diffusion.ui.destination.SetupDestination
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.screen.app.AppScreen
import io.github.leonhardweiler.diffusion.ui.screen.setup.SetupNav
import io.github.leonhardweiler.diffusion.ui.theme.DiffusionTheme
import io.github.leonhardweiler.diffusion.ui.utils.crossFade
import io.github.leonhardweiler.diffusion.ui.theme.Theme
import io.github.leonhardweiler.diffusion.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    /**
     * The file another app asked this one to open, until the composition has
     * done something with it.
     *
     * A field rather than something read straight out of [getIntent], because
     * the activity is `singleTop`: a second file tapped while the app is already
     * running arrives at [onNewIntent], with the composition long since built.
     */
    private val pendingFile = mutableStateOf<Uri?>(null)

    /** Only a file, and only an intent that means to open one. */
    private fun rememberIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_EDIT) return
        pendingFile.value = intent.data
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rememberIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        rememberIntent(intent)

        setContent {

            val vm: MainViewModel = viewModel()

            val theme by vm.prefs.theme.getAsState()

            DiffusionTheme(
                darkTheme = (theme == Theme.SYSTEM && isSystemInDarkTheme()) || theme == Theme.DARK,
            ) {

                val startDestination: Destination = rememberSaveable {
                    if (runBlocking { vm.tryInit() }) {
                        Destination.App(AppDestination.Grid)
                    } else Destination.Setup(SetupDestination.Main)
                }

                val backstack = rememberBackstack(startDestination)

                // A markdown file this app was handed by another one. Resolved
                // here rather than in onCreate, because it takes the database
                // and the repository, both of which tryInit is what opens.
                //
                // It replaces the backstack instead of adding to it: nothing was
                // asked for beyond this note, so leaving the editor leaves the
                // app the same way it was entered.
                val pendingFileUri = pendingFile.value
                LaunchedEffect(pendingFileUri) {
                    if (pendingFileUri == null) return@LaunchedEffect
                    pendingFile.value = null

                    val path = openedFilePath(pendingFileUri)
                    if (path == null) {
                        Log.d(TAG, "no path behind $pendingFileUri")
                        vm.uiHelper.makeToast(
                            vm.uiHelper.getString(R.string.error_file_outside_repo)
                        )
                        return@LaunchedEffect
                    }

                    val note = vm.noteFromFile(path) ?: return@LaunchedEffect

                    backstack.replaceAll(
                        Destination.App(
                            AppDestination.Edit(EditParams(note, EditType.Update))
                        )
                    )
                }

                NavHost(
                    backstack = backstack,
                    // The setup and the app are not two steps of one path, so
                    // neither slides into the other.
                    transition = { _, _, _ -> crossFade() },
                ) { destination ->
                    when (destination) {
                        is Destination.Setup -> SetupNav(
                            startDestination = destination.setupDestination,
                            onSetupSuccess = {
                                backstack.replaceAll(Destination.App(AppDestination.Grid))
                            }
                        )

                        is Destination.App -> AppScreen(
                            appDestination = destination.appDestination,
                            onCloseRepo = {
                                backstack.replaceAll(
                                    Destination.Setup(SetupDestination.Main)
                                )
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
