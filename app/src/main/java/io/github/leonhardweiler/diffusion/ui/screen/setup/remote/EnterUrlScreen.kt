package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.helper.NetworkPermissionHelper
import kotlinx.coroutines.launch
import io.github.leonhardweiler.diffusion.helper.CloneUrlKind
import io.github.leonhardweiler.diffusion.helper.cloneUrlKind
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage


/**
 * Whether this app can clone from that address, which means: is it ssh.
 *
 * https is not offered anymore. It is the transport that wants a password or a
 * token in the app's own storage, in the clear, so that every sync can replay
 * it — an ssh key is at least a thing the remote can be told to stop trusting
 * without changing anything else about the account.
 */
fun isCloneUrlSupported(url: String): Boolean = cloneUrlKind(url) == CloneUrlKind.Ssh

/** An address that is a clone url, but one over http or https. */
private fun isUnsupportedTransport(url: String): Boolean =
    cloneUrlKind(url).let { it == CloneUrlKind.Http || it == CloneUrlKind.Https }

@Composable
fun EnterUrlScreen(
    onBackClick: () -> Unit,
    defaultUrl: String = "",
    onUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingUrl = remember { mutableStateOf<String?>(null) }
    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingUrl.value?.let(onUrl)
        } else {
            MyApp.appModule.uiHelper.makeToast(
                MyApp.appModule.context.getString(R.string.error_need_network_permission)
            )
        }
        pendingUrl.value = null
    }

    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage {
            val url = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(
                    TextFieldValue(defaultUrl, selection = TextRange(defaultUrl.length))
                )
            }

            SetupLine(
                text = "1. " + stringResource(R.string.url_explain_enter_url)
            ) {
                UrlTextField(url = url)
            }

            // A dead "Next" says nothing about why. An https address is the one
            // wrong answer somebody arrives with on purpose — it is what the
            // provider's page offers first — so it gets a sentence of its own.
            if (isUnsupportedTransport(url.value.text)) {
                SetupLine(text = stringResource(R.string.error_https_not_supported)) {}
            }

            SetupButton(
                text = stringResource(R.string.next),
                onClick = {
                    val urlText = url.value.text
                    scope.launch {
                        if (NetworkPermissionHelper.requiresLocalNetworkPermission(urlText) && !NetworkPermissionHelper.isPermissionGranted(context)) {
                            pendingUrl.value = urlText
                            nearbyPermissionLauncher.launch(NetworkPermissionHelper.PERMISSION)
                        } else {
                            onUrl(urlText)
                        }
                    }
                },
                enabled = isCloneUrlSupported(url.value.text)
            )
        }
    }
}

@Composable
private fun UrlTextField(url: MutableState<TextFieldValue>) {

    OutlinedTextField(
        modifier = Modifier
            .fillMaxSize(),
        value = url.value,
        onValueChange = {
            url.value = it
        },
        label = {
            Text(text = stringResource(R.string.clone_step_url_label))
        },
        placeholder = {
            Text(text = "git@github.com:LeonhardWeiler/diffusion")
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri
        )
    )
}


@Preview
@Composable
private fun EnterUrlScreenPreview() {

    EnterUrlScreen(
        onBackClick = {},
        onUrl = {}
    )
}
