package io.github.leonhardweiler.diffusion.ui.screen.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.helper.StoragePermissionHelper
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage

/**
 * The one screen a repository that is already set up can still end at.
 *
 * Reading a repository needs the permission to read all files, and that
 * permission can be gone while everything else is still here — a new build
 * installed over the old one is the usual way. What is stored says exactly
 * where the notes are, so there is nothing to choose and nothing to set up
 * again: the way back in is the permission and the folder it names.
 *
 * @param repoPath the repository the app is holding on to, shown so that the
 * screen is about something rather than about a permission in general.
 * @param onGranted asked again with the permission in hand — which is what
 * decides whether this was all that was missing.
 * @param onGiveUp forgets the repository and starts the setup over. For the
 * case this screen cannot fix: the folder is really gone.
 */
@Composable
fun StoragePermissionScreen(
    repoPath: String,
    onGranted: () -> Unit,
    onGiveUp: () -> Unit,
) {

    val storagePermissionHelper = remember { StoragePermissionHelper() }
    val (contract, permissionName) = storagePermissionHelper.permissionContract()

    val permissionLauncher = rememberLauncherForActivityResult(contract = contract) { granted ->
        if (granted) onGranted()
    }

    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        SetupPage(
            title = stringResource(R.string.storage_permission_needed_title),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            SetupLine(
                text = stringResource(R.string.storage_permission_needed_text),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    text = repoPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SetupButton(
                    text = stringResource(R.string.grant_storage_permission),
                    onClick = { permissionLauncher.launch(permissionName) }
                )

                SetupButton(
                    text = stringResource(R.string.choose_another_repo),
                    onClick = onGiveUp
                )
            }
        }
    }
}

@Preview
@Composable
private fun StoragePermissionScreenPreview() {
    StoragePermissionScreen(
        repoPath = "/storage/emulated/0/Documents/notes",
        onGranted = {},
        onGiveUp = {},
    )
}
