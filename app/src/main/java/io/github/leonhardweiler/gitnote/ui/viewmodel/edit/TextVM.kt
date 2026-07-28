package io.github.leonhardweiler.gitnote.ui.viewmodel.edit

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.gitnote.MyApp
import io.github.leonhardweiler.gitnote.R
import io.github.leonhardweiler.gitnote.data.room.Note
import io.github.leonhardweiler.gitnote.helper.EditHistory
import io.github.leonhardweiler.gitnote.helper.NameValidation
import io.github.leonhardweiler.gitnote.helper.NoteSaver
import io.github.leonhardweiler.gitnote.helper.UiHelper
import io.github.leonhardweiler.gitnote.manager.StorageManager
import io.github.leonhardweiler.gitnote.ui.destination.EditParams
import io.github.leonhardweiler.gitnote.ui.model.EditType
import io.github.leonhardweiler.gitnote.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.zip.DataFormatException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

data class History(
    val index: Int,
    val size: Int,
)

enum class EditExceptionType {
    NoteAlreadyExist,
}

class EditException(
    val type: EditExceptionType,
) : Exception(type.name)

private const val TAG = "TextVM"

/** How long typing pauses before a note of ordinary size is written to disk. */
private const val SAVE_DEBOUNCE_MS = 500L

/** Up to here a note is written after the usual pause, whatever it holds. */
private const val CHEAP_NOTE_CHARS = 16 * 1024

/** However large a note gets, this is the longest it goes unwritten while typing. */
private const val MAX_SAVE_DEBOUNCE_MS = 3_000L

open class TextVM() : ViewModel() {

    lateinit var editType: EditType
        private set

    lateinit var previousNote: Note
        private set

    /**
     * The note as it stood when the editor was opened, date and all.
     *
     * Undoing every change ends at exactly this, and a note that is back to what
     * it was was not written just now. [previousNote] cannot answer that — it
     * moves along with every save.
     */
    private lateinit var openedNote: Note

    private val _name = mutableStateOf(TextFieldValue())
    val name: State<TextFieldValue> get() = _name

    private val _content = mutableStateOf(TextFieldValue())
    val content: State<TextFieldValue> get() = _content

    /**
     * Kept by the app, not by this view model, so that leaving a note and
     * opening it again can still undo what was typed before.
     */
    private lateinit var editHistory: EditHistory

    private val _historyManager: MutableStateFlow<History> =
        MutableStateFlow(History(index = 0, size = 1))
    val historyManager: StateFlow<History>
        get() = _historyManager.asStateFlow()

    private fun initHistory(initial: TextFieldValue) {
        editHistory = MyApp.appModule.editHistoryStore.of(previousNote.id)
        editHistory.seed(initial)
        publishHistory()
    }

    private fun publishHistory() {
        _historyManager.value = History(
            size = editHistory.size,
            index = editHistory.index,
        )
    }


    val shouldForceNotReadOnlyMode: MutableState<Boolean> = mutableStateOf(false)

    constructor(editType: EditType, previousNote: Note) : this() {

        shouldForceNotReadOnlyMode.value = editType == EditType.Create

        this.editType = editType
        this.previousNote = previousNote
        this.openedNote = previousNote

        _name.value = previousNote.nameWithoutExtension().let {
            TextFieldValue(it, selection = TextRange(it.length))
        }

        val textFieldValue = TextFieldValue(
            previousNote.content,
            selection = TextRange(0)
        )

        _content.value = textFieldValue.copy()
        initHistory(textFieldValue)

        Log.d(TAG, "init: $previousNote, $editType")
    }

    constructor(
        editType: EditType,
        previousNote: Note,
        name: String,
        content: String,
    ) : this() {

        shouldForceNotReadOnlyMode.value = editType == EditType.Create

        this.editType = editType
        this.previousNote = previousNote
        this.openedNote = previousNote
        _name.value = TextFieldValue(name, selection = TextRange(name.length))
        val textFieldValue = TextFieldValue(
            content,
            selection = TextRange(0)
        )

        _content.value = textFieldValue.copy()
        initHistory(textFieldValue)

        Log.d(TAG, "init saved: $previousNote, $editType")
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
        _content.value = v.copy()

        // moving the caret before anything was typed changes nothing worth
        // saving, and the history says so
        if (!editHistory.record(v.copy())) return

        publishHistory()
        scheduleSave()
    }

    fun onNameChange(v: TextFieldValue) {
        _name.value = v
        scheduleSave()
    }


    fun undo() {
        moveInHistory(editHistory.index - 1)
    }

    fun redo() {
        moveInHistory(editHistory.index + 1)
    }

    private fun moveInHistory(index: Int) {
        val state = editHistory.stateAt(index) ?: return

        editHistory.index = index
        publishHistory()
        _content.value = state.copy()
        scheduleSave()
    }

    fun setReadOnlyMode(value: Boolean) {
        shouldForceNotReadOnlyMode.value = false

        viewModelScope.launch {
            prefs.isReadOnlyModeActive.update(value)
        }
    }

    private val storageManager: StorageManager = MyApp.appModule.storageManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper
    private val noteSaver: NoteSaver = MyApp.appModule.noteSaver
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

        val note = noteAsEdited().getOrElse { return }
        refuseToOverwrite(note).getOrElse { return }

        val previous = previousNote
        val isNew = editType == EditType.Create
        // undone back to what it was, so what was written may be nothing at all
        val restored = isOpenedNoteTheSame()

