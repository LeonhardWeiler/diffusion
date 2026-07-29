package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.viewmodel.InitState

private const val TAG = "CloningScreen"

/** The most of a libgit2 message that is shown before it has to be scrolled. */
private val MaxDetailHeight = 160.dp

@Composable
fun CloningScreen(
    cloneState: InitState,
    onCancel: () -> Unit,
    onShowLogs: () -> Unit,
) {
    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClickEnabled = !cloneState.isLoading()
    ) {
        if (cloneState is InitState.Error) {
            CloneError(detail = cloneState.message)
        } else {
            Text(text = cloneState.message())
        }

        Spacer(Modifier.height(20.dp))

        if (cloneState is InitState.Error) {
            Button(
                onClick = onShowLogs
            ) {
                Text(stringResource(R.string.show_logs))
            }
        }

        Button(
            onClick = onCancel,
            enabled = cloneState !is InitState.ReadingRepo
        ) {
            Text(
                if (cloneState is InitState.Error) {
                    stringResource(R.string.go_back)

                } else stringResource(R.string.cancel)
            )
        }
    }
}

/**
 * What a failed clone looks like.
 *
 * A libgit2 message is a sentence about sockets and classes, and on its own in
 * the middle of an empty screen it reads as if the app had broken. It is still
 * the only thing that says what actually happened, so it stays — under a
 * heading that names the failure, and under the one sentence that says what to
 * do about it when the message is recognisable enough to tell.
 */
@Composable
private fun CloneError(detail: String?) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SimpleIcon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    // the heading right next to it says the same thing
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.clone_failed),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            hintFor(detail)?.let { hint ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.clone_failed_detail),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                // selectable, because the one thing to do with a message nobody
                // can act on is to hand it to somebody who can
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .heightIn(max = MaxDetailHeight)
                            .verticalScroll(rememberScrollState()),
                        text = detail,
                        style = MaterialTheme.typography.bodySmall
                            .copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}

/**
 * The one sentence that says what to do, for the two failures that are worth
 * telling apart. Anything else is left to the message itself rather than
 * guessed at.
 */
@Composable
private fun hintFor(detail: String?): String? {
    val message = detail?.lowercase() ?: return null

    return when {
        "auth" in message || "credential" in message || "permission denied" in message ->
            stringResource(R.string.clone_failed_auth_hint)

        "resolve" in message || "network" in message || "connect" in message
                || "timed out" in message || "class=net" in message ->
            stringResource(R.string.clone_failed_network_hint)

        else -> null
    }
}

@Preview
@Composable
private fun CloningScreenPreview() {

    CloningScreen(
        cloneState = InitState.Error(
            "can't clone repository: clone: Failed to authenticate SSH session: " +
                    "Unable to send userauth-publickey request; class=Ssh (23)"
        ),
        onCancel = {},
        onShowLogs = {}
    )
}
