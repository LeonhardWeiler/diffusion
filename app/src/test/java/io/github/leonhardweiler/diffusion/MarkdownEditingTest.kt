package io.github.leonhardweiler.diffusion

import androidx.compose.ui.text.input.TextFieldValue
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.markdownSmartEditor
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onCode
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onLink
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onNumberedList
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onQuote
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onTaskList
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onTitle
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.onUnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the formatting buttons do, written down as they behave, so the code
 * underneath can be rearranged without the behaviour moving with it. `|` marks
 * the caret and `[`/`]` fence a selection — see [edit] and [show].
 */
class MarkdownActionsTest {

    private fun check(
        action: (TextFieldValue) -> TextFieldValue,
        vararg cases: Pair<String, String>
    ) {
        for ((input, expected) in cases) {
            assertEquals(expected, show(action(edit(input))), "input: $input")
        }
    }

    /** A heading marker on the current line, toggled. */
    @Test
    fun title() = check(
        ::onTitle,
        "|" to "### |",
        "abc|" to "### abc|",
        "|abc" to "### |abc",
        "ab|c" to "### ab|c",
        "[abc]" to "### [abc]",
        "a\n[bc]" to "a\n### [bc]",
        "one\ntw[o\nthre]e\nfour" to "one\ntw### [o\nthre]e\nfour",
        "### abc|" to "abc|",
        "- a|" to "### - a|",
        "- a\n- b|" to "- a\n### - b|",
        "1. a\n2. b|" to "1. a\n### 2. b|",
        "- [ ] a|" to "### - [ ] a|",
        "- [x] a|" to "### - [x] a|",
        "> a\n> b|" to "> a\n### > b|",
        "  - a|" to "###   - a|",
        "\t1. a|" to "### \t1. a|",
        "[a](url)|" to "### [a](url)|",
    )

    /** Backticks around the selection, a fence when it spans lines. */
    @Test
    fun code() = check(
        ::onCode,
        "|" to "`|`",
        "abc|" to "abc`|`",
        "|abc" to "`|`abc",
        "ab|c" to "ab`|`c",
        "[abc]" to "`[abc]`",
        "a\n[bc]" to "a\n`[bc]`",
        "one\ntw[o\nthre]e\nfour" to "one\ntw```\n[o\nthre]\n```e\nfour",
        "### abc|" to "### abc`|`",
        "- a|" to "- a`|`",
        "- a\n- b|" to "- a\n- b`|`",
        "1. a\n2. b|" to "1. a\n2. b`|`",
        "- [ ] a|" to "- [ ] a`|`",
        "- [x] a|" to "- [x] a`|`",
        "> a\n> b|" to "> a\n> b`|`",
        "  - a|" to "  - a`|`",
        "\t1. a|" to "\t1. a`|`",
        "[a](url)|" to "[a](url)`|`",
    )

    /** A link skeleton around the selection. */
    @Test
    fun link() = check(
        ::onLink,
        "|" to "[|](url)",
        "abc|" to "abc[|](url)",
        "|abc" to "[|](url)abc",
        "ab|c" to "ab[|](url)c",
        "[abc]" to "[abc]([url])",
        "a\n[bc]" to "a\n[bc]([url])",
        "one\ntw[o\nthre]e\nfour" to "one\ntw[o\nthre]([url])e\nfour",
        "### abc|" to "### abc[|](url)",
        "- a|" to "- a[|](url)",
        "- a\n- b|" to "- a\n- b[|](url)",
        "1. a\n2. b|" to "1. a\n2. b[|](url)",
        "- [ ] a|" to "- [ ] a[|](url)",
        "- [x] a|" to "- [x] a[|](url)",
        "> a\n> b|" to "> a\n> b[|](url)",
        "  - a|" to "  - a[|](url)",
        "\t1. a|" to "\t1. a[|](url)",
        "[a](url)|" to "[a](url)[|](url)",
    )

