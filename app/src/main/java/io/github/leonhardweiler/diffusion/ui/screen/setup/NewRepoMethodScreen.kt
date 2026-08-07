package io.github.leonhardweiler.diffusion.ui.screen.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRepoMethodScreen(
    openRepo: (StorageConfiguration, (String) -> Unit, () -> Unit) -> Unit,
    checkPathForClone: (String) -> Result<Unit>,
    finishWithoutRemote: (StorageConfiguration, () -> Unit) -> Unit,
    makeToast: (String) -> Unit,
    navigate: (SetupDestination) -> Unit,
    onSetupSuccess: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    initState: InitState = InitState.Idle,
) {
    val newRepoMethod: MutableState<NewRepoMethod?> =
        remember { mutableStateOf(null) }

    val repoWithoutRemote: MutableState<StorageConfiguration?> = remember { mutableStateOf(null) }
    val askAboutRemote = remember { mutableStateOf(false) }

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
                    { remoteUrl ->
                        navigate(
                            SetupDestination.Remote(
                                storageConfig,
                                remoteUrl,
                                alreadyOnDevice = true
                            )
                        )
                    },
                    {
                        repoWithoutRemote.value = storageConfig
                        askAboutRemote.value = true
                    },
                )

                NewRepoMethod.Clone ->
                    if (checkPathForClone(storageConfig.repoPath()).isSuccess) {
                        navigate(SetupDestination.Remote(storageConfig))
                    }
            }
        }

    val permissionLauncher = rememberLauncherForActivityResult(StoragePermissionHelper.contract) {
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
            permissionLauncher.launch(Unit)
        }
    }

    AppPage(
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        onBackClick = onBackClick,
        onBackClickEnabled = !initState.isLoading(),
    ) {
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
            repoWithoutRemote.value?.let {
                navigate(SetupDestination.Remote(it, alreadyOnDevice = true))
            }
        },
        onDecline = {
            repoWithoutRemote.value?.let { finishWithoutRemote(it, onSetupSuccess) }
        },
    )
}

@Preview
@Composable
private fun NewRepoMethodScreenPreview() {
    NewRepoMethodScreen(
        openRepo = { _, _, _ -> },
        checkPathForClone = { Result.success(Unit) },
        finishWithoutRemote = { _, _ -> },
        makeToast = {},
        navigate = {},
        onSetupSuccess = {}
    )
}
