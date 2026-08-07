package io.github.leonhardweiler.diffusion.ui.screen.settings

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.BuildConfig
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.DefaultSettingsRow
import io.github.leonhardweiler.diffusion.ui.component.MultipleChoiceSettings
import io.github.leonhardweiler.diffusion.ui.component.SettingsSection
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.screen.app.grid.SyncButton
import io.github.leonhardweiler.diffusion.ui.theme.Theme
import io.github.leonhardweiler.diffusion.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onShowLogs: () -> Unit,
    onAddRepo: () -> Unit,
    onRepoSettings: (RepoSession) -> Unit,
    onRepoChanged: () -> Unit,
    vm: SettingsViewModel
) {

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
            title = stringResource(R.string.repositories)
        ) {

            // One row per repository: what it is called, its cloud button and
            // the gear that leads to everything that belongs to it alone.
            // Tapping the row itself is switching to it, which is the only way
            // there is — the note list shows one repository and never two.
            val repos by vm.repos.collectAsStateWithLifecycle()

            repos.forEach { repository ->
                RepositoryRow(
                    repo = repository,
                    isActive = repository.id == vm.activeRepo.id,
                    onClick = { vm.switchTo(repository, onRepoChanged) },
                    onSettingsClick = { onRepoSettings(repository) },
                    onSyncClick = { vm.sync(repository) },
                )
            }

            DefaultSettingsRow(
                title = stringResource(R.string.add_repository),
                startIcon = Icons.Default.Add,
                onClick = onAddRepo
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

/**
 * One repository in the settings: its name, where it is, and the two things
 * there are to do to it without leaving this screen.
 *
 * The cloud is the same button the note list carries, saying the same things
 * about this repository — how its last sync went, and whether it holds anything
 * the remote has not been told about. Tapping the row is switching to it.
 *
 * @param isActive whether this is the repository being looked at. Its path is
 * shown either way; what marks it is the word under the name, because a tick
 * beside a row that is also a button reads as something to press.
 */
@Composable
private fun RepositoryRow(
    repo: RepoSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    val syncState by repo.storageManager.syncState.collectAsStateWithLifecycle()
    val hasLocalChanges by repo.storageManager.hasLocalChanges.collectAsStateWithLifecycle()

    DefaultSettingsRow(
        title = repo.name,
        subTitle = if (isActive) stringResource(R.string.repository_shown) else repo.path,
        showFullText = false,
        onClick = onClick,
        endContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SyncButton(
                    state = syncState,
                    hasLocalChanges = hasLocalChanges,
                    onClick = onSyncClick,
                )

                IconButton(onClick = onSettingsClick) {
                    SimpleIcon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.repository_settings,
                            repo.name
                        )
                    )
                }
            }
        },
    )
}
