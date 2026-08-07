package io.github.leonhardweiler.diffusion.ui.model

import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported

data class FolderModel(
    val noteFolder: NoteFolder,
    val noteCount: Int,
    val lastModifiedTimeMillis: Long,
)

data class NoteHeader(
    val relativePath: String,
    val lastModifiedTimeMillis: Long,
    val id: Int,
    val fileName: String,
) {
    fun nameWithoutExtension(): String =
        fileName.substringBeforeLast(".", missingDelimiterValue = fileName)

    fun extension(): String = fileName.substringAfterLast(".", missingDelimiterValue = "")

    fun isNote(): Boolean = isExtensionSupported(extension())
}

// A row is equal to another one when it says the same thing, so NoteHeader and
// NoteFolder stay plain data classes: comparing by id alone made the rebuilt
// list equal to the one before it, the StateFlow dropped it as unchanged, and
// a renamed note kept its old name on screen. Selection still asks by id —
// which is what these four are for, never contains/minus.
fun List<NoteHeader>.holds(note: NoteHeader): Boolean = any { it.id == note.id }

fun List<NoteHeader>.without(note: NoteHeader): List<NoteHeader> =
    filterNot { it.id == note.id }

fun List<NoteFolder>.holds(folder: NoteFolder): Boolean = any { it.id == folder.id }

fun List<NoteFolder>.without(folder: NoteFolder): List<NoteFolder> =
    filterNot { it.id == folder.id }

data class GridNote(
    val note: NoteHeader,
    val isUnique: Boolean,
)

sealed interface GridItem {
    data class ParentFolder(val relativePath: String) : GridItem

    data class Folder(val folder: FolderModel) : GridItem

    data class Note(val gridNote: GridNote) : GridItem

    fun key(): String = when (this) {
        is ParentFolder -> ".."
        is Folder -> "f${folder.noteFolder.id}"
        is Note -> "n${gridNote.note.id}"
    }
}
