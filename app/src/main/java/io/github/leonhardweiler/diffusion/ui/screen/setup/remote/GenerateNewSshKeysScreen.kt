package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.helper.SshKeyValidation
import io.github.leonhardweiler.diffusion.helper.openUrlInBrowser
import io.github.leonhardweiler.diffusion.helper.repoWebUrl
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.viewmodel.InitState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "GenerateNewSshKeysScreen"

private const val KEY_COPIED_LABEL_MS = 2_000L

@Composable
fun GenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    cloneState: InitState,
    remoteUrl: String,
    alreadyOnDevice: Boolean,
    generateSshKeys: () -> Pair<String, String>,
    cloneWith: (Cred) -> Unit,
    storedKey: Cred.Ssh? = null,
) {
    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = !cloneState.isLoading()
    ) {
        val scope = rememberCoroutineScope()

        val publicKey = rememberSaveable { mutableStateOf(storedKey?.publicKey.orEmpty()) }
        val privateKey = rememberSaveable { mutableStateOf(storedKey?.privateKey.orEmpty()) }

        val passphrase = rememberSaveable { mutableStateOf(storedKey?.passphrase) }

        val keyCopied = rememberSaveable { mutableStateOf(storedKey != null) }

        val justCopied = remember { mutableStateOf(false) }

        val copyCount = remember { mutableIntStateOf(0) }

        LaunchedEffect(copyCount.intValue) {
            if (copyCount.intValue == 0) return@LaunchedEffect
            justCopied.value = true
            delay(KEY_COPIED_LABEL_MS)
            justCopied.value = false
        }

        if (storedKey == null) {
            LaunchedEffect(true) {
                val (public, private) = generateSshKeys()
                Log.d(TAG, public)
                publicKey.value = public
                privateKey.value = private
            }
        }

        val clipboard = LocalClipboard.current

        fun copyKey() {
            val data = ClipData(
                ClipDescription("public ssh key", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)),
                ClipData.Item(publicKey.value)
            )

            scope.launch {
                clipboard.setClipEntry(ClipEntry(data))
                keyCopied.value = true
                copyCount.intValue++
            }
        }

        fun startClone() {
            cloneWith(
                Cred.Ssh(
                    publicKey = publicKey.value,
                    privateKey = privateKey.value,
                    passphrase = passphrase.value
                )
            )
        }

        // waits for the copy: a key generated here and never taken away is one
        // the remote has never seen, and the authentication error that follows
        // says nothing about the step that was skipped
        val canStart = keyCopied.value &&
                SshKeyValidation.isKeyPair(publicKey.value, privateKey.value)

        SetupPage(
            title = stringResource(
                if (storedKey == null) {
                    R.string.ssh_keys_setup_title
                } else {
                    R.string.ssh_keys_stored_title
                }
            )
        ) {
            if (storedKey != null) {
                SetupLine(text = "1. " + stringResource(R.string.add_key_if_missing)) {
                    KeyBox(publicKey = publicKey.value)
                    CopyKeyButton(justCopied = justCopied, onCopy = ::copyKey)
                    OpenRepositoryButton(remoteUrl = remoteUrl)
                }

                SetupLine(
                    text = "2. " + stringResource(
                        if (alreadyOnDevice) R.string.try_syncing else R.string.try_cloning
                    )
                ) {
                    StartButton(
                        alreadyOnDevice = alreadyOnDevice,
                        enabled = canStart,
                        onClick = ::startClone,
                    )
                }

                return@SetupPage
            }

            SetupLine(text = "1. " + stringResource(R.string.copy_the_key)) {
                KeyBox(publicKey = publicKey.value)
                CopyKeyButton(justCopied = justCopied, onCopy = ::copyKey)

                SetupButton(
                    text = stringResource(R.string.regenerate_key),
                    onClick = {
                        val (public, private) = generateSshKeys()
                        publicKey.value = public
                        privateKey.value = private
                        passphrase.value = null
                        keyCopied.value = false
                        justCopied.value = false
                        copyCount.intValue = 0
                    }
                )
            }

            SetupLine(text = "2. " + stringResource(R.string.paste_deploy_key_no_provider)) {
                OpenRepositoryButton(remoteUrl = remoteUrl)
            }

            SetupLine(
                text = "3. " + stringResource(
                    if (alreadyOnDevice) R.string.try_syncing else R.string.try_cloning
                )
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(R.string.copy_key_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                StartButton(
                    alreadyOnDevice = alreadyOnDevice,
                    enabled = canStart,
                    onClick = ::startClone,
                )
            }
        }
    }
}

@Composable
private fun KeyBox(publicKey: String) {
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
private fun CopyKeyButton(justCopied: MutableState<Boolean>, onCopy: () -> Unit) {
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
private fun ColumnScope.OpenRepositoryButton(remoteUrl: String) {
    val webUrl = remember(remoteUrl) { repoWebUrl(remoteUrl) } ?: return

    val context = LocalContext.current

    SetupButton(
        text = stringResource(R.string.open_git_repository),
        link = true,
        onClick = { openUrlInBrowser(context, webUrl) }
    )
}

@Composable
private fun StartButton(alreadyOnDevice: Boolean, enabled: Boolean, onClick: () -> Unit) {
    SetupButton(
        text = stringResource(
            if (alreadyOnDevice) R.string.sync_repo else R.string.clone_repo
        ),
        enabled = enabled,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun GenerateNewSshKeysScreenPreview() {
    GenerateNewSshKeysScreen(
        onBackClick = {},
        cloneState = InitState.Idle,
        remoteUrl = "git@github.com:LeonhardWeiler/diffusion.git",
        alreadyOnDevice = false,
        generateSshKeys = { "aaaaaaaaaaaabbbbbbbbbbbbb" to "aaaaaaaaaaaabbbbbbbbbbbbb" },
        cloneWith = {},
    )
}

@Preview
@Composable
private fun StoredSshKeyScreenPreview() {
    GenerateNewSshKeysScreen(
        onBackClick = {},
        cloneState = InitState.Idle,
        remoteUrl = "git@github.com:LeonhardWeiler/diffusion.git",
        alreadyOnDevice = true,
        generateSshKeys = { "" to "" },
        cloneWith = {},
        storedKey = Cred.Ssh(
            publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIexample",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----",
            passphrase = null,
        ),
    )
}
