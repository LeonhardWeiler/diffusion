package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.helper.openUrlInBrowser
import io.github.leonhardweiler.diffusion.helper.repoWebUrl
import io.github.leonhardweiler.diffusion.ui.component.SetupButton

@Composable
internal fun KeyBox(publicKey: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            text = publicKey,
            maxLines = 1
        )
    }
}

@Composable
internal fun CopyKeyButton(justCopied: MutableState<Boolean>, onCopy: () -> Unit) {
    SetupButton(
        text = if (justCopied.value) {
            stringResource(R.string.key_copied)
        } else {
            stringResource(R.string.copy_key)
        },
        onClick = onCopy
    )
}

@Composable
internal fun ColumnScope.OpenRepositoryButton(remoteUrl: String) {
    val webUrl = remember(remoteUrl) { repoWebUrl(remoteUrl) } ?: return

    val context = LocalContext.current

    SetupButton(
        text = stringResource(R.string.open_git_repository),
        link = true,
        onClick = { openUrlInBrowser(context, webUrl) }
    )
}

@Composable
internal fun StartButton(alreadyOnDevice: Boolean, enabled: Boolean, onClick: () -> Unit) {
    SetupButton(
        text = stringResource(
            if (alreadyOnDevice) R.string.sync_repo else R.string.clone_repo
        ),
        enabled = enabled,
        onClick = onClick,
    )
}
