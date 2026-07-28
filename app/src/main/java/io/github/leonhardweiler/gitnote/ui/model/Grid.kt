package io.github.leonhardweiler.gitnote.ui.model

import androidx.room.Embedded
import io.github.leonhardweiler.gitnote.data.room.NoteFolder

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

/**
 * A note as the list knows it. Deliberately without the content: the list shows
 * a name and a date, and a note that is only listed should not carry its whole
 * text through every loaded page. The editor reads the note itself when it
 * opens one.
 */
data class NoteHeader(
    val relativePath: String,
    val lastModifiedTimeMillis: Long,
    val id: Int,
    val fileName: String,
) {
    fun nameWithoutExtension(): String =
        fileName.substringBeforeLast(".", missingDelimiterValue = fileName)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as NoteHeader).id
    }

    override fun hashCode(): Int = id
}

data class GridNote(
    @Embedded
    val note: NoteHeader,
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
