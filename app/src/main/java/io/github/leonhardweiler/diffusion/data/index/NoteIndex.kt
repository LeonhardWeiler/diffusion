package io.github.leonhardweiler.diffusion.data.index

import android.os.SystemClock
import android.util.Log
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.manager.Progress
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "NoteIndex"

/**
 * The largest file whose text a search reads — and, because the editor refuses
 * what a search will not look into, the largest note that can be opened here.
 */
const val LIMIT_FILE_SIZE = 2 * 1024 * 1024

/**
 * The repository as the list knows it: every file and every folder in it, by
 * name and date, and not a byte of what any of them says.
 *
 * A snapshot rather than something to be written into. Every change replaces it
 * whole, so whoever is drawing a list is drawing one that agrees with itself.
 */
data class IndexState(
    val rootPath: String = "",
    /** By relative path, which is what everything else names a note by. */
    val notes: Map<String, NoteHeader> = emptyMap(),
    /** The same, and the root folder — the empty path — is one of them. */
    val folders: Map<String, NoteFolder> = emptyMap(),
)

/**
 * What the note list, the search and the editor read, held in memory and built
 * from the files.
 *
 * There was a database here — Room, an FTS4 table and a ranking function over
 * its match info — and the files were mirrored into it so that the list and the
 * search could be answered in SQL. The mirror is what cost: a commit had to be
 * remembered to know whether the rows still described the files, every write
 * had to keep two things in step, and a rebuild had to be triggered by whoever
 * might have touched the repository from outside.
 *
 * What it bought was a list that was there before the files had been read. That
 * read takes a few hundred milliseconds on a real repository on a real phone,
 * which is a wait nobody sees behind a screen that is coming up anyway — so it
 * happens at every start now, and there is one place that knows what a note is.
 *
 * Every method here is called under the storage manager's lock, so nothing
 * synchronises: what is shared is [state], and that is only ever swapped for a
 * new snapshot.
 */
class NoteIndex {

    private val _state = MutableStateFlow(IndexState())
    val state: StateFlow<IndexState> = _state

    /**
     * Reads the whole repository, which is the only thing that says what is in
     * it.
     *
     * Hidden files and symlinks are left out: the `.gitkeep` of every empty
     * folder is one this app writes, and `.gitignore` is not something anybody
     * came here to read.
     */
    suspend fun rebuild(rootPath: String, progressCb: ((Progress) -> Unit)? = null) {
        val startedAt = SystemClock.elapsedRealtime()

        val notes = HashMap<String, NoteHeader>()
        val folders = HashMap<String, NoteFolder>()

        val rootFs = NodeFs.Folder.fromPath(rootPath)
        folders[""] = NoteFolder.new(relativePath = "")

        val rootLength = rootFs.path.length + 1

        suspend fun readFolder(folder: NodeFs.Folder) {
            folder.forEachNodeFs { nodeFs ->

                if (nodeFs.isHidden() || nodeFs.isSym()) return@forEachNodeFs

                val relativePath = nodeFs.path.substring(startIndex = rootLength)

                when (nodeFs) {
                    is NodeFs.File -> {
                        // The file's own date, and nothing else. It is the only
                        // one that is right for a note that was written and not
                        // committed yet, and a checkout hands the others theirs
                        // (see applyCommitTimestamps) rather than the moment it
                        // ran — so the commit history has nothing to add here.
                        notes[relativePath] = NoteHeader(
                            relativePath = relativePath,
                            lastModifiedTimeMillis = nodeFs.lastModifiedTime().toMillis(),
                            id = generateUid(),
                            fileName = nodeFs.fullName,
                        )
                    }

                    is NodeFs.Folder -> {
                        folders[relativePath] = NoteFolder.new(relativePath = relativePath)
                        progressCb?.invoke(Progress.ReadingRepo(relativePath))
                        readFolder(nodeFs)
                    }
                }
            }
        }

        readFolder(rootFs)

        _state.value = IndexState(rootPath = rootPath, notes = notes, folders = folders)

        Log.i(
            TAG,
            "rebuild: ${notes.size} files in ${folders.size} folders, " +
                    "${SystemClock.elapsedRealtime() - startedAt} ms"
        )
    }

