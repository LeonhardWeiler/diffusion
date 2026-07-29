package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.AppPage
import io.github.leonhardweiler.diffusion.ui.component.SetupButton
import io.github.leonhardweiler.diffusion.ui.component.SetupLine
import io.github.leonhardweiler.diffusion.ui.component.SetupPage


@Composable
fun SelectGenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
    onCustom: () -> Unit,
    /** Whether there is a pair in the store to be offered at all. */
    hasStoredKey: Boolean = false,
    onUseStored: () -> Unit = {},
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

            SetupLine(text = "") {

                // First, and only when there is one. A key already on the device
                // is one the repository probably already trusts, and taking it
                // costs no further deploy key — which is what both of the other
                // ways out of this screen do.
                //
                // Its fingerprint stood under the button and said nothing: the
                // key itself, and what to do with it, is on the screen the
                // button leads to.
                if (hasStoredKey) {
                    SetupButton(
                        onClick = onUseStored,
                        text = stringResource(R.string.use_stored_key)
                    )
                }

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
        hasStoredKey = true,
        onUseStored = {},
    )
}
