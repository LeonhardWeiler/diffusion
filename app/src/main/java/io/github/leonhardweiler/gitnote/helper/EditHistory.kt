package io.github.leonhardweiler.gitnote.helper

import androidx.compose.ui.text.input.TextFieldValue
import io.github.leonhardweiler.gitnote.utils.endsWith
import kotlin.math.absoluteValue

/**
 * How far back an edit can be undone. One more state than that is kept: the one
 * the note was in before the first change.
 */
private const val MAX_UNDO_STEPS = 50

/** How much text has to appear or vanish at once to count as its own step. */
private const val BIG_EDIT = 10

data class HistoryItem(
    val v: TextFieldValue,
    val flagDoNotRemove: Boolean = false,
)

/**
 * The states a note passed through while being edited, and where in them the
 * undo/redo buttons currently stand.
 *
 * The editor hands over a state per keystroke, which is far too fine to undo
 * one at a time. So consecutive states get folded together while they still
 * look like one continuous edit, and only what ends a thought — a space, a full
 * stop, a newline, a jump of the caret, a large change at once — is kept as a
 * step of its own.
 */
class EditHistory {

    private val items = mutableListOf<HistoryItem>()

    /** How many states there are to move between. */
    val size: Int get() = items.size

    /** Where undo and redo currently stand within them. */
    var index = 0

    /** The state at [index], or null when there is none — that is where undo and redo stop. */
    fun stateAt(index: Int): TextFieldValue? = items.getOrNull(index)?.v

    /** Records the state the note is in when it is opened for the first time. */
    fun seed(v: TextFieldValue) {
        if (items.isEmpty()) {
            items.add(HistoryItem(v))
            index = 0
        }
    }

    /**
     * Takes down the state the note is in now.
     *
     * Returns false if the text did not actually change, which happens while the
     * caret is being moved around before anything has been typed. Then only the
     * caret is remembered, and there is nothing to save either.
     */
    fun record(v: TextFieldValue): Boolean {
        if (items.size == 1 && items[0].v.text == v.text) {
            items[0] = HistoryItem(v)
            return false
        }

        dropStatesAheadOfIndex()
        items.add(HistoryItem(v))
        foldLastIntoPreviousIfSameEdit()
        trim()
        index = items.lastIndex
        return true
    }

    /** Typing after an undo abandons what was undone. */
    private fun dropStatesAheadOfIndex() {
        while (items.lastIndex > index) {
            items.removeAt(items.lastIndex)
        }
    }

    /**
     * Two states in a row that are still the same edit leave one step, not two.
     * The state before them has to agree as well, so that the step a fold builds
     * on is one that was deliberately kept.
     */
    private fun foldLastIntoPreviousIfSameEdit() {
        // never fold away the first or the last state
        if (items.size < 3) return

        val previous = items.size - 2
        val last = items.size - 1

        when (sameEdit(items[previous], items[last], caretMayHaveJumped = true)) {
            SameEdit.Yes ->
                if (sameEdit(items[previous - 1], items[previous], false) == SameEdit.Yes) {
                    items.removeAt(previous)
                }

            SameEdit.No -> Unit
            SameEdit.KeepAsStep -> items[last] = items[last].copy(flagDoNotRemove = true)
        }
    }

    /** Forgets the oldest states once there are more of them than we keep. */
    private fun trim() {
        while (items.size > MAX_UNDO_STEPS + 1) {
            items.removeAt(0)
            if (index > 0) index--
        }
    }
}

private enum class SameEdit {
    Yes,
    No,

    /** Not the same edit, and the later state must survive future folding. */
    KeepAsStep,
}

/**
 * Whether [to] continues the edit that produced [from], looked at from the text
 * around the caret. [caretMayHaveJumped] is set for the newest pair only: a
 * caret that moved more than one character ended the edit, but that is not worth
 * asking about a pair that was already accepted as one step.
 */
private fun sameEdit(from: HistoryItem, to: HistoryItem, caretMayHaveJumped: Boolean): SameEdit {
    if (to.flagDoNotRemove) return SameEdit.No

    val caret = to.v.selection.max

    if (caretMayHaveJumped) {
        val movedFar = (to.v.selection.start - from.v.selection.start).absoluteValue > 1 ||
                (to.v.selection.end - from.v.selection.end).absoluteValue > 1
        if (movedFar) return SameEdit.KeepAsStep

        // a word, a sentence or a list marker just ended
        if (to.v.text.endsWith(".", startIndex = caret)) return SameEdit.No
        if (to.v.text.endsWith(" ", startIndex = caret) &&
            !to.v.text.endsWith(". ", startIndex = caret)
        ) return SameEdit.No
        if (to.v.text.endsWith("-", startIndex = caret)) return SameEdit.No
    }

    if (to.v.text.endsWith("\n", startIndex = caret)) return SameEdit.No
    if ((to.v.text.length - from.v.text.length).absoluteValue >= BIG_EDIT) return SameEdit.No

    return SameEdit.Yes
}

/**
 * How many notes keep their history at once.
 *
 * Every one of them holds up to 51 whole copies of a note, so the store is not
 * something to let run on: twenty notes of ten kilobytes would be ten megabytes
 * lying there until the process ends. Five covers what the store is for —
 * leaving a note and coming back to it, and the note before that.
 */
private const val MAX_REMEMBERED_NOTES = 5

/**
 * The undo histories of the notes edited since the app was started. They live
 * here rather than in the edit view model, which is thrown away as soon as a
 * note is left, so coming back to a note can still undo what was typed there.
 * Nothing is written down: when the process ends, the histories are gone.
 *
 * Only the last [MAX_REMEMBERED_NOTES] are kept. Going back far enough into
 * other notes is how one stops meaning to undo the one left behind.
 */
class EditHistoryStore {

    // in access order, so the note that drops out is the one nobody has come
    // back to for longest rather than the one opened first
    private val histories = object : LinkedHashMap<Int, EditHistory>(
        MAX_REMEMBERED_NOTES + 1,
        1f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, EditHistory>) =
            size > MAX_REMEMBERED_NOTES
    }

    /** How many notes are currently remembered. */
    val size: Int get() = histories.size

    fun of(noteId: Int): EditHistory = histories.getOrPut(noteId) { EditHistory() }
}
