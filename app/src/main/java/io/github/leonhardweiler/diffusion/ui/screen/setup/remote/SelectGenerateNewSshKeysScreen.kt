package io.github.leonhardweiler.diffusion.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    storedKeyFingerprint: String? = null,
    onUseStored: () -> Unit = {},
) {

    AppPage(
        title = stringResource(R.string.ssh_keys_title),
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
                if (storedKeyFingerprint != null) {
                    SetupButton(
                        onClick = onUseStored,
                        text = stringResource(R.string.use_stored_key)
                    )

                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = storedKeyFingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
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
        storedKeyFingerprint = "SHA256:kPBQ1w0kSjTNciZ2mR7pW9xV4kL0aB3cD5eFgHiJkLm",
        onUseStored = {},
    )
}
