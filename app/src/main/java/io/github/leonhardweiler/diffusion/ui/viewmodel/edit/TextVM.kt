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

private const val SAVE_DEBOUNCE_MS = 500L

private const val CHEAP_NOTE_CHARS = 16 * 1024

private const val MAX_SAVE_DEBOUNCE_MS = 3_000L

/** A save writes the whole file, so a note with a book in it waits longer. */
internal fun saveDelayMillis(length: Int): Long {
    if (length <= CHEAP_NOTE_CHARS) return SAVE_DEBOUNCE_MS

    return (SAVE_DEBOUNCE_MS * length / CHEAP_NOTE_CHARS)
        .coerceAtMost(MAX_SAVE_DEBOUNCE_MS)
}

open class TextVM() : ViewModel() {
    lateinit var previousNote: Note
        private set

    val fileName: String get() = previousNote.fileName

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

    open fun onValueChange(v: TextFieldValue) {
        val typed = _content.value.text != v.text

        _content.value = v.copy()

        if (typed) scheduleSave()
    }

    private val _isReading = mutableStateOf(false)
    val isReading: State<Boolean> get() = _isReading

    fun setReadOnlyMode(value: Boolean) {
        _isReading.value = value

        viewModelScope.launch {
            prefs.setReadingMode(repo.id, previousNote.relativePath, value)
        }
    }

    private val repo: RepoSession = MyApp.appModule.activeRepo

    private val storageManager: StorageManager = repo.storageManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper
    private val appScope = MyApp.appModule.appScope
    val prefs = MyApp.appModule.appPreferences

    fun save(onSuccess: () -> Unit = {}) {
        if (isPreviousNoteTheSame()) {
            Log.d(TAG, "No modification")
            onSuccess()
            return
        }

        val note = noteAsEdited()
        val previous = previousNote
        val restored = isOpenedNoteTheSame()

        storageManager.startWrite {
            storageManager.updateNote(new = note, previous = previous)
                .onFailure { uiHelper.makeToast(it.message) }

            if (restored) storageManager.refreshChangeState()
        }

        previousNote = note

        onSuccess()
    }

    /** A note typed back to what it was keeps the date it was opened with. */
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