    /** Everything goes with the repository it was read from. */
    fun clear() {
        _state.value = IndexState()
    }

    fun hasNote(relativePath: String): Boolean = _state.value.notes.containsKey(relativePath)

    /**
     * The note behind a row, text and all, read from its file.
     *
     * The row is what the index holds; the text is on disk and nowhere else,
     * which is the point of not keeping it here. Null for a note that is no
     * longer there — deleted outside the app, say.
     */
    fun loadNote(relativePath: String): Note? {
        val state = _state.value
        val header = state.notes[relativePath] ?: return null

        val file = NodeFs.File.fromPath(state.rootPath, relativePath)
        if (!file.exist()) return null

        return Note(
            relativePath = relativePath,
            content = if (file.fileSize() > LIMIT_FILE_SIZE) "" else file.readText(),
            lastModifiedTimeMillis = header.lastModifiedTimeMillis,
            id = header.id,
        )
    }

    /** Puts a note in, or writes over the one that is there under that path. */
    fun putNote(note: Note) = update { state ->
        state.copy(
            notes = state.notes + (note.relativePath to NoteHeader(
                relativePath = note.relativePath,
                lastModifiedTimeMillis = note.lastModifiedTimeMillis,
                // a note keeps its id across a save: the row keeps its place and
                // the undo history stays with it
                id = note.id,
                fileName = note.fileName,
            ))
        )
    }

    fun removeNoteAt(relativePath: String) = update { state ->
        state.copy(notes = state.notes - relativePath)
    }

    fun removeNotesAt(relativePaths: List<String>) = update { state ->
        state.copy(notes = state.notes - relativePaths.toSet())
    }

    /**
     * The same note under a different path, with the id and the date it had —
     * the file was moved rather than written somewhere else, and so is this.
     */
    fun moveNote(oldRelativePath: String, note: Note) = update { state ->
        state.copy(
            notes = state.notes - oldRelativePath + (note.relativePath to NoteHeader(
                relativePath = note.relativePath,
                lastModifiedTimeMillis = note.lastModifiedTimeMillis,
                id = note.id,
                fileName = note.fileName,
            ))
        )
    }

    fun putFolder(noteFolder: NoteFolder) = update { state ->
        state.copy(folders = state.folders + (noteFolder.relativePath to noteFolder))
    }

    /**
     * A folder and everything under it, which is what deleting one does on disk.
     *
     * The subfolders matter: one left behind is a row in the list that opens
     * onto nothing.
     */
    fun removeFolders(noteFolders: List<NoteFolder>) = update { state ->
        val prefixes = noteFolders.map { it.relativePath }

        state.copy(
            notes = state.notes.filterKeys { path -> prefixes.none { path.isUnder(it) } },
            folders = state.folders.filterKeys { path ->
                prefixes.none { path == it || path.isUnder(it) }
            },
        )
    }

    /**
     * A folder moved to where [newRelativePath] says, with everything under it.
     *
     * One rename of the directory on disk, and here the paths rewritten. The
     * ids come along, so the list keeps its place and an open undo history
     * stays with its note.
     */
    fun moveFolder(noteFolder: NoteFolder, newRelativePath: String) = update { state ->
        val oldPath = noteFolder.relativePath

        fun moved(path: String) = newRelativePath + path.substring(oldPath.length)

        val notes = state.notes.mapKeys { (path, _) ->
            if (path.isUnder(oldPath)) moved(path) else path
        }.mapValues { (path, header) ->
            if (path == header.relativePath) header else header.at(path)
        }

        val folders = state.folders.mapKeys { (path, _) ->
            if (path == oldPath || path.isUnder(oldPath)) moved(path) else path
        }.mapValues { (path, folder) ->
            if (path == folder.relativePath) folder else folder.copy(relativePath = path)
        }

        state.copy(notes = notes, folders = folders)
    }

