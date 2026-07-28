package io.github.leonhardweiler.gitnote.ui.screen.setup.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.gitnote.R
import io.github.leonhardweiler.gitnote.ui.component.AppPage
import io.github.leonhardweiler.gitnote.helper.SshKeyValidation
import io.github.leonhardweiler.gitnote.ui.component.SetupButton
import io.github.leonhardweiler.gitnote.ui.component.SetupLine
import io.github.leonhardweiler.gitnote.ui.component.SetupPage
import io.github.leonhardweiler.gitnote.ui.model.Cred
import io.github.leonhardweiler.gitnote.ui.model.StorageConfiguration
import io.github.leonhardweiler.gitnote.ui.viewmodel.InitState
import io.github.leonhardweiler.gitnote.ui.viewmodel.SetupViewModelI
import io.github.leonhardweiler.gitnote.ui.viewmodel.SetupViewModelMock

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
) {

    AppPage(
        title = stringResource(R.string.ssh_keys_title),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = !cloneState.isLoading()
    ) {

        val publicKey = rememberSaveable { mutableStateOf("") }
        val privateKey = rememberSaveable { mutableStateOf("") }

        LaunchedEffect(true) {
            val (public, private) = generateSshKeys()
            Log.d(TAG, public)
            publicKey.value = public
            privateKey.value = private
        }

        SetupPage(
            title = stringResource(R.string.ssh_keys_setup_title)
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
                    text = stringResource(R.string.copy_key),
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
                        }
                    }
                )

                SetupButton(
                    text = stringResource(R.string.regenerate_key),
                    onClick = {
                        val (public, private) = generateSshKeys()
                        publicKey.value = public
                        privateKey.value = private
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

                SetupButton(
                    text = stringResource(R.string.clone_repo),
                    // the keys are generated off the composition, so for a moment
                    // there is nothing here to authenticate with
                    enabled = SshKeyValidation.isKeyPair(publicKey.value, privateKey.value),
                    onClick = {
                        vm.cloneRepo(
                            storageConfig = storageConfig,
                            remoteUrl = url,
                            cred = Cred.Ssh(
                                publicKey = publicKey.value,
                                privateKey = privateKey.value,
                                passphrase = null
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
