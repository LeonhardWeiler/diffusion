package io.github.wiiznokes.gitnote.ui.viewmodel.edit

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
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.helper.EditHistory
import io.github.wiiznokes.gitnote.helper.HistoryItem
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.helper.NoteSaver
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import io.github.wiiznokes.gitnote.utils.endsWith
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.zip.DataFormatException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.math.absoluteValue

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

/** How long typing pauses before the note is written to disk. */
private const val SAVE_DEBOUNCE_MS = 500L

open class TextVM() : ViewModel() {

    lateinit var editType: EditType
        private set

    lateinit var previousNote: Note
        private set

    private val _name = mutableStateOf(TextFieldValue())
    val name: State<TextFieldValue> get() = _name

    private val _content = mutableStateOf(TextFieldValue())
    val content: State<TextFieldValue> get() = _content

    /**
     * Kept by the app, not by this view model, so that leaving a note and
     * opening it again can still undo what was typed before.
     */
    private lateinit var editHistory: EditHistory
    private val history: MutableList<HistoryItem> get() = editHistory.items

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
            size = history.size,
            index = editHistory.index,
        )
    }


    val shouldForceNotReadOnlyMode: MutableState<Boolean> = mutableStateOf(false)

    constructor(editType: EditType, previousNote: Note) : this() {

        shouldForceNotReadOnlyMode.value = editType == EditType.Create

        this.editType = editType
        this.previousNote = previousNote

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
        _name.value = TextFieldValue(name, selection = TextRange(name.length))
        val textFieldValue = TextFieldValue(
            content,
            selection = TextRange(0)
        )

        _content.value = textFieldValue.copy()
        initHistory(textFieldValue)

        Log.d(TAG, "init saved: $previousNote, $editType")
    }

    enum class IsSimilarResult {
        Yes,
        No,
        FlagDoNotRemove
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

        if (history.size == 1 && content.value.text == v.text) {
            _content.value = v.copy()
            history[0] = HistoryItem(v.copy())
            return
        }
        _content.value = v.copy()

        val historyManager = historyManager.value

        var i = (history.size - 1) - historyManager.index
        while (i > 0) {
            history.removeAt(history.lastIndex)
            i--
        }

        fun isSimilar(v1: HistoryItem, v2: HistoryItem, firstPass: Boolean): IsSimilarResult {

            if (v2.flagDoNotRemove) {
                return IsSimilarResult.No
            }

            if (firstPass) {
                if ((v2.v.selection.start - v1.v.selection.start).absoluteValue > 1
                    || (v2.v.selection.end - v1.v.selection.end).absoluteValue > 1
                ) {
                    return IsSimilarResult.FlagDoNotRemove
                }

                if (v2.v.text.endsWith(".", startIndex = v2.v.selection.max)) {
                    return IsSimilarResult.No
                }

                if (!v2.v.text.endsWith(
                        ". ",
                        startIndex = v2.v.selection.max
                    ) && v2.v.text.endsWith(" ", startIndex = v2.v.selection.max)
                ) {
                    return IsSimilarResult.No
                }

                if (v2.v.text.endsWith("-", startIndex = v2.v.selection.max)) {
                    return IsSimilarResult.No
                }
            }

            if (v2.v.text.endsWith("\n", startIndex = v2.v.selection.max)) {
                return IsSimilarResult.No
            }
            if ((v2.v.text.length - v1.v.text.length).absoluteValue >= 10) {
                return IsSimilarResult.No
            }

            return IsSimilarResult.Yes
        }

        history.add(HistoryItem(v.copy()))

        fun cleanHistory() {

            // we don't want to remove the last and first index of the history
            // [_,a,ab] -> the size is 3, "a" will be removed
            if (history.size < 3) return

            val secondLast = history.size - 2
            val last = history.size - 1

            when (isSimilar(history[secondLast], history[last], true)) {
                IsSimilarResult.Yes -> {
                    if (isSimilar(
                            history[secondLast - 1],
                            history[secondLast],
                            false
                        ) == IsSimilarResult.Yes
                    ) {
                        history.removeAt(secondLast)
                    }
                }

                IsSimilarResult.No -> {}
                IsSimilarResult.FlagDoNotRemove -> {
                    history[last] = history[last].copy(flagDoNotRemove = true)
                }
            }
        }

        cleanHistory()
        editHistory.trim()
        editHistory.index = history.size - 1
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
        if (index !in history.indices) return

        editHistory.index = index
        publishHistory()
        _content.value = history[index].v.copy()
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

    fun save(onSuccess: () -> Unit = {}) {

        if (isPreviousNoteTheSame()) {
            Log.d(TAG, "No modification")
            onSuccess()
            return
        }

        when (editType) {
            EditType.Create -> create(
                parentPath = previousNote.parentPath,
                name = name.value.text,
                fileExtension = previousNote.fileExtension(),
                content = content.value.text,
                id = previousNote.id
            ).onSuccess {
                editType = EditType.Update
                previousNote = it
                onSuccess()
            }

            EditType.Update -> update(
                previousNote = previousNote,
                parentPath = previousNote.parentPath,
                name = name.value.text,
                fileExtension = previousNote.fileExtension(),
                content = content.value.text,
            ).onSuccess {
                // a saved note is a new row with a new id, and its history has
                // to follow, or reopening the note would start from scratch
                MyApp.appModule.editHistoryStore.move(previousNote.id, it.id)
                previousNote = it
                onSuccess()
            }
        }
    }

    /** Return early to not block the ui thread.
     * This is a best effort to catch problem
     */
    private fun update(
        previousNote: Note,
        parentPath: String,
        name: String,
        fileExtension: FileExtension,
        content: String,
    ): Result<Note> {


        val name = NameValidation.removeEndingWhiteSpace(name)

        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_extension))
            return failure(DataFormatException("extension invalid: $name"))
        }

        val relativePath = "$parentPath/$name.${fileExtension.text}"

        val newNote = Note.new(
            relativePath = relativePath,
            content = content,
        )

        prefs.repoPathBlocking().let { repoPath ->
            val previousFile = previousNote.toFileFs(repoPath)
            if (!previousFile.exist()) {
                Log.w(TAG, "previous file ${previousFile.path} does not exist")
            }

            val newFile = newNote.toFileFs(repoPath)
            if (newFile.path != previousFile.path) {
                if (newFile.exist()) {
                    uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
                    return failure(EditException(EditExceptionType.NoteAlreadyExist))
                }
            }
        }

        appScope.launch {

            storageManager.updateNote(
                new = newNote,
                previous = previousNote
            ).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

        }
        return success(newNote)
    }

    /** Return early to note block the ui thread.
     * This is a best effort to catch problem
     */
    private fun create(
        parentPath: String,
        name: String,
        fileExtension: FileExtension,
        content: String,
        id: Int
    ): Result<Note> {

        val name = NameValidation.removeEndingWhiteSpace(name)

        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_extension))
            return failure(DataFormatException("extension invalid: $name"))
        }

        val relativePath = "$parentPath/$name.${fileExtension.text}"

        val note = Note.new(
            relativePath = relativePath,
            content = content,
            id = id,
        )

        if (note.toFileFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
            return failure(EditException(EditExceptionType.NoteAlreadyExist))
        }

        appScope.launch {
            storageManager.createNote(note).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }
        }

        return success(note)
    }

    fun isPreviousNoteTheSame(): Boolean =
        previousNote.nameWithoutExtension() == NameValidation.removeEndingWhiteSpace(name.value.text)
                && previousNote.content == content.value.text

    private var saveJob: Job? = null

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
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
            noteSaver.clear()
        } else {
            writeDraft()
        }
    }

    private fun writeDraft() {
        noteSaver.save(
            shouldSave = !isPreviousNoteTheSame(),
            name = name.value.text,
            content = content.value.text,
            previousNote = previousNote,
            editType = editType
        )
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

