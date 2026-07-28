package io.github.wiiznokes.gitnote.ui.model

import androidx.room.Embedded
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder

enum class SortOrder {
    AZ,
    ZA,
    MostRecent,
    Oldest,
}

data class FolderModel(
    @Embedded
    val noteFolder: NoteFolder,
    val noteCount: Int,
)

data class GridNote(
    @Embedded
    val note: Note,
    val isUnique: Boolean,
    val selected: Boolean = false,
)

/**
 * One row of the note list. Folders and notes come from two different queries
 * that answer at their own pace, so they are merged into a single paged list:
 * that way the list changes once when a folder is opened, not once per query.
 */
sealed interface GridItem {

    data class ParentFolder(val relativePath: String) : GridItem

    data class Folder(val folder: FolderModel) : GridItem

    data class Note(val gridNote: GridNote) : GridItem

    /** Stable across the two kinds of row, which have their own id spaces. */
    fun key(): String = when (this) {
        is ParentFolder -> ".."
        is Folder -> "f${folder.noteFolder.id}"
        is Note -> "n${gridNote.note.id}"
    }
}
