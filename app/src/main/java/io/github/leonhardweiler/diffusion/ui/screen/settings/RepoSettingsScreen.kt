package io.github.leonhardweiler.diffusion.ui.screen.settings

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.helper.repoWebUrl
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.DefaultSettingsRow
import io.github.leonhardweiler.diffusion.ui.component.RequestConfirmationDialog
import io.github.leonhardweiler.diffusion.ui.component.SettingsSection
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.component.StringSettings
import io.github.leonhardweiler.diffusion.ui.component.ToggleableSettings
import io.github.leonhardweiler.diffusion.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Everything that belongs to one repository and to no other: who its commits
 * are by, where it pushes to, which key it takes, where its folder is and
 * whether it syncs by itself.
 *
 * These used to be the settings, because there was one repository. They are
 * reached from that repository's row now — the gear beside it — and the settings
 * screen itself is left with the things that are the app's rather than a
 * repository's.
 */
@Composable
fun RepoSettingsScreen(
    repo: RepoSession,
    onBackClick: () -> Unit,
    onRemoved: (wasShown: Boolean) -> Unit,
    vm: SettingsViewModel,
) {

    AppPage(
        title = repo.name,
        onBackClick = onBackClick,
    ) {

        SettingsSection(
            title = stringResource(R.string.repository),
            isLast = true,
        ) {

            val authorName by repo.prefs.authorName.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_name),
                subtitle = authorName.ifEmpty { stringResource(id = R.string.none) },
                stringValue = authorName,
                onChange = { updated ->
                    vm.update { repo.prefs.authorName.update(updated.trim()) }
                }
            )

            val authorEmail by repo.prefs.authorEmail.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_email),
                subtitle = authorEmail.ifEmpty { stringResource(id = R.string.none) },
                stringValue = authorEmail,
                onChange = { updated ->
                    vm.update { repo.prefs.authorEmail.update(updated.trim()) }
                },
                keyboardType = KeyboardType.Email
            )

            val remoteUrl by repo.prefs.remoteUrl.getAsState()

            SshKeyRow(repo = repo, remoteUrl = remoteUrl, vm = vm)

            StringSettings(
                title = stringResource(R.string.remote_url),
                subtitle = remoteUrl.ifEmpty { stringResource(id = R.string.none) },
                stringValue = remoteUrl,
                onChange = { vm.updateRemoteUrl(repo, it) },
                endContent = { OpenRepositoryButton(remoteUrl = remoteUrl, vm = vm) },
                showFullText = false,
                keyboardType = KeyboardType.Uri
            )

            // Where the notes are. Not something to type: the folder was chosen
            // with the system picker, and moving a repository is moving a
            // directory, which is not this app's to do.
            DefaultSettingsRow(
                title = stringResource(R.string.repository_folder),
                subTitle = repo.path,
                startIcon = Icons.Default.Folder,
            )

            val syncOnOpenAndClose by repo.prefs.syncOnOpenAndClose.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.sync_automatically),
                subtitle = stringResource(R.string.sync_automatically_subtitle),
                checked = syncOnOpenAndClose,
                onCheckedChange = {
                    vm.update { repo.prefs.syncOnOpenAndClose.update(it) }
                }
            )

            val expanded = rememberSaveable { mutableStateOf(false) }

            DefaultSettingsRow(
                title = stringResource(R.string.close_repository),
                subTitle = stringResource(R.string.close_repository_subtitle),
                startIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = { expanded.value = true }
            )

            RequestConfirmationDialog(
                expanded = expanded,
                text = stringResource(R.string.close_repository_confirmation),
                onConfirmation = { vm.removeRepo(repo, onRemoved) }
            )
        }
    }
}

/**
 * The public key this repository authenticates with, with the two things there
 * are to do with it: take it away to paste as a deploy key, and replace it.
 *
 * Regenerating asks first, and says what it costs — the pair is written over,
 * every repository that takes this key gets the new one, and none of them can
 * reach its remote again until the new key has been added there.
 */
@Composable
private fun SshKeyRow(
    repo: RepoSession,
    remoteUrl: String,
    vm: SettingsViewModel,
) {
    val keyId by repo.prefs.sshKeyId.getAsState()
    val keys by vm.sshKeys.collectAsStateWithLifecycle(initialValue = emptyList())

    val publicKey = keys.firstOrNull { it.id == keyId }?.publicKey.orEmpty()

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val regenerating = rememberSaveable { mutableStateOf(false) }

    DefaultSettingsRow(
        title = stringResource(R.string.ssh_key),
        subTitle = publicKey.ifEmpty { stringResource(R.string.none) },
        showFullText = false,
        // One child of the row, not three: what a settings row puts on the
        // right is a single thing, and three of them would be spread across it.
        endContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    enabled = publicKey.isNotEmpty(),
                    onClick = {
                        val data = ClipData(
                            ClipDescription(
                                "public ssh key",
                                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            ),
                            ClipData.Item(publicKey)
                        )

                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(data))
                            vm.uiHelper.makeToast(vm.uiHelper.getString(R.string.key_copied))
                        }
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_key)
                    )
                }

                Button(onClick = { regenerating.value = true }) {
                    SimpleIcon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = stringResource(R.string.regenerate_key)
                    )
                }

                OpenRepositoryButton(remoteUrl = remoteUrl, vm = vm)
            }
        },
    )

    RequestConfirmationDialog(
        expanded = regenerating,
        text = stringResource(R.string.regenerate_key_confirmation),
        onConfirmation = { vm.regenerateSshKey(repo) }
    )
}

/**
 * The way to the repository's page, which is where a deploy key is added. An
 * ssh remote is not an address a browser can follow, so it is offered the https
 * form of it, and nothing is shown for an address with no page behind it.
 */
@Composable
private fun OpenRepositoryButton(remoteUrl: String, vm: SettingsViewModel) {
    val uriHandler = LocalUriHandler.current

    Button(
        onClick = {
            val link = repoWebUrl(remoteUrl)
            if (link == null) {
                vm.uiHelper.makeToast(vm.uiHelper.getString(R.string.error_invalid_link))
                return@Button
            }
            try {
                uriHandler.openUri(link)
            } catch (_: Exception) {
                vm.uiHelper.makeToast(vm.uiHelper.getString(R.string.error_invalid_link))
            }
        }
    ) {
        SimpleIcon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(R.string.open_in_browser)
        )
    }
}
