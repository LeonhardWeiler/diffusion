package io.github.leonhardweiler.diffusion.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
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


    private val storageManager: StorageManager = MyApp.appModule.storageManager
    private val appScope = MyApp.appModule.appScope

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val index = MyApp.appModule.noteIndex
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


    /**
     * The dates the list is ordered by, which are the dates the rows carried
     * when it was last put in order. See [sortDatesNow] and [resort].
     */
    private val _sortDates: MutableStateFlow<Map<Int, Long>> = MutableStateFlow(emptyMap())

    init {
        Log.d(TAG, "init")

        // A read of the whole repository is a moment the list may be put in
        // order again: the start, a pull, the reload row of the settings. The
        // first value that arrives here is the read the app started with.
        viewModelScope.launch {
            index.state
                .map { it.reads }
                .distinctUntilChanged()
                .collect { resort() }
        }
    }

    /**
     * Puts the list in order again, and only ever at a moment nobody can see it
     * happen — a folder opened or left, a search begun or ended, the app closed,
     * the repository read again.
     *
     * A note that is written moves to the top of its folder, and doing that
     * while the list is on screen means the row somebody came back to has gone
     * somewhere else. So a write changes the date a row shows and leaves the row
     * where it is until one of those moments, which is what this is.
     */
    fun resort() {
        _sortDates.value = index.state.value.sortDatesNow()
    }

    fun search(query: String) {
        // Beginning a search and ending one both replace the whole list, so
        // nobody can tell the rows that stayed from the ones that moved. Not on
        // every letter: what is typed after the first one only narrows what is
        // already there, and that is a list being looked at like any other.
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
        // the folder being left is not the folder being opened, so its order is
        // nothing anybody is about to compare against
        resort()

        viewModelScope.launch {
            _currentNoteFolderRelativePath.emit(relativePath)
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        // the same words a rename is refused in, for the same reasons
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

        if (noteFolder.toFolderFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_already_exist, name))
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
            // by id, not by value: what is held here is the note as it was when
            // it was tapped, and a save since then has moved its date
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

    /**
     * Everything the list is showing, which during a search is the results and
     * otherwise the folder being looked at with its subfolders. Not the way
     * out of the folder — ".." is not a thing that can be deleted.
     */
    fun selectAll() = viewModelScope.launch {
        val folderPath = currentNoteFolderRelativePath.value
        val currentQuery = query.value

        val state = index.state.value

        _selectedNotes.emit(
            withContext(Dispatchers.IO) { state.search(folderPath, currentQuery) }
        )

        // a search spans subfolders, so the folders of the one being looked at
        // are not among what it found — the list does not show them either
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
     * the index holds — and for a file above [LIMIT_FILE_SIZE] that is nothing.
     * Opening it would show an empty note, and the first save would make the
     * file agree with it.
     */
    fun openNote(note: NoteHeader, onLoaded: (Note) -> Unit) = viewModelScope.launch {
        val loaded = withContext(Dispatchers.IO) { index.loadNote(note.relativePath) }
        if (loaded == null) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_not_found))
            return@launch
        }

        val size = withContext(Dispatchers.IO) {
            runCatching { NodeFs.File.fromPath(prefs.repoPath(), note.relativePath).fileSize() }
                .getOrDefault(0L)
        }
        if (size > LIMIT_FILE_SIZE) {
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

    /**
     * Renames a note, or moves it — the typed text is a path, read exactly the
     * way a folder's is: `notes.md` renames it in place, `../notes.md` puts it a
     * folder up, `/notes.md` at the root. A last segment with no dot in it keeps
     * the extension the note has, so only somebody who types one changes what
     * the file is.
     *
     * This is where a note is renamed. The name above an open note is what the
     * note is called and not a field to type in — a rename is one act, and the
     * editor's field was one that happened somewhere in the middle of the next
     * save.
     */
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
            // the row of the list carries no text, and the move writes the row
            // again on the other side with everything it had
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

    /**
     * Renames a folder, or moves it — the typed text is a path, read the same
     * way the editor reads the one above a note: `archive` renames it in place,
     * `../archive` puts it a folder up, `/archive` at the root. Everything under
     * it comes along.
     */
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


    /**
     * What the name of a new note starts out as: whatever is in the search field,
     * when that is something a file can be called, and otherwise nothing at all.
     * Somebody searching for a note that is not there is somebody about to write
     * it.
     *
     * No extension. There was a preference for which one to append, and what it
     * did was put `.md` in the field before anybody had typed a name — so the
     * name was typed in front of an extension, and the one thing a note's name
     * says about it was a setting somewhere else. What is typed is what the file
     * is called, extension or none, and a name with no dot in it is a text note
     * (see extensionType).
     */
    fun defaultNewNoteName(): String =
        query.value.let { if (NameValidation.check(it)) it else "" }

    /**
     * Writes an empty note where the typed name says, and leaves it in the list.
     *
     * Creating a note does not open it. It used to open the editor on a note that
     * had no file yet, with the name to type in above it — so a note existed only
     * once something had been typed into it, the name was being edited halfway
     * through every save, and leaving the screen early left nothing behind. The
     * name is asked for here, the file is written here, and the note is a row of
     * the list like any other: opened by tapping it, renamed from its own menu.
     *
     * The typed text is a path, read exactly the way a rename reads one, and the
     * folder it names has to exist already — the way `mv` wants it.
     *
     * @return whether the dialog that asked can close.
     */
    fun createNote(typed: String): Boolean {
        val resolved = resolveRepoPath(currentNoteFolderRelativePath.value, typed.trim())
        if (resolved !is ResolvedPath.Ok) {
            uiHelper.makeToast((resolved as ResolvedPath.Bad).problem.describe(uiHelper))
            return false
        }

        val note = Note.new(relativePath = resolved.relativePath)
        val repoPath = prefs.repoPathBlocking()

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


    /**
     * The whole list: the way out of the folder, its subfolders and its notes.
     *
     * One list from one place. It was a paged query and a second flow of folder
     * rows folded into it, which is what a database bought and also what it
     * cost — a [androidx.paging.PagingData] may be collected exactly once, and
     * everything that wanted to know about the list had to be careful not to be
     * the second reader.
     *
     * The search reads the files, so it runs off the main thread and the one
     * before it is cancelled the moment another letter is typed. Everything
     * else is a filter over what is already in memory.
     */
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

                // A name that appears once is enough to tell a row by; the
                // others say where they are. Asked of what is being shown, not
                // of the whole repository — two notes called the same in two
                // folders are only worth telling apart when both are listed.
                val duplicated = notes
                    .groupingBy { it.fileName }
                    .eachCount()

                buildList {
                    // A search shows neither the way out of the folder nor its
                    // subfolders: it reaches into them, so they are not what was
                    // asked for, and the way out would sit above results that
                    // come from inside.
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

/**
 * The four things the list is built from, carried through [Flow.combine] as one
 * value — Kotlin has a Pair and a Triple and nothing after that.
 */
private data class ListInput(
    val state: IndexState,
    val folderPath: String,
    val query: String,
    val sortDates: Map<Int, Long>,
)
