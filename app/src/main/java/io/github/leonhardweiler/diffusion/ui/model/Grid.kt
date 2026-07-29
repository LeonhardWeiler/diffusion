package io.github.leonhardweiler.diffusion.ui.model

import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.manager.extensionType
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported

/**
 * A folder as a row of the list: what it is called, and how many notes stand
 * under it — which is what deleting it would take with it.
 */
data class FolderModel(
    val noteFolder: NoteFolder,
    val noteCount: Int,
    /**
     * When the most recently written note under it was written, which is what a
     * folder is sorted by. Zero for one with nothing in it: a folder has no date
     * of its own, and there is nothing in it to speak for one.
     */
    val lastModifiedTimeMillis: Long,
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

    fun extension(): String = fileName.substringAfterLast(".", missingDelimiterValue = "")

    /**
     * Whether this is a file the app itself can show. Everything in the
     * repository is listed, so a row can just as well be a photo or a pdf —
     * tapping one of those hands it to another app instead of opening an
     * editor. The answer is remembered per extension, see [extensionType].
     */
    fun isNote(): Boolean = isExtensionSupported(extension())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as NoteHeader).id
    }

    override fun hashCode(): Int = id
}

/**
 * A note plus the one thing the row needs that the note itself cannot say:
 * whether its file name is enough to tell it apart from the others.
 *
 * Whether it is selected is deliberately not in here. It used to be, folded in
 * by combining the selection into the paged list — and a PagingData may be
 * collected exactly once, so every change of the selection re-wrapped a stream
 * that had already been read and the app died on the second tap. The row asks
 * the selection itself now.
 */
data class GridNote(
    val note: NoteHeader,
    val isUnique: Boolean,
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
