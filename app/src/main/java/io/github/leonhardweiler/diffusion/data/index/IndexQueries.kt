package io.github.leonhardweiler.diffusion.data.index

import android.util.Log
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val TAG = "IndexQueries"

/**
 * The dates as they were when the list was last put in order. The date a row
 * shows is current, the place it stands is not: a save reaches the list at once
 * but must not take the row with it, or the note somebody is about to tap jumps
 * to the top while they look at it. By id, so a rename keeps its place; a note
 * the snapshot has never seen stands where its own date puts it.
 */
fun IndexState.sortDatesNow(): Map<Int, Long> =
    notes.values.associate { it.id to it.lastModifiedTimeMillis }

fun IndexState.notesIn(
    folderPath: String,
    sortDates: Map<Int, Long> = emptyMap()
): List<NoteHeader> =
    notes.values
        .filter { it.parentPath() == folderPath }
        .sortedWith(byDate(sortDates))

/** A folder has no date of its own and takes the newest note under it. */
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

/** The one thing here that spans subfolders, and the one that reads files. */
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

private fun NoteHeader.sortDate(sortDates: Map<Int, Long>): Long =
    sortDates[id] ?: lastModifiedTimeMillis

// two notes can carry the same date — a clone stamps whole folders with the
// commit they came from — so path decides, and the same folder read twice
// reads the same way
private fun byDate(sortDates: Map<Int, Long>) =
    compareByDescending<NoteHeader> { it.sortDate(sortDates) }.thenBy { it.relativePath }

private val byFolderDate = compareByDescending<FolderModel> { it.lastModifiedTimeMillis }
    .thenBy { it.noteFolder.relativePath }

internal fun String.isUnder(folderPath: String): Boolean =
    if (folderPath.isEmpty()) isNotEmpty() else startsWith("$folderPath/")

private fun NoteHeader.parentPath(): String = parentOf(relativePath)

private fun parentOf(path: String): String =
    path.substringBeforeLast("/", missingDelimiterValue = "")
