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

/**
 * How long the copy button says that it copied something. Long enough to be
 * read, short enough that the button is a button again by the time the key has
 * been pasted somewhere.
 */
private const val KEY_COPIED_LABEL_MS = 2_000L

@Composable
fun GenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    cloneState: InitState,
    /** The clone url this setup is about, so that its page can be offered. */
    remoteUrl: String,
    /**
     * Whether the repository is already on the device. Then there is nothing to
     * download and the last step is a sync — which is also what finds out
     * whether the remote takes this key, and takes it for writing.
     */
    alreadyOnDevice: Boolean,
    generateSshKeys: () -> Pair<String, String>,
    /** Starts the clone with these credentials and goes to the clone screen. */
    cloneWith: (Cred) -> Unit,
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

        val scope = rememberCoroutineScope()

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

        // What the button says is not what the clone goes by: the key is copied
        // again and again while a deploy key is being set up, and a button that
        // reads "Key copied" for good reads as something that has happened
        // rather than something to press. It says so for a moment and is a copy
        // button again after that, while the clone stays unlocked.
        val justCopied = remember { mutableStateOf(false) }

        // Every press starts the two seconds over rather than adding a second
        // timer next to the one already running, which would end the label
        // early.
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
                    text = if (justCopied.value) {
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

                        scope.launch {
                            clipboardManager.setClipEntry(ClipEntry(data))
                            keyCopied.value = true
                            copyCount.intValue++
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
                        // the key that was copied is not this one anymore, and
                        // a label still saying so would be about the old pair
                        keyCopied.value = false
                        justCopied.value = false
                        copyCount.intValue = 0
                    }
                )
            }



            SetupLine(
                text = "2. " + stringResource(R.string.paste_deploy_key_no_provider)
            ) {
                // The key belongs in the settings of one particular repository,
                // and its address is the one thing this setup already knows.
                // Nothing is shown for an address with no page behind it.
                val webUrl = remember(remoteUrl) { repoWebUrl(remoteUrl) }

                if (webUrl != null) {
                    val context = LocalContext.current

                    SetupButton(
                        text = stringResource(R.string.open_git_repository),
                        link = true,
                        onClick = { openUrlInBrowser(context, webUrl) }
                    )
                }
            }


            SetupLine(
                text = "3. " + stringResource(
                    if (alreadyOnDevice) R.string.try_syncing else R.string.try_cloning
                )
            ) {

                // Always here, whether the key has been copied or not: copying
                // it is one of the two steps this sentence is about, and the
                // other one happens on a different device entirely. A clone that
                // fails after the key was copied but never pasted is exactly the
                // case that needs the sentence still standing.
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(R.string.copy_key_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SetupButton(
                    text = stringResource(
                        if (alreadyOnDevice) R.string.sync_repo else R.string.clone_repo
                    ),
                    // the keys are generated off the composition, so for a moment
                    // there is nothing here to authenticate with — and until the
                    // key has been copied it is nowhere the remote could know it
                    enabled = keyCopied.value
                            && SshKeyValidation.isKeyPair(publicKey.value, privateKey.value),
                    onClick = {
                        cloneWith(
                            Cred.Ssh(
                                publicKey = publicKey.value,
                                privateKey = privateKey.value,
                                passphrase = passphrase.value
                            )
                        )
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
        remoteUrl = "git@github.com:LeonhardWeiler/diffusion.git",
        alreadyOnDevice = false,
        generateSshKeys = { "aaaaaaaaaaaabbbbbbbbbbbbb" to "aaaaaaaaaaaabbbbbbbbbbbbb" },
        cloneWith = {},
    )

}