    /** A quote marker per selected line, toggled. */
    @Test
    fun quote() = check(
        ::onQuote,
        "|" to "> |",
        "abc|" to "> abc|",
        "|abc" to "> |abc",
        "ab|c" to "> ab|c",
        "[abc]" to "> [abc]",
        "a\n[bc]" to "a\n> [bc]",
        "one\ntw[o\nthre]e\nfour" to "one\n> tw[o\n> thre]e\nfour",
        "### abc|" to "> ### abc|",
        "- a|" to "> - a|",
        "- a\n- b|" to "- a\n> - b|",
        "1. a\n2. b|" to "1. a\n> 2. b|",
        "- [ ] a|" to "> - [ ] a|",
        "- [x] a|" to "> - [x] a|",
        "> a\n> b|" to "> a\nb|",
        "  - a|" to ">   - a|",
        "\t1. a|" to "> \t1. a|",
        "[a](url)|" to "> [a](url)|",
    )

    /** Dashes per selected line. */
    @Test
    fun unorderedList() = check(
        ::onUnorderedList,
        "|" to "- |",
        "abc|" to "- abc|",
        "|abc" to "- |abc",
        "ab|c" to "- ab|c",
        "[abc]" to "- [abc]",
        "a\n[bc]" to "a\n- [bc]",
        "one\ntw[o\nthre]e\nfour" to "one\n- tw[o\n- thre]e\nfour",
        "### abc|" to "- ### abc|",
        "- a|" to "a|",
        "- a\n- b|" to "- a\nb|",
        "1. a\n2. b|" to "1. a\n- b|",
        "- [ ] a|" to "a|",
        "- [x] a|" to "a|",
        "> a\n> b|" to "> a\n- > b|",
        "  - a|" to "  a|",
        "\t1. a|" to "\t- a|",
        "[a](url)|" to "- [a](url)|",
    )

    /** Numbers per selected line, counted from one. */
    @Test
    fun numberedList() = check(
        ::onNumberedList,
        "|" to "1. |",
        "abc|" to "1. abc|",
        "|abc" to "1. |abc",
        "ab|c" to "1. ab|c",
        "[abc]" to "1. [abc]",
        "a\n[bc]" to "a\n1. [bc]",
        "one\ntw[o\nthre]e\nfour" to "one\n1. tw[o\n2. thre]e\nfour",
        "### abc|" to "1. ### abc|",
        "- a|" to "1. a|",
        "- a\n- b|" to "- a\n1. b|",
        "1. a\n2. b|" to "1. a\nb|",
        "- [ ] a|" to "1. [ ] a|",
        "- [x] a|" to "1. [x] a|",
        "> a\n> b|" to "> a\n1. > b|",
        "  - a|" to "  1. a|",
        "\t1. a|" to "\ta|",
        "[a](url)|" to "1. [a](url)|",
    )

    /** Checkboxes per selected line. */
    @Test
    fun taskList() = check(
        ::onTaskList,
        "|" to "- [ ] |",
        "abc|" to "- [ ] abc|",
        "|abc" to "- [ ] |abc",
        "ab|c" to "- [ ] ab|c",
        "[abc]" to "- [ ] [abc]",
        "a\n[bc]" to "a\n- [ ] [bc]",
        "one\ntw[o\nthre]e\nfour" to "one\n- [ ] tw[o\n- [ ] thre]e\nfour",
        "### abc|" to "- [ ] ### abc|",
        "- a|" to "- [x] a|",
        "- a\n- b|" to "- a\n- [x] b|",
        "1. a\n2. b|" to "1. a\n1. [x] b|",
        "- [ ] a|" to "a|",
        "- [x] a|" to "a|",
        "> a\n> b|" to "> a\n- [ ] > b|",
        "  - a|" to "  - [x] a|",
        "\t1. a|" to "\t1. [x] a|",
        "[a](url)|" to "- [ ] [a](url)|",
    )

}

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
