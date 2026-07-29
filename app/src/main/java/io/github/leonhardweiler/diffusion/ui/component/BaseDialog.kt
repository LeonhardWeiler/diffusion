package io.github.leonhardweiler.diffusion.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.leonhardweiler.diffusion.ui.utils.conditional

@Composable
fun BaseDialog(
    expanded: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    verticalScrollEnabled: Boolean = true,
    /** Tapping outside the dialog or going back, which is not the same as saying no. */
    onDismiss: () -> Unit = {},
    dialogContent: @Composable ColumnScope.(MutableState<Boolean>) -> Unit
) {
    if (expanded.value) {
        Dialog(
            onDismissRequest = {
                expanded.value = false
                onDismiss()
            }
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .conditional(verticalScrollEnabled) {
                        verticalScroll(rememberScrollState())
                    },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 25.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    dialogContent(expanded)
                }
            }
        }
    }

}