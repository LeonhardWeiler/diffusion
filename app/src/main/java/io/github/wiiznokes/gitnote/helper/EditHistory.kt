package io.github.wiiznokes.gitnote.helper

import androidx.compose.ui.text.input.TextFieldValue

/**
 * How far back an edit can be undone. One more state than that is kept: the one
 * the note was in before the first change.
 */
private const val MAX_UNDO_STEPS = 50

data class HistoryItem(
    val v: TextFieldValue,
    val flagDoNotRemove: Boolean = false,
)

/**
 * The states a note passed through while being edited, and where in them the
 * undo/redo buttons currently stand.
 */
class EditHistory {

    val items = mutableListOf<HistoryItem>()

    var index = 0

    /** Records the state the note is in when it is opened for the first time. */
    fun seed(v: TextFieldValue) {
        if (items.isEmpty()) {
            items.add(HistoryItem(v))
            index = 0
        }
    }

    /** Forgets the oldest states once there are more of them than we keep. */
    fun trim() {
        while (items.size > MAX_UNDO_STEPS + 1) {
            items.removeAt(0)
            if (index > 0) index--
        }
    }
}

/**
 * The undo histories of the notes edited since the app was started. They live
 * here rather than in the edit view model, which is thrown away as soon as a
 * note is left, so coming back to a note can still undo what was typed there.
 * Nothing is written down: when the process ends, the histories are gone.
 */
class EditHistoryStore {

    private val histories = mutableMapOf<Int, EditHistory>()

    fun of(noteId: Int): EditHistory = histories.getOrPut(noteId) { EditHistory() }
}
