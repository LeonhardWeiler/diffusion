package io.github.wiiznokes.gitnote.ui.model

import androidx.room.Embedded
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder

enum class SortOrder {
    AZ,
    ZA,
    MostRecent,
    Oldest;

    override fun toString(): String {
        val res = when (this) {
            AZ -> R.string.az_sort_order
            ZA -> R.string.za_sort_order
            MostRecent -> R.string.most_recent_sort_order
            Oldest -> R.string.oldest_sort_order
        }
        return MyApp.appModule.uiHelper.getString(res)
    }
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
