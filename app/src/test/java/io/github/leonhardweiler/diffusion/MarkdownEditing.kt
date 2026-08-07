package io.github.leonhardweiler.diffusion

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun edit(annotated: String): TextFieldValue {
    val caret = annotated.indexOf('|')
    if (caret != -1) {
        return TextFieldValue(
            text = annotated.removeRange(caret, caret + 1),
            selection = TextRange(caret)
        )
    }

    val start = annotated.indexOf('[')
    val end = annotated.indexOf(']')
    require(start != -1 && end > start) { "no caret and no selection in: $annotated" }

    return TextFieldValue(
        text = annotated.removeRange(end, end + 1).removeRange(start, start + 1),
        selection = TextRange(start, end - 1)
    )
}

fun show(v: TextFieldValue): String = with(v.selection) {
    if (collapsed) {
        StringBuilder(v.text).insert(start, '|').toString()
    } else {
        StringBuilder(v.text).insert(max, ']').insert(min, '[').toString()
    }
}
