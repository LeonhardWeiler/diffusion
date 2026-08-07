package io.github.leonhardweiler.diffusion.data.index

import android.os.SystemClock
import android.util.Log
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.helper.movedUnder
import io.github.leonhardweiler.diffusion.manager.Progress
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "NoteIndex"

/** Above this a file is listed, is not looked into and does not open here. */
const val LIMIT_FILE_SIZE = 2 * 1024 * 1024

/**
 * A snapshot, never written into: every change swaps it for a new one, so
 * whoever is drawing a list is drawing one that agrees with itself. [reads]
 * counts the reads of the whole repository, which is the only thing that says
 * a read happened — a write of this app's own goes in one row at a time.
 */
data class IndexState(
    val rootPath: String = "",
    val notes: Map<String, NoteHeader> = emptyMap(),
    val folders: Map<String, NoteFolder> = emptyMap(),
    val reads: Int = 0,
)

class NoteIndex {
    private val _state = MutableStateFlow(IndexState())
    val state: StateFlow<IndexState> = _state

    suspend fun rebuild(rootPath: String, progressCb: ((Progress) -> Unit)? = null) {
        val startedAt = SystemClock.elapsedRealtime()

        val notes = HashMap<String, NoteHeader>()
        val folders = HashMap<String, NoteFolder>()

        val rootFs = NodeFs.Folder.fromPath(rootPath)
        folders[""] = NoteFolder.new(relativePath = "")

        val rootLength = rootFs.path.length + 1

        suspend fun readFolder(folder: NodeFs.Folder) {
            folder.forEachNodeFs { nodeFs ->

                // skipping hidden files is what keeps the .gitkeep of every
                // empty folder out of the list
                if (nodeFs.isHidden() || nodeFs.isSym()) return@forEachNodeFs

                val relativePath = nodeFs.path.substring(startIndex = rootLength)

                when (nodeFs) {
                    is NodeFs.File -> {
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

        _state.value = IndexState(
            rootPath = rootPath,
            notes = notes,
            folders = folders,
            reads = _state.value.reads + 1,
        )

        Log.i(
            TAG,
            "rebuild: ${notes.size} files in ${folders.size} folders, " +
                    "${SystemClock.elapsedRealtime() - startedAt} ms"
        )
    }

    fun clear() {
        _state.value = IndexState()
    }

    fun hasNote(relativePath: String): Boolean = _state.value.notes.containsKey(relativePath)

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

    fun putNote(note: Note) = update { state ->
        state.copy(
            notes = state.notes + (note.relativePath to NoteHeader(
                relativePath = note.relativePath,
                lastModifiedTimeMillis = note.lastModifiedTimeMillis,
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

    /** The notes under it, the rows of its subfolders, and then the folder. */
    fun removeFolders(noteFolders: List<NoteFolder>) = update { state ->
        val prefixes = noteFolders.map { it.relativePath }

        state.copy(
            notes = state.notes.filterKeys { path -> prefixes.none { path.isUnder(it) } },
            folders = state.folders.filterKeys { path ->
                prefixes.none { path == it || path.isUnder(it) }
            },
        )
    }

    fun moveFolder(noteFolder: NoteFolder, newRelativePath: String) = update { state ->
        val oldPath = noteFolder.relativePath

        fun moved(path: String) = movedUnder(path, oldPath, newRelativePath)

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

internal fun String.isUnder(folderPath: String): Boolean =
    if (folderPath.isEmpty()) isNotEmpty() else startsWith("$folderPath/")

private fun NoteHeader.at(relativePath: String) = NoteHeader(
    relativePath = relativePath,
    lastModifiedTimeMillis = lastModifiedTimeMillis,
    id = id,
    fileName = relativePath.substringAfterLast("/"),
)

/**
 * The dates as they were when the list was last put in order. The date a row
 * shows is current, the place it stands is not: a save reaches the list at once
 * but must not take the row with it, or the note somebody is about to tap jumps
 * to the top while they look at it. By id, so a rename keeps its place; a note
 * the snapshot has never seen stands where its own date puts it.
 */
fun IndexState.sortDatesNow(): Map<Int, Long> =
    notes.values.associate { it.id to it.lastModifiedTimeMillis }

private fun NoteHeader.sortDate(sortDates: Map<Int, Long>): Long =
    sortDates[id] ?: lastModifiedTimeMillis

fun IndexState.notesIn(
    folderPath: String,
    sortDates: Map<Int, Long> = emptyMap()
): List<NoteHeader> =
    notes.values
        .filter { it.parentPath() == folderPath }
        .sortedWith(byDate(sortDates))

fun IndexState.foldersIn(
    folderPath: String,
    sortDates: Map<Int, Long> = emptyMap()
): List<FolderModel> =
    folders.values
        .filter { it.relativePath.isNotEmpty() && parentOf(it.relativePath) == folderPath }
        .map { folder ->
            var count = 0
            var newest = 0L

            notes.values.forEach { note ->
                if (!note.relativePath.isUnder(folder.relativePath)) return@forEach

                count++
                newest = maxOf(newest, note.sortDate(sortDates))
            }

            FolderModel(
                noteFolder = folder,
                noteCount = count,
                lastModifiedTimeMillis = newest,
            )
        }
        .sortedWith(byFolderDate)

suspend fun IndexState.search(
    folderPath: String,
    query: String,
    sortDates: Map<Int, Long> = emptyMap()
): List<NoteHeader> {
    val needle = query.trim()
    if (needle.isEmpty()) return notesIn(folderPath, sortDates)

    val under = notes.values
        .filter { folderPath.isEmpty() || it.relativePath.isUnder(folderPath) }
        .sortedWith(byDate(sortDates))

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

private fun byDate(sortDates: Map<Int, Long>) =
    compareByDescending<NoteHeader> { it.sortDate(sortDates) }.thenBy { it.relativePath }

private val byFolderDate = compareByDescending<FolderModel> { it.lastModifiedTimeMillis }
    .thenBy { it.noteFolder.relativePath }

private fun NoteHeader.parentPath(): String = parentOf(relativePath)

private fun parentOf(path: String): String =
    path.substringBeforeLast("/", missingDelimiterValue = "")
