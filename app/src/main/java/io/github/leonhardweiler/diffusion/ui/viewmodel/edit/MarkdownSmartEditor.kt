package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun markdownSmartEditor(prev: TextFieldValue, v: TextFieldValue): TextFieldValue {
    if (!v.selection.collapsed) return v

    val caret = v.selection.start
    if (caret <= 0 || caret > v.text.length) return v

    return if (v.text[caret - 1] == '\n') continueList(prev, v, caret)
    else unindentBlankLine(prev, v, caret)
}

private fun continueList(prev: TextFieldValue, v: TextFieldValue, caret: Int): TextFieldValue {
    if (prev.text.length >= v.text.length) return v

    val lineBefore = v.text.substring(v.text.lineStartAt(caret - 2), caret - 1)
    val restOfLine = v.text.substring(caret, v.text.lineEndAt(caret))

    val item = ListItemInfo.parseSafely(lineBefore)

    if (restOfLine.isBlank() && item?.shouldRemove() == true) {
        val lineStart = caret - (lineBefore.length + 1)
        return v.copy(
            text = v.text.removeRange(lineStart, caret),
            selection = TextRange(lineStart)
        )
    }

    val continuation = when {
        item != null -> item.padding + item.prefix(numberOp = { it + 1 })
        else -> getPadding(lineBefore) ?: return v
    }

    return v.copy(
        text = v.text.replaceRange(caret, caret, continuation),
        selection = TextRange(caret + continuation.length)
    )
}

private fun unindentBlankLine(prev: TextFieldValue, v: TextFieldValue, caret: Int): TextFieldValue {
    if (prev.text.length != v.text.length + 1) return v
    if (prev.text[caret] != ' ' && prev.text[caret] != '\t') return v

    val lineStart = v.text.lineStartAt(caret - 1)
    if (v.text.substring(lineStart, caret).isNotBlank()) return v

    return v.copy(
        text = v.text.removeRange(lineStart, caret),
        selection = TextRange(lineStart)
    )
}
