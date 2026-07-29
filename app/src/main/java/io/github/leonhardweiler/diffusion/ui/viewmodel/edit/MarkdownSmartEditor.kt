package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * What typing does on its own, without a button being pressed: enter inside a
 * list carries the list to the next line — or drops the item, if it was left
 * empty — and the delete key clears a line that holds nothing but indentation.
 *
 * [prev] is the value from before the keystroke. Comparing the two lengths is
 * how a typed character is told apart from a deleted one, which matters because
 * both can leave the caret in the same place.
 */
fun markdownSmartEditor(prev: TextFieldValue, v: TextFieldValue): TextFieldValue {
    if (!v.selection.collapsed) return v

    val caret = v.selection.start
    if (caret <= 0 || caret > v.text.length) return v

    return if (v.text[caret - 1] == '\n') continueList(prev, v, caret)
    else unindentBlankLine(prev, v, caret)
}

private fun continueList(prev: TextFieldValue, v: TextFieldValue, caret: Int): TextFieldValue {
    // the caret can also come to rest behind a newline by deleting; only text
    // that just grew was a typed enter
    if (prev.text.length >= v.text.length) return v

    val lineBefore = v.text.substring(v.text.lineStartAt(caret - 2), caret - 1)
    val restOfLine = v.text.substring(caret, v.text.lineEndAt(caret))

    val item = ListItemInfo.parseSafely(lineBefore)

    // enter on an item nobody wrote anything into ends the list instead of
    // adding one more empty item to it
    if (restOfLine.isBlank() && item?.shouldRemove() == true) {
        val lineStart = caret - (lineBefore.length + 1)
        return v.copy(
            text = v.text.removeRange(lineStart, caret),
            selection = TextRange(lineStart)
        )
    }

    val continuation = when {
        item != null -> item.padding + item.prefix(numberOp = { it + 1 })
        // not a list, but the indentation is still worth keeping
        else -> getPadding(lineBefore) ?: return v
    }

    return v.copy(
        text = v.text.replaceRange(caret, caret, continuation),
        selection = TextRange(caret + continuation.length)
    )
}

private fun unindentBlankLine(prev: TextFieldValue, v: TextFieldValue, caret: Int): TextFieldValue {
    // only for a single deleted character, and only if it was a space or a tab
    if (prev.text.length != v.text.length + 1) return v
    if (prev.text[caret] != ' ' && prev.text[caret] != '\t') return v

    val lineStart = v.text.lineStartAt(caret - 1)
    if (v.text.substring(lineStart, caret).isNotBlank()) return v

    return v.copy(
        text = v.text.removeRange(lineStart, caret),
        selection = TextRange(lineStart)
    )
}
