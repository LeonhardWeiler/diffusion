package io.github.wiiznokes.gitnote.ui.viewmodel.edit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * What the buttons above the keyboard do. Two shapes cover all of them: a
 * pattern wrapped around the selection, and a prefix applied to every line the
 * selection touches. Each action is a toggle — pressed again, it takes its own
 * mark away.
 */

private const val TITLE = "### "

fun onTitle(v: TextFieldValue): TextFieldValue {
    // without a selection the marker belongs in front of the line; with one it
    // belongs directly in front of what is selected
    val insertAt =
        if (v.selection.collapsed) v.text.lineStartAt(v.selection.min - 1) else v.selection.min
    val presentAt = if (v.selection.collapsed) insertAt else insertAt - TITLE.length

    return if (v.text.startsWith(TITLE, startIndex = presentAt)) {
        v.copy(
            text = v.text.removeRange(presentAt, presentAt + TITLE.length),
            selection = v.selection.shiftedBy(-TITLE.length)
        )
    } else {
        v.copy(
            text = v.text.replaceRange(insertAt, insertAt, TITLE),
            selection = v.selection.shiftedBy(TITLE.length)
        )
    }
}

fun addOrRemovePatternAtTheExtremitiesOfSelection(
    v: TextFieldValue,
    startPattern: String,
    endPattern: String
): TextFieldValue {
    val min = v.selection.min
    val max = v.selection.max

    return if (v.surroundedBy(startPattern, endPattern)) {
        v.copy(
            text = v.text.removeRange(max, max + endPattern.length)
                .removeRange(min - startPattern.length, min),
            selection = v.selection.shiftedBy(-startPattern.length)
        )
    } else {
        v.copy(
            text = v.text.surroundSelection(min, max, startPattern, endPattern),
            selection = v.selection.shiftedBy(startPattern.length)
        )
    }
}

fun addOrRemovePatternAtTheExtremitiesOfSelection(
    v: TextFieldValue,
    pattern: String
): TextFieldValue = addOrRemovePatternAtTheExtremitiesOfSelection(v, pattern, pattern)

fun onCode(v: TextFieldValue): TextFieldValue {
    val spansLines = v.text.substring(v.selection.min, v.selection.max).contains('\n')
    return if (spansLines) {
        addOrRemovePatternAtTheExtremitiesOfSelection(v, "```\n", "\n```")
    } else {
        addOrRemovePatternAtTheExtremitiesOfSelection(v, "`")
    }
}

private const val LINK_START = "["
private const val LINK_END = "](url)"

fun onLink(v: TextFieldValue): TextFieldValue {
    val min = v.selection.min
    val max = v.selection.max

    if (v.surroundedBy(LINK_START, LINK_END)) {
        return v.copy(
            text = v.text.removeRange(max, max + LINK_END.length)
                .removeRange(min - LINK_START.length, min),
            selection = v.selection.shiftedBy(-LINK_START.length)
        )
    }

    return v.copy(
        text = v.text.surroundSelection(min, max, LINK_START, LINK_END),
        // over selected text the caret lands on "url", ready to be typed over
        selection = if (v.selection.collapsed) v.selection.shiftedBy(LINK_START.length)
        else TextRange(start = max + 3, end = max + 6)
    )
}

private fun TextFieldValue.surroundedBy(startPattern: String, endPattern: String): Boolean =
    text.startsWith(startPattern, startIndex = selection.min - startPattern.length) &&
            text.startsWith(endPattern, startIndex = selection.max)

/** The end goes in first, so the index of the start is still the right one. */
private fun String.surroundSelection(min: Int, max: Int, start: String, end: String): String =
    replaceRange(max, max, end).replaceRange(min, min, start)

fun onQuote(v: TextFieldValue): TextFieldValue {
    val pattern = "> "
    var add = false

    return eachSelectedLine(
        v = v,
        decide = { line ->
            if (!line.startsWith(pattern)) add = true
            add
        },
        transform = { line, _ ->
            if (add) line.takeIf { it.startsWith(pattern) } ?: (pattern + line)
            else line.substring(pattern.length)
        }
    )
}

fun onUnorderedList(v: TextFieldValue) = toggleListStyle(
    v = v,
    isStyled = { it.listType == ListType.Dash },
    style = { copy(listType = ListType.Dash) }
)

fun onNumberedList(v: TextFieldValue) = toggleListStyle(
    v = v,
    isStyled = { it.listType is ListType.Number },
    style = { lineNumber -> copy(listType = ListType.Number(lineNumber)) }
)

fun onTaskList(v: TextFieldValue) = toggleListStyle(
    v = v,
    isStyled = { it.isTaskList },
    style = { copy(isTaskList = true) }
)

/**
 * Turns every selected line into a list of one style, or — when they all carry
 * that style already — strips the list markers off again. Lines that are not
 * list items borrow their shape from the first one that is, so converting a
 * block keeps its indentation.
 */
private fun toggleListStyle(
    v: TextFieldValue,
    isStyled: (ListItemInfo) -> Boolean,
    style: ListItemInfo.(lineNumber: Int) -> ListItemInfo,
): TextFieldValue {

    var convert = false
    var template: ListItemInfo? = null

    return eachSelectedLine(
        v = v,
        decide = { line ->
            val item = ListItemInfo.parseSafely(line)
            if (item == null) {
                convert = true
            } else {
                if (template == null) template = item
                if (!isStyled(item)) convert = true
            }
            convert && template != null
        },
        transform = { line, lineNumber ->
            val item = ListItemInfo.parseSafely(line)
            when {
                item == null -> (template ?: ListItemInfo().also { template = it })
                    .style(lineNumber)
                    .copy(isChecked = false, title = line, padding = getPadding(line) ?: "")
                    .line(numberOp = { lineNumber }, minusPaddingInTitle = true)

                convert -> item.style(lineNumber).line(numberOp = { lineNumber })
                else -> item.lineWithoutPrefix()
            }
        }
    )
}

/**
 * Rewrites every line the selection touches. [decide] sees those lines first and
 * may stop early by returning true — that is how an action makes up its mind
 * about adding or removing before it changes anything. [transform] then gets
 * each line together with its position in the block, counted from one.
 */
private fun eachSelectedLine(
    v: TextFieldValue,
    decide: (String) -> Boolean,
    transform: (String, Int) -> String
): TextFieldValue {
    // a caret resting just past a line still belongs to it, not to the next one
    val from =
        if (v.text.getOrNull(v.selection.min) == '\n') v.selection.min - 1 else v.selection.min

    val start = v.text.lineStartAt(from)
    val end = v.text.lineEndAt(v.selection.max)

    val lines = v.text.substring(start, end).lines()
    for (line in lines) if (decide(line)) break

    val rewritten = lines.mapIndexed { index, line -> transform(line, index + 1) }

    val firstLineGrewBy = rewritten.first().length - lines.first().length
    val allLinesGrewBy = rewritten.sumOf { it.length } - lines.sumOf { it.length }

    // the end of the selection moves by everything that was inserted before it,
    // the start only by what happened on its own line — and neither may slide
    // out in front of the block
    return v.copy(
        text = v.text.replaceRange(start, end, rewritten.joinToString("\n")),
        selection = if (v.selection.reversed) TextRange(
            start = maxOf(v.selection.start + allLinesGrewBy, start),
            end = maxOf(v.selection.end + firstLineGrewBy, start),
        ) else TextRange(
            start = maxOf(v.selection.start + firstLineGrewBy, start),
            end = maxOf(v.selection.end + allLinesGrewBy, start),
        )
    )
}
