package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.manager.StorageManager
import io.github.leonhardweiler.diffusion.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

private const val TAG = "TextVM"

/** How long typing pauses before a note of ordinary size is written to disk. */
private const val SAVE_DEBOUNCE_MS = 500L

/** Up to here a note is written after the usual pause, whatever it holds. */
private const val CHEAP_NOTE_CHARS = 16 * 1024

/** However large a note gets, this is the longest it goes unwritten while typing. */
private const val MAX_SAVE_DEBOUNCE_MS = 3_000L

/**
 * The pause before a note of [length] characters is written to disk.
 *
 * A save is not free: the whole file is written out, the row is rewritten, and
 * the full text search index is built again from it. Half a second is nothing
 * to ask of a note of a few kilobytes and quite a lot to ask of one with a book
 * pasted into it — that one was rewritten twice a second for as long as
 * somebody kept typing in it, which is the disk being busy underneath the thing
 * that is trying to draw. So the pause grows with the note, up to a ceiling.
 *
 * Nothing is risked by waiting longer: leaving the editor, leaving the app and
 * the view model being cleared all write straight away.
 */
internal fun saveDelayMillis(length: Int): Long {
    if (length <= CHEAP_NOTE_CHARS) return SAVE_DEBOUNCE_MS

    return (SAVE_DEBOUNCE_MS * length / CHEAP_NOTE_CHARS)
        .coerceAtMost(MAX_SAVE_DEBOUNCE_MS)
}

open class TextVM() : ViewModel() {

    lateinit var previousNote: Note
        private set

    /**
     * What the note is called, which the editor shows and does not change: a
     * rename is one act and it happens from the note's row in the list.
     */
    val fileName: String get() = previousNote.fileName

    /**
     * The note as it stood when the editor was opened, date and all.
     *
     * Typing a change back out ends at exactly this, and a note that is back to
     * what it was was not written just now. [previousNote] cannot answer that —
     * it moves along with every save.
     */
    private lateinit var openedNote: Note

    private val _content = mutableStateOf(TextFieldValue())
    val content: State<TextFieldValue> get() = _content

    constructor(previousNote: Note) : this() {

        this.previousNote = previousNote
        this.openedNote = previousNote
        _isReading.value = prefs.opensInReadingMode(repo.id, previousNote.relativePath)

        val textFieldValue = TextFieldValue(
            previousNote.content,
            selection = TextRange(0)
        )

        _content.value = textFieldValue.copy()

        Log.d(TAG, "init: $previousNote")
    }

    /**
     * The value handed in is not always the one last written to [content]: the
     * TextField keeps an internal copy and can answer with that, which shows up
     * when a key is held down. Long pressing delete can also stop the calls
     * coming altogether.
     *
     * https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
     */
    open fun onValueChange(v: TextFieldValue) {
        // moving the caret is a value change as well, and it is not one the
        // file has anything to say about
        val typed = _content.value.text != v.text

        _content.value = v.copy()

        if (typed) scheduleSave()
    }

    /**
     * Whether this note is being read rather than written.
     *
     * The note's own answer, not the app's: see
     * [io.github.leonhardweiler.diffusion.data.AppPreferences.opensInReadingMode].
     * A note nobody has ever switched opens for writing.
     */
    private val _isReading = mutableStateOf(false)
    val isReading: State<Boolean> get() = _isReading

    fun setReadOnlyMode(value: Boolean) {
        _isReading.value = value

        viewModelScope.launch {
            prefs.setReadingMode(repo.id, previousNote.relativePath, value)
        }
    }

    /** The repository this note is in — the one the list it was opened from shows. */
    private val repo: RepoSession = MyApp.appModule.activeRepo

    private val storageManager: StorageManager = repo.storageManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper
    private val appScope = MyApp.appModule.appScope
    val prefs = MyApp.appModule.appPreferences

    /**
     * Writes the note as the editor currently holds it.
     *
     * Everything that can be answered without touching the disk is answered
     * here, so that the ui thread is not the one waiting; the write itself goes
     * to the app's scope, which outlives this view model.
     */
    fun save(onSuccess: () -> Unit = {}) {

        if (isPreviousNoteTheSame()) {
            Log.d(TAG, "No modification")
            onSuccess()
            return
        }

        val note = noteAsEdited()
        val previous = previousNote
        // back to what it was, so what was written may be nothing at all
        val restored = isOpenedNoteTheSame()

        // through the storage manager rather than straight into the app's scope:
        // leaving the app writes and syncs at the same moment, and the sync
        // waits for the write it can see
        storageManager.startWrite {
            storageManager.updateNote(new = note, previous = previous)
                .onFailure { uiHelper.makeToast(it.message) }

            // Writing a note means there is something to sync, except when the
            // note is the one that was already there — then only git can say
            // whether anything is left, and it is worth asking for the one
            // write that can take a change back.
            if (restored) storageManager.refreshChangeState()
        }

        // the note is now the one on disk, whatever the write makes of it
        previousNote = note

        onSuccess()
    }

    /**
     * The note the editor is describing: the one it was opened on, with whatever
     * has been typed into it. Where it stands is not in question — the editor
     * cannot move a note, only write it.
     *
     * It keeps the id across a save. That is what the list rows are keyed by,
     * and with the editor saving every 500 ms a new id per keystroke meant
     * moving them along every time.
     */
    private fun noteAsEdited(): Note = Note.new(
        relativePath = previousNote.relativePath,
        content = content.value.text,
        lastModifiedTimeMillis = if (isOpenedNoteTheSame()) {
            openedNote.lastModifiedTimeMillis
        } else {
            Instant.now().toEpochMilli()
        },
        id = previousNote.id,
    )

    /**
     * Whether the editor holds exactly what it was opened with, which is where
     * every change being taken back out again ends up. Such a note was not
     * written today: it is the one it already was, and it keeps its date.
     */
    private fun isOpenedNoteTheSame(): Boolean = openedNote.content == content.value.text

    fun isPreviousNoteTheSame(): Boolean = previousNote.content == content.value.text

    private var saveJob: Job? = null

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(saveDelayMillis(content.value.text.length))
            saveNow()
        }
    }

    /**
     * Writes the note to disk. There is no save button anymore, so this runs
     * while typing, when the editor is left and when the app is stopped.
     * Committing is not part of it, that waits for the user to sync.
     *
     * Nothing can stand in the way of it: the note has a file, and the only
     * thing the editor changes is what is in it. There used to be a draft here
     * for the one case that could not be written — text under a name no file can
     * carry, which is what the editor's name field could produce while a note
     * was being created in it.
     */
    fun saveNow() {
        saveJob?.cancel()
        save()
    }

    override fun onCleared() {
        saveNow()
    }
}


@Composable
fun newEditViewModel(note: Note): TextVM =
    viewModel<TextVM>(factory = viewModelFactory { TextVM(note) })

