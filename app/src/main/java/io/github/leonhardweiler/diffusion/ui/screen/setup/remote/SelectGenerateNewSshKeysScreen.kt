package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.repo.StoredSshKey
import io.github.leonhardweiler.diffusion.helper.sshKeyLabel
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage


@Composable
fun SelectGenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
    onCustom: () -> Unit,
    /** Every pair in the store, since any of them may be the right one. */
    storedKeys: List<StoredSshKey> = emptyList(),
    onUseStored: (StoredSshKey) -> Unit = {},
) {

    AppPage(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage(
            title = stringResource(R.string.we_need_ssh_keys_to_authenticate),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // First, and one button each. A key already on the device is one
            // some repository already got a deploy key for, and taking it again
            // costs the remote nothing — which is what both of the other ways
            // out of this screen do.
            //
            // Named by its fingerprint, in the form every host shows beside a
            // deploy key: with several keys there is a right one, and a row of
            // identical buttons is no way to find it. There was one button here
            // saying "Use stored key", back when the app held one key because it
            // held one repository.
            if (storedKeys.isNotEmpty()) {
                SetupLine(
                    text = stringResource(R.string.keys_on_this_device),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    storedKeys.forEach { key ->
                        SetupButton(
                            onClick = { onUseStored(key) },
                            text = sshKeyLabel(key.publicKey)
                        )
                    }
                }
            }

            SetupLine(text = "") {

                SetupButton(
                    onClick = onGenerate,
                    text = stringResource(R.string.generate_new_keys)
                )

                SetupButton(
                    onClick = onCustom,
                    text = stringResource(R.string.custom_ssh_keys)
                )
            }
        }
    }
}


@Preview
@Composable
private fun SelectGenerateNewSshKeysScreenPreview() {
    SelectGenerateNewSshKeysScreen(
        onBackClick = {},
        onGenerate = {},
        onCustom = {},
        storedKeys = listOf(
            StoredSshKey(
                id = "a",
                publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIexample",
                privateKey = "",
                passphrase = null,
            )
        ),
        onUseStored = {},
    )
}
