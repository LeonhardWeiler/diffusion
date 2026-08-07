package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

import android.util.Log

private const val TAG = "MarkdownList"

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

data class ListItemInfo(
    val listType: ListType = ListType.Dash,
    val isTaskList: Boolean = false,
    val isChecked: Boolean = false,
    val padding: String = "",
    val title: String? = null,
) {
    companion object {
        private val LINE = Regex("""^(\s*)(?:(-)|(\*)|(\d+)\.)\s(?:\[([ xX])]\s)?(.+)?""")

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

    fun shouldRemove(): Boolean = title?.isNotBlank() != true
}
