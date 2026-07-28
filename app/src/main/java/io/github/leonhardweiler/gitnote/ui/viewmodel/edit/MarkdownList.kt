package io.github.leonhardweiler.gitnote.ui.viewmodel.edit

import android.util.Log

private const val TAG = "MarkdownList"

/** Everything before the first non-blank character, or null if there is none. */
fun getPadding(line: String): String? = Regex("^\\s+").find(line)?.value

sealed class ListType {
    object Dash : ListType()
    object Asterisk : ListType()
    data class Number(val number: Int) : ListType()

    fun prefix(numberOp: (Int) -> Int = { it }): String = when (this) {
        Asterisk -> "* "
        Dash -> "- "
        is Number -> "${numberOp(number)}. "
    }
}

/**
 * One line of a markdown list, taken apart: what marks it as an item, whether it
 * carries a checkbox, how far it is indented and what it says. The editor works
 * on this rather than on the line, so continuing, renumbering and converting a
 * list are all the same operation with a different piece swapped out.
 */
data class ListItemInfo(
    val listType: ListType = ListType.Dash,
    val isTaskList: Boolean = false,
    val isChecked: Boolean = false,
    val padding: String = "",
    val title: String? = null,
) {

    companion object {

        private val LINE = Regex("""^(\s*)(?:(-)|(\*)|(\d+)\.)\s(?:\[([ xX])]\s)?(.+)?""")

        /** Null when the line is not a list item at all. */
        fun parse(line: String): ListItemInfo? {
            val match = LINE.matchEntire(line) ?: return null

            val padding = match.groups[1]?.value ?: throw Exception("padding null: $line")

            val listType = when {
                match.groups[2] != null -> ListType.Dash
                match.groups[3] != null -> ListType.Asterisk
                match.groups[4] != null -> ListType.Number(match.groups[4]!!.value.toInt())
                else -> throw Exception("listType is null but we have a match: $line")
            }

            return ListItemInfo(
                listType = listType,
                isTaskList = match.groups[5] != null,
                isChecked = match.groups[5]?.value != " ",
                padding = padding,
                title = match.groups[6]?.value
            )
        }

        /** For the editing paths, where a malformed line is not worth a crash. */
        fun parseSafely(line: String): ListItemInfo? = try {
            parse(line)
        } catch (e: Exception) {
            Log.d(TAG, "$e")
            null
        }
    }

    fun prefix(numberOp: (Int) -> Int = { it }): String {
        val marker = listType.prefix(numberOp)
        if (!isTaskList) return marker
        return marker + if (isChecked) "[x] " else "[ ] "
    }

    fun line(numberOp: (Int) -> Int = { it }, minusPaddingInTitle: Boolean = false): String {
        val body = when {
            title == null -> ""
            minusPaddingInTitle -> title.removePrefix(padding)
            else -> title
        }
        return padding + prefix(numberOp) + body
    }

    /** The line stripped of what makes it a list item. */
    fun lineWithoutPrefix(): String = padding + title

    /** An item nobody filled in, which is what enter on it means to get rid of. */
    fun shouldRemove(): Boolean = title?.isNotBlank() != true
}
