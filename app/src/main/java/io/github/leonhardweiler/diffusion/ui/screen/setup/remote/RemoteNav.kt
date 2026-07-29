package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.leonhardweiler.diffusion.ui.navigation.NavHost
import io.github.leonhardweiler.diffusion.ui.navigation.rememberBackstack
import io.github.leonhardweiler.diffusion.helper.sshFingerprint
import io.github.leonhardweiler.diffusion.manager.generateSshKeysLib
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.EnterUrl
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.GenerateNewKeys
import io.github.leonhardweiler.diffusion.ui.destination.RemoteDestination.SelectGenerateNewSshKeys
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import io.github.leonhardweiler.diffusion.ui.screen.settings.LogsScreen
import io.github.leonhardweiler.diffusion.ui.utils.slide
import io.github.leonhardweiler.diffusion.ui.viewmodel.SetupViewModel


private const val TAG = "RemoteScreen"


@Composable
fun RemoteScreen(
    vm: SetupViewModel,
    storageConfig: StorageConfiguration,
    onInitSuccess: () -> Unit,
    onBackClick: () -> Unit,
    openedRemoteUrl: String? = null,
) {

    // A repository that is already on the device brings its remote with it, so
    // the questions leading up to a url have nothing left to ask — unless that
    // remote is an https one, which this app cannot use: then it goes through
    // the url screen with the old address in the field, and setting the ssh one
    // writes it into the repository.
    val startDestination = when {
        openedRemoteUrl == null -> EnterUrl()
        isCloneUrlSupported(openedRemoteUrl) -> SelectGenerateNewSshKeys(url = openedRemoteUrl)
        else -> EnterUrl(defaultUrl = openedRemoteUrl)
    }

    val backstack = rememberBackstack(startDestination)

    // a credential screen can be the first thing shown, and popping the only
    // entry would leave an empty backstack behind
    fun back() {
        if (!backstack.pop()) onBackClick()
    }

    // The two key screens differ in where the credentials come from and in
    // nothing else, so this is all they are given of the setup.
    fun clone(url: String, cred: Cred) {
        vm.cloneRepo(
            storageConfig = storageConfig,
            remoteUrl = url,
            cred = cred,
            onSuccess = onInitSuccess
        )
        backstack.navigate(RemoteDestination.Cloning)
    }

    val initState = vm.initState.collectAsState().value
    val storedSshKey = vm.storedSshKey.collectAsState().value

    NavHost(
        backstack = backstack,
        onBack = onBackClick,
        transition = { _, _, wentBack -> slide(backWard = wentBack) },
    ) { remoteDestination ->

        when (remoteDestination) {
            is EnterUrl -> EnterUrlScreen(
                onBackClick = { back() },
                defaultUrl = remoteDestination.defaultUrl,
                onUrl = { url -> backstack.navigate(SelectGenerateNewSshKeys(url = url)) }
            )

            is SelectGenerateNewSshKeys -> SelectGenerateNewSshKeysScreen(
                onBackClick = { back() },
                storedKeyFingerprint = storedSshKey?.let { sshFingerprint(it.publicKey) },
                onUseStored = {
                    backstack.navigate(
                        GenerateNewKeys(
                            url = remoteDestination.url,
                            useStored = true
                        )
                    )
                },
                onGenerate = {
                    backstack.navigate(
                        GenerateNewKeys(
                            url = remoteDestination.url
                        )
                    )
                },
                onCustom = {
                    backstack.navigate(
                        RemoteDestination.LoadKeysFromDevice(
                            url = remoteDestination.url
                        )
                    )
                }
            )

            is GenerateNewKeys -> GenerateNewSshKeysScreen(
                onBackClick = { backstack.pop() },
                cloneState = initState,
                remoteUrl = remoteDestination.url,
                generateSshKeys = ::generateSshKeysLib,
                cloneWith = { cred -> clone(remoteDestination.url, cred) },
                storedKey = storedSshKey.takeIf { remoteDestination.useStored },
            )

            is RemoteDestination.LoadKeysFromDevice -> LoadKeysFromDeviceScreen(
                onBackClick = { backstack.pop() },
                cloneState = initState,
                cloneWith = { cred -> clone(remoteDestination.url, cred) },
            )

            RemoteDestination.Cloning -> CloningScreen(
                cloneState = initState,
                onCancel = {
                    if (vm.cancelClone())
                        backstack.pop()
                },
                onShowLogs = {
                    backstack.navigate(RemoteDestination.Logs)
                }

            )

            RemoteDestination.Logs -> {
                LogsScreen(
                    onBackClick = {
                        backstack.pop()
                    },
                )
            }
        }
    }

    // After the NavHost, not before it: of two back handlers the one composed
    // last is the one asked first, and while a clone is running the answer has
    // to be "nothing happens" rather than "go back a screen".
    BackHandler(enabled = initState.isLoading()) {
        // a clone is not a thing to walk out of half way
    }
}


