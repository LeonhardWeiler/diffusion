package io.github.leonhardweiler.diffusion.ui.screen.settings

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.BuildConfig
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.helper.repoWebUrl
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.DefaultSettingsRow
import io.github.leonhardweiler.diffusion.ui.component.MultipleChoiceSettings
import io.github.leonhardweiler.diffusion.ui.component.RequestConfirmationDialog
import io.github.leonhardweiler.diffusion.ui.component.SettingsSection
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.component.StringSettings
import io.github.leonhardweiler.diffusion.ui.component.ToggleableSettings
import io.github.leonhardweiler.diffusion.ui.theme.Theme
import io.github.leonhardweiler.diffusion.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onShowLogs: () -> Unit,
    onAddRepo: () -> Unit,
    onRepoChanged: () -> Unit,
    vm: SettingsViewModel
) {
    val repo = vm.activeRepo

    AppPage(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
    ) {

        SettingsSection(
            title = stringResource(R.string.user_interface)
        ) {

            val theme by vm.prefs.theme.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.theme),
                subtitle = theme.toString(),
                startIcon = Icons.Default.Palette,
                options = Theme.entries,
                onOptionClick = {
                    vm.update { vm.prefs.theme.update(it) }
                }
            )

        }

        SettingsSection(
            title = stringResource(R.string.repository)
        ) {

            val gitAuthorName by repo.prefs.authorName.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_name),
                subtitle = gitAuthorName.ifEmpty { stringResource(id = R.string.none) },
                stringValue = gitAuthorName,
                onChange = { updated ->
                    vm.update { repo.prefs.authorName.update(updated.trim()) }
                }
            )

            val gitAuthorEmail by repo.prefs.authorEmail.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_email),
                subtitle = gitAuthorEmail.ifEmpty { stringResource(id = R.string.none) },
                stringValue = gitAuthorEmail,
                onChange = { updated ->
                    vm.update { repo.prefs.authorEmail.update(updated.trim()) }
                },
                keyboardType = KeyboardType.Email
            )

            val remoteUrl by repo.prefs.remoteUrl.getAsState()
            StringSettings(
                title = stringResource(R.string.remote_url),
                subtitle = remoteUrl.ifEmpty { stringResource(id = R.string.none) },
                stringValue = remoteUrl,
                onChange = { vm.updateRemoteUrl(repo, it) },
                endContent = {
                    val uriHandler = LocalUriHandler.current
                    Button(
                        onClick = {
                            // an ssh remote is not an address a browser can
                            // follow, so it is offered the https form of it
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
                },
                showFullText = false,
                keyboardType = KeyboardType.Uri
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

            val expanded = rememberSaveable {
                mutableStateOf(false)
            }

            DefaultSettingsRow(
                title = stringResource(R.string.add_repository),
                startIcon = Icons.Default.Add,
                onClick = onAddRepo
            )

            DefaultSettingsRow(
                title = stringResource(R.string.close_repository),
                startIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    expanded.value = true
                }
            )

            RequestConfirmationDialog(
                expanded = expanded,
                text = stringResource(R.string.close_repository_confirmation),
                onConfirmation = {
                    vm.removeRepo(repo, onRepoChanged)
                }
            )
        }

        SettingsSection(
            title = stringResource(R.string.about),
            isLast = true
        ) {
            val version =
                "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}-${
                    BuildConfig.GIT_HASH.substring(
                        0..6
                    )
                }"
            val clipboardManager = LocalClipboard.current

            DefaultSettingsRow(
                title = stringResource(R.string.version),
                subTitle = version,
                onClick = {
                    val data = ClipData(
                        ClipDescription(
                            "version of diffusion",
                            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        ),
                        ClipData.Item(version)
                    )

                    vm.viewModelScope.launch {
                        clipboardManager.setClipEntry(ClipEntry(data))
                    }
                }
            )

            DefaultSettingsRow(
                title = stringResource(R.string.reload_notes),
                startIcon = Icons.Default.Refresh,
                onClick = {
                    vm.reloadIndex()
                }
            )

            DefaultSettingsRow(
                title = stringResource(R.string.show_logs),
                startIcon = Icons.AutoMirrored.Filled.Article,
                onClick = {
                    onShowLogs()
                }
            )

            val uriHandler = LocalUriHandler.current
            DefaultSettingsRow(
                title = stringResource(R.string.report_an_issue),
                startIcon = Icons.Default.BugReport,
                onClick = {
                    uriHandler.openUri("https://github.com/LeonhardWeiler/diffusion/issues")
                }
            )
            DefaultSettingsRow(
                title = stringResource(R.string.source_code),
                startIcon = Icons.Default.Code,
                onClick = {
                    uriHandler.openUri("https://github.com/LeonhardWeiler/diffusion")
                }
            )
        }
    }
}
