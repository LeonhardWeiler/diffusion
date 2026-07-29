package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

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
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.helper.EditHistory
import io.github.leonhardweiler.diffusion.helper.NameValidation
import io.github.leonhardweiler.diffusion.helper.NoteSaver
import io.github.leonhardweiler.diffusion.helper.PathProblem
import io.github.leonhardweiler.diffusion.helper.ResolvedPath
import io.github.leonhardweiler.diffusion.helper.resolveRepoPath
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.StorageManager
import io.github.leonhardweiler.diffusion.ui.destination.EditParams
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.viewmodel.viewModelFactory
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
    FolderNotFound,
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

        _name.value = previousNote.fileName.let {
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

        // A move is read against where the note is, so once it has moved the
        // field has to say where it now is — "../notes.md" left standing would
        // take it a folder further up on the next save, and keep going.
        if (note.parentPath != previous.parentPath) {
            _name.value = TextFieldValue(note.fileName, selection = TextRange(note.fileName.length))
        }

        onSuccess()
    }

    /**
     * Where the typed name says the note belongs.
     *
     * The field is a path, not only a name: `../notes.md` moves the note a
     * folder up, `archive/notes.md` into one beside it. What is typed is read
     * against the folder the note is in *now*, which is why a save that moved it
     * writes the plain file name back into the field — otherwise the next save
     * would move it the same way again.
     *
     * A last segment without a dot keeps the extension the note already has, so
     * that typing a plain name still means what it always did and only somebody
     * who writes one changes the type.
     */
    private fun editedPath(): ResolvedPath {
        val typed = NameValidation.removeEndingWhiteSpace(name.value.text)

        val withExtension = if (typed.substringAfterLast('/').contains('.')) {
            typed
        } else {
            "$typed.${previousNote.fileExtension().text}"
        }

        return resolveRepoPath(previousNote.parentPath, withExtension)
    }

    /**
     * The note the editor is describing, or why it does not describe one.
     *
     * It keeps the id across a save. That is what the undo history and the list
     * rows are keyed by, and with the editor saving every 500 ms a new id per
     * keystroke meant moving both along every time.
     */
    private fun noteAsEdited(): Result<Note> {
        val relativePath = when (val resolved = editedPath()) {
            is ResolvedPath.Ok -> resolved.relativePath

            is ResolvedPath.Bad -> {
                uiHelper.makeToast(uiHelper.getString(resolved.problem.message()))
                return failure(DataFormatException("path invalid: ${name.value.text}"))
            }
        }

        return success(
            Note.new(
                relativePath = relativePath,
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
        editedPath().let { it is ResolvedPath.Ok && it.relativePath == openedNote.relativePath }
                && openedNote.content == content.value.text

    /**
     * Refuses a note whose file is already somebody else's, or whose folder does
     * not exist.
     *
     * Writing over the note it already is, is not that — which is the usual
     * case, since this runs on every typing pause.
     *
     * The folder has to be there already, the way `mv` wants it to be: a typo in
     * a path would otherwise leave a folder behind that nobody asked for, and
     * the note in it would be somewhere the user did not mean.
     */
    private fun refuseToOverwrite(note: Note): Result<Unit> {
        if (editType == EditType.Update && note.relativePath == previousNote.relativePath) {
            return success(Unit)
        }

        val repoPath = prefs.repoPathBlocking()

        if (note.parentPath.isNotEmpty() &&
            !NodeFs.Folder.fromPath(repoPath, note.parentPath).exist()
        ) {
            uiHelper.makeToast(
                uiHelper.getString(R.string.error_folder_not_found, note.parentPath)
            )
            return failure(EditException(EditExceptionType.FolderNotFound))
        }

        if (note.toFileFs(repoPath).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
            return failure(EditException(EditExceptionType.NoteAlreadyExist))
        }

        return success(Unit)
    }

    fun isPreviousNoteTheSame(): Boolean =
        editedPath().let { it is ResolvedPath.Ok && it.relativePath == previousNote.relativePath }
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

    /** What to say about a path the user cannot be given. */
    private fun PathProblem.message(): Int = when (this) {
        PathProblem.Empty -> R.string.error_invalid_name
        PathProblem.InvalidName -> R.string.error_invalid_name
        PathProblem.AboveRoot -> R.string.error_path_above_root
    }

    /**
     * Writes the note to disk. There is no save button anymore, so this runs
     * while typing, when the editor is left and when the app is stopped.
     * Committing is not part of it, that waits for the user to sync.
     */
    fun saveNow() {
        saveJob?.cancel()

        val typed = NameValidation.removeEndingWhiteSpace(name.value.text)
        val resolved = editedPath()
        if (resolved is ResolvedPath.Bad) {
            // a note without a usable path has nowhere to go on disk; the draft
            // holds the text until the name is one a file can carry
            if (typed.isNotEmpty() && typed != rejectedName) {
                rejectedName = typed
                uiHelper.makeToast(uiHelper.getString(resolved.problem.message()))
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

