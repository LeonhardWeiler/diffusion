package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.EditHistory
import io.github.leonhardweiler.diffusion.helper.EditHistoryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The undo history folds a keystroke-by-keystroke stream into steps a person
 * would recognise. These say where it draws the lines.
 */
class EditHistoryTest {

    /** Types [text] one character at a time, as the editor delivers it. */
    private fun typing(text: String, into: EditHistory = EditHistory()): EditHistory {
        into.seed(edit("|"))
        for (i in text.indices) {
            into.record(edit(text.take(i + 1) + "|"))
        }
        return into
    }

    private fun EditHistory.states() = (0 until size).map { show(stateAt(it)!!) }

    @Test
    fun seedIsTheStateBeforeAnyChange() {
        val history = EditHistory()
        history.seed(edit("hello|"))

        assertEquals(listOf("hello|"), history.states())
        assertEquals(0, history.index)
    }

    @Test
    fun seedOnlyCountsOnce() {
        val history = EditHistory()
        history.seed(edit("first|"))
        history.seed(edit("second|"))

        assertEquals(listOf("first|"), history.states())
    }

    @Test
    fun movingTheCaretBeforeTypingIsNotAnEdit() {
        val history = EditHistory()
        history.seed(edit("|abc"))

        assertFalse(history.record(edit("abc|")))
        assertEquals(1, history.size)
        assertEquals(listOf("abc|"), history.states())
    }

    @Test
    fun typingIsAnEdit() {
        val history = EditHistory()
        history.seed(edit("|"))

        assertTrue(history.record(edit("a|")))
    }

    @Test
    fun lettersInAWordCollapseIntoOneStep() {
        val history = typing("word")

        assertEquals(listOf("|", "word|"), history.states())
        assertEquals(1, history.index)
    }

    /** Undo goes back to the finished word, not to the space after it. */
    @Test
    fun aSpaceKeepsTheWordBeforeItAsAStep() {
        val history = typing("one two")

        assertEquals(listOf("|", "one|", "one two|"), history.states())
    }

    @Test
    fun aFullStopKeepsTheSentenceBeforeItAsAStep() {
        val history = typing("no.yes")

        assertEquals(listOf("|", "no|", "no.yes|"), history.states())
    }

    /**
     * A newline is the firmest boundary there is: unlike a space it keeps the
     * state on either side of itself, because the check that folds a step away
     * later on refuses to touch it too.
     */
    @Test
    fun aNewlineKeepsBothSidesOfItself() {
        val history = typing("a\nb")

        assertEquals(listOf("|", "a|", "a\n|", "a\nb|"), history.states())
    }

    /**
     * The rule that a dash ends a step only stops the very next fold, and the
     * pass that runs one keystroke later removes the state anyway. Written down
     * because it reads like it should do more than it does.
     */
    @Test
    fun aDashDoesNotSurviveAsAStepOfItsOwn() {
        val history = typing("-item")

        assertEquals(listOf("|", "-item|"), history.states())
    }

    @Test
    fun pastingALotAtOnceIsItsOwnStep() {
        val history = EditHistory()
        history.seed(edit("|"))
        history.record(edit("a|"))
        history.record(edit("a0123456789|"))

        assertEquals(listOf("|", "a|", "a0123456789|"), history.states())
    }

    @Test
    fun typingAfterAnUndoDropsWhatWasUndone() {
        val history = typing("one two three")
        val stepCount = history.size

        history.index = 1
        history.record(edit("one x|"))

        assertTrue(history.size < stepCount, "the redo tail should be gone")
        assertEquals("one x|", history.states().last())
        assertEquals(history.size - 1, history.index)
    }

    @Test
    fun theHistoryDoesNotGrowWithoutEnd() {
        val history = EditHistory()
        history.seed(edit("|"))
        // each newline is a step of its own, so this cannot fold
        repeat(200) { history.record(edit("x\n".repeat(it + 1) + "|")) }

        assertEquals(51, history.size)
        assertEquals(50, history.index)
    }

    @Test
    fun aLongNoteKeepsFewerStepsThanAShortOne() {
        // 200 kB a step, so three of them are already past the budget
        val chunk = "x".repeat(200 * 1024)

        val history = EditHistory()
        history.seed(edit("|"))
        repeat(10) { history.record(edit("$chunk\n".repeat(1) + "$it|")) }

        assertTrue(
            history.size < 10,
            "a note this size should be undone less far, not remembered whole ten times"
        )
        assertEquals(history.size - 1, history.index, "undo has to stand at the newest state")
        assertTrue(history.size >= 2, "there must still be something to undo to")
    }

    @Test
    fun undoAndRedoStopAtTheEnds() {
        val history = typing("word")

        assertEquals(null, history.stateAt(-1))
        assertEquals(null, history.stateAt(history.size))
    }
}

/**
 * The histories outlive the editor, so something has to say how long. These say
 * how many notes are kept and which one is let go of first.
 */
class EditHistoryStoreTest {

    @Test
    fun onlyTheLastFewNotesAreRemembered() {
        val store = EditHistoryStore()

        repeat(6) { note -> store.of(note).seed(edit("note $note|")) }

        assertEquals(5, store.size)
        assertEquals(0, store.of(0).size, "the note opened first should have been let go of")
    }

    @Test
    fun theNoteComeBackToIsNotTheOneLetGoOf() {
        val store = EditHistoryStore()
        repeat(5) { note -> store.of(note).seed(edit("note $note|")) }

        // opened again, which is what the store is for
        store.of(0)
        store.of(5).seed(edit("note 5|"))

        assertEquals(1, store.of(0).size, "the note come back to was let go of")
        assertEquals(0, store.of(1).size, "the note nobody came back to should be gone")
    }
}
