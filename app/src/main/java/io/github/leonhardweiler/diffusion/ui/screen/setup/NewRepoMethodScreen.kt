package io.github.leonhardweiler.diffusion.ui.screen.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.platform.pickedFolderPath
import io.github.leonhardweiler.diffusion.data.platform.primaryStorageUri
import io.github.leonhardweiler.diffusion.helper.StoragePermissionHelper
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.RequestConfirmationDialog
import io.github.leonhardweiler.diffusion.ui.destination.NewRepoMethod
import io.github.leonhardweiler.diffusion.ui.destination.SetupDestination
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import io.github.leonhardweiler.diffusion.ui.viewmodel.InitState
import kotlinx.coroutines.launch


private const val TAG = "NewRepoMethodScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRepoMethodScreen(
    openRepo: (StorageConfiguration, () -> Unit, (String) -> Unit, () -> Unit) -> Unit,
    checkPathForClone: (String) -> Result<Unit>,
    makeToast: (String) -> Unit,
    navigate: (SetupDestination) -> Unit,
    onSetupSuccess: () -> Unit,
    initState: InitState = InitState.Idle,
) {


    val newRepoMethod: MutableState<NewRepoMethod?> =
        remember { mutableStateOf(null) }

    // an opened repository without a remote: it works as it is, but nothing it
    // holds would ever leave the device
    val repoWithoutRemote: MutableState<StorageConfiguration?> = remember { mutableStateOf(null) }
    val askAboutRemote = remember { mutableStateOf(false) }

    val storagePermissionHelper = remember {
        StoragePermissionHelper()
    }
    val (contract, permissionName) = storagePermissionHelper.permissionContract()

    // The system picker only says which folder was chosen; reading and writing it
    // is what the storage permission is for, so both are still needed.
    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val path = pickedFolderPath(uri)
            if (path == null) {
                makeToast(MyApp.appModule.context.getString(R.string.error_folder_not_on_device))
                return@rememberLauncherForActivityResult
            }

            val storageConfig = StorageConfiguration(path)
            when (newRepoMethod.value!!) {
                NewRepoMethod.Open -> openRepo(
                    storageConfig,
                    onSetupSuccess,
                    { remoteUrl -> navigate(SetupDestination.Remote(storageConfig, remoteUrl)) },
                    {
                        repoWithoutRemote.value = storageConfig
                        askAboutRemote.value = true
                    },
                )

                // said now rather than after the remote has been set up
                NewRepoMethod.Clone ->
                    if (checkPathForClone(storageConfig.repoPath()).isSuccess) {
                        navigate(SetupDestination.Remote(storageConfig))
                    }
            }
        }

    val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
        if (it) {
            folderPicker.launch(primaryStorageUri())
        } else {
            makeToast(MyApp.appModule.context.getString(R.string.error_need_storage_permission))
        }
    }

    fun pickFolder() {
        if (StoragePermissionHelper.isPermissionGranted()) {
            folderPicker.launch(primaryStorageUri())
        } else {
            permissionLauncher.launch(permissionName)
        }
    }

    AppPage(
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Opening a repository is a second or two of libgit2 and of reading the
        // whole working tree, and it happens after the folder picker has closed
        // — so without this the app was back on this screen doing nothing
        // visible, and the obvious thing to do was to tap again.
        if (initState.isLoading()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = initState.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            return@AppPage
        }

        Button(
            onClick = {
                newRepoMethod.value = NewRepoMethod.Open
                pickFolder()
            }
        ) {
            Text(
                text = stringResource(R.string.open_repo)
            )
        }


        Button(
            onClick = {
                newRepoMethod.value = NewRepoMethod.Clone
                pickFolder()
            }
        ) {
            Text(
                text = stringResource(R.string.clone_remote_repo)
            )
        }
    }


    RequestConfirmationDialog(
        expanded = askAboutRemote,
        text = stringResource(R.string.set_up_remote_question),
        onConfirmation = {
            repoWithoutRemote.value?.let { navigate(SetupDestination.Remote(it)) }
        },
        onDecline = onSetupSuccess,
    )

}

@Preview
@Composable
private fun NewRepoMethodScreenPreview() {

    NewRepoMethodScreen(
        openRepo = { _, _, _, _ -> },
        checkPathForClone = { Result.success(Unit) },
        makeToast = {},
        navigate = {},
        onSetupSuccess = {}
    )
}