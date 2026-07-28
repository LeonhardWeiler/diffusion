package io.github.wiiznokes.gitnote.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.map
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.GridItem
import io.github.wiiznokes.gitnote.ui.model.NoteHeader
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.utils.getParentPath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GridViewModel : ViewModel() {

    companion object {
        private const val TAG = "GridViewModel"

        /** Notes are shown newest first, folders alphabetically. */
        private val NOTE_SORT_ORDER = SortOrder.MostRecent
        private val FOLDER_SORT_ORDER = SortOrder.AZ
    }


    private val storageManager: StorageManager = MyApp.appModule.storageManager
    private val appScope = MyApp.appModule.appScope

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase
    private val dao = db.repoDatabaseDao
    val uiHelper = MyApp.appModule.uiHelper

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val syncState = storageManager.syncState

    /** Commits what has been written since the last sync, then pulls and pushes. */
    fun syncWithRemote() {
        appScope.launch {
            storageManager.syncWithRemote()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()


    private val _currentNoteFolderRelativePath = MutableStateFlow("")
    val currentNoteFolderRelativePath: StateFlow<String>
        get() = _currentNoteFolderRelativePath.asStateFlow()


    private val _selectedNotes: MutableStateFlow<List<NoteHeader>> =
        MutableStateFlow(emptyList())

    val selectedNotes: StateFlow<List<NoteHeader>>
        get() = _selectedNotes.asStateFlow()


    init {
        Log.d(TAG, "init")
    }

    suspend fun refreshSelectedNotes() {
        selectedNotes.value.filter { selectedNote ->
            dao.isNoteExist(selectedNote.relativePath)
        }.let { newSelectedNotes ->
            _selectedNotes.emit(newSelectedNotes)
        }
    }

    /**
     * Reads the files back into the database, which catches whatever changed
     * outside the app. The remote is not part of it.
     */
    fun refresh() {
        appScope.launch {
            _isRefreshing.emit(true)
            storageManager.updateDatabase(force = true)
            refreshSelectedNotes()
            _isRefreshing.emit(false)
        }
    }

    fun updateSettings(f: suspend AppPreferences.() -> Unit) {
        viewModelScope.launch { prefs.f() }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _query.emit(query)
        }
    }

    fun clearQuery() {
        viewModelScope.launch {
            _query.emit("")
        }
    }

    fun openFolder(relativePath: String) {
        viewModelScope.launch {
            _currentNoteFolderRelativePath.emit(relativePath)
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return false
        }

        val relativePath = "$relativeParentPath/$name"

        val noteFolder = NoteFolder.new(
            relativePath = relativePath
        )

        if (noteFolder.toFolderFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_already_exist))
            return false
        }

        appScope.launch {
            storageManager.createNoteFolder(noteFolder)
        }

        return true
    }


    /**
     * @param add true if the note must be selected, false otherwise
     */
    fun selectNote(note: NoteHeader, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedNotes.value.plus(note)
        } else {
            selectedNotes.value.minus(note)
        }.let {
            _selectedNotes.emit(it)
        }
    }

    fun unselectAllNotes() = viewModelScope.launch {
        _selectedNotes.emit(emptyList())
    }

    fun deleteSelectedNotes() {
        appScope.launch {
            val currentSelectedNotes = selectedNotes.value
            unselectAllNotes()
            storageManager.deleteNotes(currentSelectedNotes.map { it.relativePath })
        }
    }

    fun deleteNote(note: NoteHeader) {
        appScope.launch {
            storageManager.deleteNote(note.relativePath)
        }
    }

    /**
     * Reads the note behind a row of the list, which carries no content, and
     * hands it to the editor. A note that is gone by the time it is tapped —
     * deleted outside the app, say — says so instead of opening empty.
     */
    fun openNote(note: NoteHeader, onLoaded: (Note) -> Unit) = viewModelScope.launch {
        val loaded = dao.note(note.relativePath)
        if (loaded == null) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_not_found))
            return@launch
        }
        onLoaded(loaded)
    }

    fun deleteFolder(noteFolder: NoteFolder) {
        appScope.launch {
            storageManager.deleteNoteFolder(noteFolder)
        }
    }


    fun defaultNewNote(): Note {

        val defaultName = query.value.let {
            if (NameValidation.check(it)) {
                it
            } else ""
        }

        val defaultExtension = FileExtension.match(prefs.defaultExtension.getBlocking())
        val defaultFullName = "$defaultName.${defaultExtension.text}"

        return Note.new(
            relativePath = "${currentNoteFolderRelativePath.value}/$defaultFullName",
        )
    }


    private data class GridQuery(
        val folderPath: String,
        val query: String,
    )

    /**
     * The whole list: the way out of the folder, its subfolders and its notes.
     * The folders ride along in the [PagingData] instead of being a list of
     * their own, so opening a folder swaps the list in one go.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val gridItems = combine(
        currentNoteFolderRelativePath,
        query,
    ) { folderPath, query ->
        GridQuery(folderPath, query)
    }.flatMapLatest { gridQuery ->

        val notes = Pager(
            config = PagingConfig(pageSize = 50),
            pagingSourceFactory = {
                if (gridQuery.query.isEmpty()) {
                    dao.gridNotes(gridQuery.folderPath, NOTE_SORT_ORDER)
                } else {
                    dao.gridNotesWithQuery(
                        gridQuery.folderPath,
                        NOTE_SORT_ORDER,
                        gridQuery.query
                    )
                }
            }
        ).flow.cachedIn(viewModelScope)

        combine(
            notes,
            dao.folders(gridQuery.folderPath, FOLDER_SORT_ORDER),
            selectedNotes,
        ) { pagingData, folders, selectedNotes ->

            var items: PagingData<GridItem> = pagingData.map { gridNote ->
                GridItem.Note(
                    gridNote.copy(
                        selected = selectedNotes.contains(gridNote.note)
                    )
                )
            }

            // every header goes to the very front, so the last one inserted wins
            folders.asReversed().forEach { folder ->
                items = items.insertHeaderItem(item = GridItem.Folder(folder))
            }

            if (gridQuery.folderPath.isNotEmpty()) {
                items = items.insertHeaderItem(
                    item = GridItem.ParentFolder(getParentPath(gridQuery.folderPath))
                )
            }

            items
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), PagingData.empty()
    )

    fun reloadDatabase() {
        appScope.launch {
            val res = storageManager.updateDatabase(force = true)
            res.onFailure {
                uiHelper.makeToast("$it")
            }
        }
    }
}
