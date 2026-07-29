package io.github.leonhardweiler.diffusion.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.map
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.data.room.NoteFolder
import io.github.leonhardweiler.diffusion.data.room.LIMIT_FILE_SIZE_DB
import io.github.leonhardweiler.diffusion.data.room.RepoDatabase
import io.github.leonhardweiler.diffusion.helper.NameValidation
import io.github.leonhardweiler.diffusion.manager.StorageManager
import io.github.leonhardweiler.diffusion.ui.model.FileExtension
import io.github.leonhardweiler.diffusion.ui.model.GridItem
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.SortOrder
import io.github.leonhardweiler.diffusion.utils.getParentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Whether there is anything written here that the remote has not seen. */
    val hasLocalChanges = storageManager.hasLocalChanges

    /** Commits what has been written since the last sync, then pulls and pushes. */
    fun syncWithRemote() {
        // Before the coroutine, not inside it: this is the one sync somebody is
        // watching, and the button has to have changed by the time the finger
        // is lifted. Everything the sync does before it reaches the network is
        // otherwise a button that looks like it was not pressed.
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

    /**
     * Folders can be selected alongside notes. Deleting one takes everything
     * inside it, which is what a folder row already did on its own — the
     * selection only makes it possible to do that to several at once.
     */
    val selectedFolders: StateFlow<List<NoteFolder>>
        get() = _selectedFolders.asStateFlow()

    /** How many rows are marked, notes and folders together. */
    val selectionSize: StateFlow<Int> =
        combine(selectedNotes, selectedFolders) { notes, folders -> notes.size + folders.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    init {
        Log.d(TAG, "init")
    }

    /** Drops from the selection whatever is no longer in the database. */
    private suspend fun refreshSelection() {
        _selectedNotes.emit(
            selectedNotes.value.filter { dao.isNoteExist(it.relativePath) }
        )

        val folders = dao.folderList(currentNoteFolderRelativePath.value, FOLDER_SORT_ORDER)
            .map { it.noteFolder }
        _selectedFolders.emit(selectedFolders.value.filter { folders.contains(it) })
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

    fun selectFolder(folder: NoteFolder, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedFolders.value.plus(folder)
        } else {
            selectedFolders.value.minus(folder)
        }.let {
            _selectedFolders.emit(it)
        }
    }

    /**
     * Everything the list is showing, which during a search is the results and
     * otherwise the folder being looked at with its subfolders. Not the way
     * out of the folder — ".." is not a thing that can be deleted.
     */
    fun selectAll() = viewModelScope.launch {
        val folderPath = currentNoteFolderRelativePath.value
        val currentQuery = query.value

        _selectedNotes.emit(
            dao.gridNoteList(folderPath, NOTE_SORT_ORDER, currentQuery).map { it.note }
        )

        // a search spans subfolders, so the folders of the one being looked at
        // are not among what it found — the list does not show them either
        _selectedFolders.emit(
            if (currentQuery.isEmpty()) {
                dao.folderList(folderPath, FOLDER_SORT_ORDER).map { it.noteFolder }
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

            // A note that stands in a folder that is going anyway is already
            // gone by the time its own turn comes, and deleting a file that is
            // not there says so out loud.
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

    /**
     * Reads the note behind a row of the list, which carries no content, and
     * hands it to the editor. A note that is gone by the time it is tapped —
     * deleted outside the app, say — says so instead of opening empty.
     *
     * So does one the index refused to read. The row is there either way, since
     * the list shows every file in the repository, but the editor is given what
     * the index holds — and for a file above [LIMIT_FILE_SIZE_DB] that is
     * nothing. Opening it would show an empty note, and the first save would
     * make the file agree with it.
     */
    fun openNote(note: NoteHeader, onLoaded: (Note) -> Unit) = viewModelScope.launch {
        val loaded = dao.note(note.relativePath)
        if (loaded == null) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_not_found))
            return@launch
        }

        val size = withContext(Dispatchers.IO) {
            runCatching { NodeFs.File.fromPath(prefs.repoPath(), note.relativePath).fileSize() }
                .getOrDefault(0L)
        }
        if (size > LIMIT_FILE_SIZE_DB) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_too_large, note.fileName))
            return@launch
        }

        onLoaded(loaded)
    }

    /**
     * Where a row's file actually is, for the one thing this app does not do
     * with it: hand it to another one.
     *
     * The path is put together here rather than in the row, because the row
     * knows the note only by its place in the repository and the repository
     * path is a preference — [onResolved] then runs on the main thread, where
     * starting an activity belongs.
     */
    fun openExternally(note: NoteHeader, onResolved: (String) -> Unit) = viewModelScope.launch {
        onResolved("${prefs.repoPath()}/${note.relativePath}")
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

    private val gridQuery: Flow<GridQuery> = combine(
        currentNoteFolderRelativePath,
        query,
    ) { folderPath, query ->
        GridQuery(folderPath, query)
    }

    /**
     * The notes of the folder being looked at, or what a search found.
     *
     * Nothing may be combined into this before [cachedIn]. A [PagingData] can
     * be collected exactly once, and combining means re-emitting the one that
     * arrived last whenever the other side changes — which hands cachedIn a
     * stream that has already been read, and that is an outright crash
     * ("Attempt to collect twice from pageEventFlow"). It was the selection
     * that used to be combined in here, so the app died on the second tap of a
     * multiple selection.
     *
     * After cachedIn it is safe, and that is the whole point of cachedIn: what
     * it holds is multicast, so a later collector — the one that comes back
     * from the editor, or the one below that folds the folder rows in — gets
     * the same pages again instead of an empty stream.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedNotes: Flow<PagingData<GridItem>> = gridQuery
        .flatMapLatest { gridQuery ->
            Pager(
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
            ).flow
        }
        .map { pagingData -> pagingData.map<GridNote, GridItem> { GridItem.Note(it) } }
        .cachedIn(viewModelScope)

    /**
     * The rows above the notes: the way out of the folder and its subfolders.
     *
     * A search shows neither. It reaches into the subfolders, so the folders of
     * the one being looked at are not what was asked for — and the way out of
     * it would sit above results that come from inside it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val folderRows: Flow<List<GridItem>> = gridQuery.flatMapLatest { gridQuery ->
        if (gridQuery.query.isNotEmpty()) return@flatMapLatest flowOf(emptyList())

        dao.folders(gridQuery.folderPath, FOLDER_SORT_ORDER).map { folders ->
            buildList {
                if (gridQuery.folderPath.isNotEmpty()) {
                    add(GridItem.ParentFolder(getParentPath(gridQuery.folderPath)))
                }
                folders.forEach { add(GridItem.Folder(it)) }
            }
        }
    }

    /**
     * The whole list: the way out of the folder, its subfolders and its notes.
     * The folders ride along in the [PagingData] instead of being a list of
     * their own, so opening a folder swaps the list in one go rather than
     * rebuilding the layout twice.
     */
    val gridItems: Flow<PagingData<GridItem>> =
        combine(pagedNotes, folderRows) { pagingData, headers ->
            var items = pagingData

            // every header goes to the very front, so the last one inserted wins
            headers.asReversed().forEach { header ->
                items = items.insertHeaderItem(item = header)
            }

            items
        }

    /**
     * Reads the files back into the database, which catches whatever changed
     * outside the app. The remote is not part of it — syncing is the button in
     * the search bar and nothing else.
     */
    fun reloadDatabase() {
        appScope.launch {
            storageManager.updateDatabase(force = true).onFailure {
                uiHelper.makeToast("$it")
            }
            refreshSelection()
        }
    }
}