        // through the storage manager rather than straight into the app's scope:
        // leaving the app writes and syncs at the same moment, and the sync
        // waits for the write it can see
        storageManager.startWrite {
            if (isNew) {
                storageManager.createNote(note)
            } else {
                storageManager.updateNote(new = note, previous = previous)
            }.onFailure { uiHelper.makeToast(it.message) }

            // Writing a note means there is something to sync, except when the
            // note is the one that was already there — then only git can say
            // whether anything is left, and it is worth asking for the one
            // write that can take a change back.
            if (restored) storageManager.refreshChangeState()
        }

        // the note is now the one on disk, whatever the write makes of it: a
        // second save must not try to create it again or rename it from a name
        // that is gone
        editType = EditType.Update
        previousNote = note
        onSuccess()
    }

    /**
     * The note the editor is describing, or why it does not describe one.
     *
     * It keeps the id across a save. That is what the undo history and the list
     * rows are keyed by, and with the editor saving every 500 ms a new id per
     * keystroke meant moving both along every time.
     */
    private fun noteAsEdited(): Result<Note> {
        val name = NameValidation.removeEndingWhiteSpace(name.value.text)
        val extension = previousNote.fileExtension()

        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(extension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_extension))
            return failure(DataFormatException("extension invalid: $name"))
        }

        return success(
            Note.new(
                relativePath = "${previousNote.parentPath}/$name.${extension.text}",
                content = content.value.text,
                lastModifiedTimeMillis = if (isOpenedNoteTheSame()) {
                    openedNote.lastModifiedTimeMillis
                } else {
                    Instant.now().toEpochMilli()
                },
                id = previousNote.id,
            )
        )
    }

    /**
     * Whether the editor holds exactly what it was opened with, which is where
     * undoing everything ends up. Such a note was not written today: it is the
     * one it already was, and it keeps its date.
     */
    private fun isOpenedNoteTheSame(): Boolean =
        openedNote.nameWithoutExtension() == NameValidation.removeEndingWhiteSpace(name.value.text)
                && openedNote.content == content.value.text

    /**
     * Refuses a note whose file is already somebody else's.
     *
     * Writing over the note it already is, is not that — which is the usual
     * case, since this runs on every typing pause.
     */
    private fun refuseToOverwrite(note: Note): Result<Unit> {
        if (editType == EditType.Update && note.relativePath == previousNote.relativePath) {
            return success(Unit)
        }

        if (note.toFileFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
            return failure(EditException(EditExceptionType.NoteAlreadyExist))
        }

        return success(Unit)
    }

    fun isPreviousNoteTheSame(): Boolean =
        previousNote.nameWithoutExtension() == NameValidation.removeEndingWhiteSpace(name.value.text)
                && previousNote.content == content.value.text

    private var saveJob: Job? = null

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(saveDelayMillis())
            saveNow()
        }
    }

    /**
     * The pause grows with the note.
     *
     * A save is not free: the whole file is written out, the row is rewritten,
     * and the full text search index is built again from it. Half a second is
     * nothing to ask of a note of a few kilobytes and quite a lot to ask of one
     * with a book pasted into it — that one was rewritten twice a second for as
     * long as somebody kept typing in it, which is the disk being busy
     * underneath the thing that is trying to draw.
     *
     * Nothing is risked by waiting longer: leaving the editor, leaving the app
     * and the view model being cleared all write straight away.
     */
    private fun saveDelayMillis(): Long {
        val length = content.value.text.length
        if (length <= CHEAP_NOTE_CHARS) return SAVE_DEBOUNCE_MS

        return (SAVE_DEBOUNCE_MS * length / CHEAP_NOTE_CHARS)
            .coerceAtMost(MAX_SAVE_DEBOUNCE_MS)
    }

    /** The name that was last complained about, so the toast is shown once. */
    private var rejectedName: String? = null

    /**
     * Writes the note to disk. There is no save button anymore, so this runs
     * while typing, when the editor is left and when the app is stopped.
     * Committing is not part of it, that waits for the user to sync.
     */
    fun saveNow() {
        saveJob?.cancel()

        val name = NameValidation.removeEndingWhiteSpace(name.value.text)
        if (!NameValidation.check(name)) {
            // a note without a usable name has nowhere to go on disk; the draft
            // holds the text until the name is one a file can carry
            if (name.isNotEmpty() && name != rejectedName) {
                rejectedName = name
                uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            }
            writeDraft()
            return
        }
        rejectedName = null

        save()

        // the draft is the net for what could not be written yet — an invalid
        // name, say. Once the note is on disk it would only restore itself.
        if (isPreviousNoteTheSame()) {
            appScope.launch { noteSaver.clear() }
        } else {
            writeDraft()
        }
    }

    /**
     * Runs in the app's scope rather than this one: the last thing the editor
     * does before it is cleared is write a draft, and a scope that dies with
     * the editor would take that write with it.
     */
    private fun writeDraft() {
        val shouldSave = !isPreviousNoteTheSame()
        val name = name.value.text
        val content = content.value.text
        val previousNote = previousNote
        val editType = editType

        appScope.launch {
            noteSaver.save(
                shouldSave = shouldSave,
                name = name,
                content = content,
                previousNote = previousNote,
                editType = editType
            )
        }
    }

    override fun onCleared() {
        saveNow()
    }
}


@Composable
fun newEditViewModel(editParams: EditParams): TextVM {

    return when (editParams) {
        is EditParams.Idle -> viewModel<TextVM>(
            factory = viewModelFactory {
                TextVM(editParams.editType, editParams.note)
            }
        )

        is EditParams.Saved -> {
            viewModel<TextVM>(
                factory = viewModelFactory {
                    TextVM(
                        editType = editParams.editType,
                        previousNote = editParams.note,
                        name = editParams.name,
                        content = editParams.content,
                    )
                }
            )
        }
    }
}

