package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.markdownSmartEditor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What typing does on its own: continuing a list on enter, dropping an item that
 * was left empty, and carrying indentation to the next line.
 */
class MarkdownSmartEditorTest {

    private fun check(before: String, after: String, expected: String) {
        assertEquals(expected, show(markdownSmartEditor(edit(before), edit(after))), after)
    }

    @Test
    fun enterInDash() = check("- a|", "- a\n|", "- a\n- |")

    @Test
    fun enterInStar() = check("* a|", "* a\n|", "* a\n* |")

    @Test
    fun enterInNumber() = check("1. a|", "1. a\n|", "1. a\n2. |")

    @Test
    fun enterInTask() = check("- [ ] a|", "- [ ] a\n|", "- [ ] a\n- [ ] |")

    @Test
    fun enterInCheckedTask() = check("- [x] a|", "- [x] a\n|", "- [x] a\n- [x] |")

    @Test
    fun enterOnEmptyItem() = check("- |", "- \n|", "|")

    @Test
    fun enterOnEmptyStar() = check("* |", "* \n|", "|")

    @Test
    fun enterOnEmptyNumber() = check("1. |", "1. \n|", "|")

    @Test
    fun enterKeepsPadding() = check("  abc|", "  abc\n|", "  abc\n  |")

    @Test
    fun enterTabPadding() = check("\tabc|", "\tabc\n|", "\tabc\n\t|")

    @Test
    fun enterPlain() = check("abc|", "abc\n|", "abc\n|")

    @Test
    fun enterNestedList() = check("  - a|", "  - a\n|", "  - a\n  - |")

    @Test
    fun enterBeforeText() = check("- a|b", "- a\n|b", "- a\n- |b")

    @Test
    fun deleteKeyShrinks() = check("- a\n|", "- a|", "- a|")

    @Test
    fun deletePadding() = check("  |", " |", "|")

    @Test
    fun deletePaddingTab() = check("\t|", "|", "|")

    @Test
    fun noChange() = check("abc|", "abc|", "abc|")

    @Test
    fun selection() = check("abc|", "[abc]", "[abc]")

}
