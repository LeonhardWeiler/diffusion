package io.github.wiiznokes.gitnote.ui.screen.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.platform.pickedFolderPath
import io.github.wiiznokes.gitnote.data.platform.primaryStorageUri
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.RequestConfirmationDialog
import io.github.wiiznokes.gitnote.ui.destination.NewRepoMethod
import io.github.wiiznokes.gitnote.ui.destination.SetupDestination
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.coroutines.launch


private const val TAG = "NewRepoMethodScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRepoMethodScreen(
    openRepo: (StorageConfiguration, () -> Unit, (String) -> Unit, () -> Unit) -> Unit,
    checkPathForClone: (String) -> Result<Unit>,
    makeToast: (String) -> Unit,
    navigate: (SetupDestination) -> Unit,
    onSetupSuccess: () -> Unit
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
        title = stringResource(R.string.app_page_choose_method),
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

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