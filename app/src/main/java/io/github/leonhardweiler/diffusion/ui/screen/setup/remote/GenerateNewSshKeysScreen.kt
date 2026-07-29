package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.helper.SshKeyValidation
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import io.github.leonhardweiler.diffusion.ui.viewmodel.InitState
import io.github.leonhardweiler.diffusion.ui.viewmodel.SetupViewModelI
import io.github.leonhardweiler.diffusion.ui.viewmodel.SetupViewModelMock

private const val TAG = "GenerateNewSshKeysScreen"

@Composable
fun GenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    cloneState: InitState,
    storageConfig: StorageConfiguration,
    url: String,
    vm: SetupViewModelI,
    generateSshKeys: () -> Pair<String, String>,
    onClone: () -> Unit,
    onSuccess: () -> Unit,
    /**
     * The pair the app already holds, when the user chose to reuse it. Nothing
     * is generated then, and the clone is not made to wait for a copy: a key
     * that has been here before is one the repository has most likely been told
     * about already.
     */
    storedKey: Cred.Ssh? = null,
) {

    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = !cloneState.isLoading()
    ) {

        val publicKey = rememberSaveable { mutableStateOf(storedKey?.publicKey.orEmpty()) }
        val privateKey = rememberSaveable { mutableStateOf(storedKey?.privateKey.orEmpty()) }

        // Only ever the stored one's: a pair generated here has none, and
        // regenerating drops it along with the key it belonged to.
        val passphrase = rememberSaveable { mutableStateOf(storedKey?.passphrase) }

        // A clone with a key the far end has never seen fails with an
        // authentication error that says nothing about the one step that was
        // skipped, so the clone waits until the key has at least been taken
        // away from here. A stored key has been through that once already.
        val keyCopied = rememberSaveable { mutableStateOf(storedKey != null) }

        if (storedKey == null) {
            LaunchedEffect(true) {
                val (public, private) = generateSshKeys()
                Log.d(TAG, public)
                publicKey.value = public
                privateKey.value = private
            }
        }

        SetupPage(
            title = stringResource(
                if (storedKey == null) {
                    R.string.ssh_keys_setup_title
                } else {
                    R.string.ssh_keys_stored_title
                }
            )
        ) {

            SetupLine(
                text = "1. " + stringResource(R.string.copy_the_key)
            ) {
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
                        text = publicKey.value,
                        maxLines = 1
                    )
                }


                val clipboardManager = LocalClipboard.current

                SetupButton(
                    text = if (keyCopied.value) {
                        stringResource(R.string.key_copied)
                    } else {
                        stringResource(R.string.copy_key)
                    },
                    onClick = {
                        val data = ClipData(
                            ClipDescription(
                                "public ssh key",
                                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            ),
                            ClipData.Item(publicKey.value)
                        )

                        vm.launch {
                            clipboardManager.setClipEntry(ClipEntry(data))
                            keyCopied.value = true
                        }
                    }
                )

                SetupButton(
                    text = stringResource(R.string.regenerate_key),
                    onClick = {
                        val (public, private) = generateSshKeys()
                        publicKey.value = public
                        privateKey.value = private
                        passphrase.value = null
                        // the key that was copied is not this one anymore
                        keyCopied.value = false
                    }
                )
            }



            SetupLine(
                text = "2. " + stringResource(R.string.paste_deploy_key_no_provider)
            ) {
            }


            SetupLine(
                text = "3. " + stringResource(R.string.try_cloning)
            ) {

                // Faded rather than taken out of the layout. Copying the key is
                // the one thing that makes this sentence go away, and a button
                // that jumps up under the finger that just pressed something
                // else is the finger's next tap landing somewhere it did not
                // mean to.
                Text(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .alpha(if (keyCopied.value) 0f else 1f),
                    text = stringResource(R.string.copy_key_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SetupButton(
                    text = stringResource(R.string.clone_repo),
                    // the keys are generated off the composition, so for a moment
                    // there is nothing here to authenticate with — and until the
                    // key has been copied it is nowhere the remote could know it
                    enabled = keyCopied.value
                            && SshKeyValidation.isKeyPair(publicKey.value, privateKey.value),
                    onClick = {
                        vm.cloneRepo(
                            storageConfig = storageConfig,
                            remoteUrl = url,
                            cred = Cred.Ssh(
                                publicKey = publicKey.value,
                                privateKey = privateKey.value,
                                passphrase = passphrase.value
                            ),
                            onSuccess = onSuccess
                        )

                        onClone()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun GenerateNewSshKeysScreenPreview() {
    GenerateNewSshKeysScreen(
        onBackClick = {},
        cloneState = InitState.Idle,
        storageConfig = StorageConfiguration("/storage/emulated/0/notes"),
        url = "url",
        vm = SetupViewModelMock(),
        generateSshKeys = { "aaaaaaaaaaaabbbbbbbbbbbbb" to "aaaaaaaaaaaabbbbbbbbbbbbb" },
        onSuccess = {},
        onClone = {}
    )

}
