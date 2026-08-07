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
 *
 * **Two of these are equal when they say the same thing**, which is what a data
 * class means and what the list depends on. It used to compare by [id] alone —
 * a note that is renamed or written again keeps its id, so the row that
 * replaced it was equal to the one before it, the [kotlinx.coroutines.flow.StateFlow]
 * of list rows dropped it as an unchanged value, and the screen kept the old
 * name and the old date. Renaming `testfile` to `testfile.md` left a row that
 * still said `testfile` and answered a tap with "this note is no longer there".
 *
 * The id is still what a note *is* — it survives a rename, keys the rows and
 * says which of them are marked (see [holds]) — it is just not what two rows
 * are compared by.
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
}

/**
 * Whether this note is one of the marked ones.
 *
 * By id, not by value: the selection is about which notes were tapped, and a row
 * is handed a new [NoteHeader] whenever anything about the note changes — a save
 * moves its date, a rename its path. Compared by value, a note that was written
 * while it stood marked would have lost its mark.
 */
fun List<NoteHeader>.holds(note: NoteHeader): Boolean = any { it.id == note.id }

/** The selection without that note, found the way [holds] finds it. */
fun List<NoteHeader>.without(note: NoteHeader): List<NoteHeader> =
    filterNot { it.id == note.id }

/** [holds], for the folders of a selection. */
fun List<NoteFolder>.holds(folder: NoteFolder): Boolean = any { it.id == folder.id }

/** [without], for the folders of a selection. */
fun List<NoteFolder>.without(folder: NoteFolder): List<NoteFolder> =
    filterNot { it.id == folder.id }

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
