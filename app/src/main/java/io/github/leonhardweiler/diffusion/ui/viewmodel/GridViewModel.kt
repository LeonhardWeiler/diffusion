package io.github.leonhardweiler.diffusion.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.LIMIT_FILE_SIZE
import io.github.leonhardweiler.diffusion.data.index.IndexState
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import io.github.leonhardweiler.diffusion.data.index.notesIn
import io.github.leonhardweiler.diffusion.data.index.search
import io.github.leonhardweiler.diffusion.data.index.sortDatesNow
import io.github.leonhardweiler.diffusion.helper.NameValidation
import io.github.leonhardweiler.diffusion.helper.PathProblem
import io.github.leonhardweiler.diffusion.helper.ResolvedPath
import io.github.leonhardweiler.diffusion.helper.describe
import io.github.leonhardweiler.diffusion.helper.keepExtension
import io.github.leonhardweiler.diffusion.helper.resolveRepoPath
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.manager.StorageManager
import io.github.leonhardweiler.diffusion.ui.model.FileExtension
import io.github.leonhardweiler.diffusion.ui.model.GridItem
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.without
import io.github.leonhardweiler.diffusion.helper.getParentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GridViewModel : ViewModel() {
    companion object {
        private const val TAG = "GridViewModel"
    }

    private val repo: RepoSession = MyApp.appModule.activeRepo

    private val storageManager: StorageManager = repo.storageManager
    private val appScope = MyApp.appModule.appScope

    private val repoPath: String = repo.path
    private val index = repo.noteIndex
    val uiHelper = MyApp.appModule.uiHelper

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val syncState = storageManager.syncState

    val hasLocalChanges = storageManager.hasLocalChanges

    fun syncWithRemote() {
        storageManager.announceSyncStart()

        appScope.launch {
            storageManager.syncWithRemote()
        }
    }

    private val _currentNoteFolderRelativePath = MutableStateFlow("")
    val currentNoteFolderRelativePath: StateFlow<String>
        get() = _currentNoteFolderRelativePath.asStateFlow()

    private val _selectedNotes: MutableStateFlow<List<NoteHeader>> =
        MutableStateFlow(emptyList())

    val selectedNotes: StateFlow<List<NoteHeader>>
        get() = _selectedNotes.asStateFlow()

    private val _selectedFolders: MutableStateFlow<List<NoteFolder>> =
        MutableStateFlow(emptyList())

    val selectedFolders: StateFlow<List<NoteFolder>>
        get() = _selectedFolders.asStateFlow()

    val selectionSize: StateFlow<Int> =
        combine(selectedNotes, selectedFolders) { notes, folders -> notes.size + folders.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _sortDates: MutableStateFlow<Map<Int, Long>> = MutableStateFlow(emptyMap())

    init {
        Log.d(TAG, "init")

        viewModelScope.launch {
            index.state
                .map { it.reads }
                .distinctUntilChanged()
                .collect { resort() }
        }
    }

    /**
     * Puts the list in order again, and is only called where nobody can see it
     * happen: a folder opened or left, a search begun or ended (not per letter),
     * the app stopped, and every read of the whole repository.
     */
    fun resort() {
        _sortDates.value = index.state.value.sortDatesNow()
    }

    fun search(query: String) {
        if (query.isEmpty() != _query.value.isEmpty()) resort()

        viewModelScope.launch {
            _query.emit(query)
        }
    }

    fun clearQuery() {
        if (_query.value.isNotEmpty()) resort()

        viewModelScope.launch {
            _query.emit("")
        }
    }

    fun openFolder(relativePath: String) {
        resort()

        viewModelScope.launch {
            _currentNoteFolderRelativePath.emit(relativePath)
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        val problem = when {
            name.isBlank() -> PathProblem.Empty
            else -> NameValidation.illegalCharacter(name)?.let {
                PathProblem.InvalidCharacter(it)
            }
        }

        if (problem != null) {
            uiHelper.makeToast(problem.describe(uiHelper))
            return false
        }

        val relativePath = "$relativeParentPath/$name"

        val noteFolder = NoteFolder.new(
            relativePath = relativePath
        )

        if (noteFolder.toFolderFs(repoPath).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_already_exist, name))
            return false
        }

        appScope.launch {
            storageManager.createNoteFolder(noteFolder)
        }

        return true
    }

    fun selectNote(note: NoteHeader, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedNotes.value.plus(note)
        } else {
            selectedNotes.value.without(note)
        }.let {
            _selectedNotes.emit(it)
        }
    }

    fun selectFolder(folder: NoteFolder, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedFolders.value.plus(folder)
        } else {
            selectedFolders.value.without(folder)
        }.let {
            _selectedFolders.emit(it)
        }
    }

    fun selectAll() = viewModelScope.launch {
        val folderPath = currentNoteFolderRelativePath.value
        val currentQuery = query.value

        val state = index.state.value

        _selectedNotes.emit(
            withContext(Dispatchers.IO) { state.search(folderPath, currentQuery) }
        )

        _selectedFolders.emit(
            if (currentQuery.isEmpty()) {
                state.foldersIn(folderPath).map { it.noteFolder }
            } else {
                emptyList()
            }
        )
    }

    fun unselectAll() = viewModelScope.launch {
        _selectedNotes.emit(emptyList())
        _selectedFolders.emit(emptyList())
    }

    fun deleteSelection() {
        appScope.launch {
            val folders = selectedFolders.value
            val notes = selectedNotes.value
            unselectAll()

            val insideDeletedFolder = folders.map { "${it.relativePath}/" }
            val paths = notes
                .map { it.relativePath }
                .filterNot { path -> insideDeletedFolder.any(path::startsWith) }

            if (folders.isNotEmpty()) storageManager.deleteNoteFolders(folders)
            if (paths.isNotEmpty()) storageManager.deleteNotes(paths)
        }
    }

    fun deleteNote(note: NoteHeader) {
        appScope.launch {
            storageManager.deleteNote(note.relativePath)
        }
    }

    fun openNote(note: NoteHeader, onLoaded: (Note) -> Unit) = viewModelScope.launch {
        val loaded = withContext(Dispatchers.IO) { index.loadNote(note.relativePath) }
        if (loaded == null) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_not_found))
            return@launch
        }

        val size = withContext(Dispatchers.IO) {
            runCatching { NodeFs.File.fromPath(repoPath, note.relativePath).fileSize() }
                .getOrDefault(0L)
        }
        if (size > LIMIT_FILE_SIZE) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_too_large, note.fileName))
            return@launch
        }

        onLoaded(loaded)
    }

    fun openExternally(note: NoteHeader, onResolved: (String) -> Unit) = viewModelScope.launch {
        onResolved("${repoPath}/${note.relativePath}")
    }

    fun renameNote(note: NoteHeader, typed: String) {
        val resolved = resolveRepoPath(
            getParentPath(note.relativePath),
            keepExtension(typed.trim(), note.extension())
        )

        if (resolved !is ResolvedPath.Ok) {
            uiHelper.makeToast((resolved as ResolvedPath.Bad).problem.describe(uiHelper))
            return
        }

        appScope.launch {
            val loaded = withContext(Dispatchers.IO) { index.loadNote(note.relativePath) }
            if (loaded == null) {
                uiHelper.makeToast(uiHelper.getString(R.string.error_note_not_found))
                return@launch
            }

            storageManager.renameNote(loaded, resolved.relativePath)
        }
    }

    fun deleteFolder(noteFolder: NoteFolder) {
        appScope.launch {
            storageManager.deleteNoteFolder(noteFolder)
        }
    }

    fun renameFolder(noteFolder: NoteFolder, typed: String) {
        val parentPath = getParentPath(noteFolder.relativePath)

        val resolved = resolveRepoPath(parentPath, typed)
        if (resolved !is ResolvedPath.Ok) {
            uiHelper.makeToast((resolved as ResolvedPath.Bad).problem.describe(uiHelper))
            return
        }

        appScope.launch {
            storageManager.renameNoteFolder(noteFolder, resolved.relativePath)
        }
    }

    fun defaultNewNoteName(): String =
        query.value.let { if (NameValidation.check(it)) it else "" }

    fun createNote(typed: String): Boolean {
        val resolved = resolveRepoPath(currentNoteFolderRelativePath.value, typed.trim())
        if (resolved !is ResolvedPath.Ok) {
            uiHelper.makeToast((resolved as ResolvedPath.Bad).problem.describe(uiHelper))
            return false
        }

        val note = Note.new(relativePath = resolved.relativePath)

        if (note.parentPath.isNotEmpty() &&
            !NodeFs.Folder.fromPath(repoPath, note.parentPath).exist()
        ) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_not_found, note.parentPath))
            return false
        }

        if (note.toFileFs(repoPath).exist()) {
            uiHelper.makeToast(
                uiHelper.getString(R.string.error_file_already_exist, note.fileName)
            )
            return false
        }

        appScope.launch {
            storageManager.createNote(note)
        }

        return true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridItems: StateFlow<List<GridItem>> =
        combine(
            index.state,
            currentNoteFolderRelativePath,
            query,
            _sortDates,
        ) { state, folderPath, query, sortDates ->
            ListInput(state, folderPath, query, sortDates)
        }
            .mapLatest { (state, folderPath, query, sortDates) ->

                val notes = if (query.isEmpty()) {
                    state.notesIn(folderPath, sortDates)
                } else {
                    state.search(folderPath, query, sortDates)
                }

                val duplicated = notes
                    .groupingBy { it.fileName }
                    .eachCount()

                buildList {
                    if (query.isEmpty()) {
                        if (folderPath.isNotEmpty()) {
                            add(GridItem.ParentFolder(getParentPath(folderPath)))
                        }
                        state.foldersIn(folderPath, sortDates)
                            .forEach { add(GridItem.Folder(it)) }
                    }

                    notes.forEach { note ->
                        add(
                            GridItem.Note(
                                GridNote(
                                    note = note,
                                    isUnique = duplicated[note.fileName] == 1,
                                )
                            )
                        )
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private data class ListInput(
    val state: IndexState,
    val folderPath: String,
    val query: String,
    val sortDates: Map<Int, Long>,
)