    private fun update(f: (IndexState) -> IndexState) {
        _state.value = f(_state.value)
    }
}

/** Whether this path stands inside [folderPath], however deep. */
internal fun String.isUnder(folderPath: String): Boolean =
    if (folderPath.isEmpty()) isNotEmpty() else startsWith("$folderPath/")

/** The same row under a new path, name derived again rather than kept. */
private fun NoteHeader.at(relativePath: String) = NoteHeader(
    relativePath = relativePath,
    lastModifiedTimeMillis = lastModifiedTimeMillis,
    id = id,
    fileName = relativePath.substringAfterLast("/"),
)

/**
 * The notes of one folder, in alphabetical order.
 *
 * Not recursive: a folder is a row of its own and opening it is what shows what
 * is inside. Only the search reaches further.
 */
fun IndexState.notesIn(folderPath: String): List<NoteHeader> =
    notes.values
        .filter { it.parentPath() == folderPath }
        .sortedWith(byName)

/**
 * The subfolders of one folder, in alphabetical order, each with how many notes
 * stand under it — its own and those of everything inside it, which is what
 * deleting it would take.
 */
fun IndexState.foldersIn(folderPath: String): List<FolderModel> =
    folders.values
        .filter { it.relativePath.isNotEmpty() && parentOf(it.relativePath) == folderPath }
        .sortedBy { it.fullName().lowercase() }
        .map { folder ->
            FolderModel(
                noteFolder = folder,
                noteCount = notes.keys.count { it.isUnder(folder.relativePath) },
            )
        }

/**
 * What a search finds: the notes under the folder being looked at whose name or
 * whose text holds what was typed, in alphabetical order.
 *
 * There is no ranking anymore. It was a function over FTS4 match info that put
 * a hit in the name above a hit in the text, and the whole of what it bought
 * was an order nobody could predict. A name is a name and the list is sorted.
 *
 * The text is read from the files, one after the other, and only for what the
 * app can read at all: a jpeg is not text, and a file above [LIMIT_FILE_SIZE]
 * is not something to pull into memory for a substring. Suspending on purpose —
 * every keystroke starts this again and the one before it is cancelled.
 */
suspend fun IndexState.search(folderPath: String, query: String): List<NoteHeader> {
    val needle = query.trim()
    if (needle.isEmpty()) return notesIn(folderPath)

    val under = notes.values
        .filter { folderPath.isEmpty() || it.relativePath.isUnder(folderPath) }
        .sortedWith(byName)

    val found = mutableListOf<NoteHeader>()

    for (header in under) {
        currentCoroutineContext().ensureActive()

        if (header.fileName.contains(needle, ignoreCase = true) ||
            header.holdsText(rootPath, needle)
        ) {
            found += header
        }
    }

    return found
}

/** Whether the file behind a row says [needle] anywhere, when it can be read. */
private fun NoteHeader.holdsText(rootPath: String, needle: String): Boolean {
    if (!isExtensionSupported(extension())) return false

    val file = NodeFs.File.fromPath(rootPath, relativePath)

    return runCatching {
        if (file.fileSize() > LIMIT_FILE_SIZE) return false
        file.readText().contains(needle, ignoreCase = true)
    }.getOrElse {
        Log.d(TAG, "could not read ${file.path}: ${it.message}")
        false
    }
}

/**
 * Alphabetically, and by path where two files are called the same — a list that
 * changes order between two reads of the same folder is worse than one whose
 * order is only nearly the one expected.
 */
private val byName = compareBy<NoteHeader>({ it.fileName.lowercase() }, { it.relativePath })

private fun NoteHeader.parentPath(): String = parentOf(relativePath)

private fun parentOf(path: String): String =
    path.substringBeforeLast("/", missingDelimiterValue = "")
