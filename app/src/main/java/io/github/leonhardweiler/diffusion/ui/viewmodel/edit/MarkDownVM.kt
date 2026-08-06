package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.ui.viewmodel.viewModelFactory

private const val TAG = "MarkDownVM"


class MarkDownVM(previousNote: Note) : TextVM(previousNote) {

    override fun onValueChange(v: TextFieldValue) {
        val newValue = markdownSmartEditor(content.value, v)
        super.onValueChange(newValue)
    }

    /**
     * [nodeStart] and [nodeEnd] delimit the checkbox node of the parsed
     * markdown, which is the only reliable way to find the right one when the
     * same line appears twice.
     */
    fun toggleCheckBox(nodeStart: Int, nodeEnd: Int) {
        val text = content.value.text

        val open = text.indexOf('[', nodeStart)
        if (open == -1 || open >= nodeEnd) return

        val close = text.indexOf(']', open)
        if (close == -1 || close > nodeEnd) return

        val checked = text.substring(open + 1, close).trim().equals("x", ignoreCase = true)
        val toggled = text.substring(0, open + 1) + (if (checked) " " else "x") + text.substring(close)

        super.onValueChange(content.value.copy(text = toggled))
        save()
    }
}


@Composable
fun newMarkDownVM(note: Note): MarkDownVM =
    viewModel<MarkDownVM>(factory = viewModelFactory { MarkDownVM(note) })
