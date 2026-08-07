package io.github.leonhardweiler.diffusion.ui.viewmodel

import io.github.leonhardweiler.diffusion.data.index.IndexState
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import io.github.leonhardweiler.diffusion.data.index.notesIn
import io.github.leonhardweiler.diffusion.data.index.search
import io.github.leonhardweiler.diffusion.helper.getParentPath
import io.github.leonhardweiler.diffusion.ui.model.GridItem
import io.github.leonhardweiler.diffusion.ui.model.GridNote

/** The four things the list is built from, so that one flow carries them. */
internal data class ListInput(
    val state: IndexState,
    val folderPath: String,
    val query: String,
    val sortDates: Map<Int, Long>,
)

/**
 * The rows of the list: the `..` row and the folders first, the notes after
 * them. A search shows no folder rows at all — it spans subfolders, so they are
 * not what was asked for — and names its results by their full path, which is
 * what [GridNote.isUnique] carries.
 */
internal suspend fun ListInput.gridItems(): List<GridItem> {
    val notes = if (query.isEmpty()) {
        state.notesIn(folderPath, sortDates)
    } else {
        state.search(folderPath, query, sortDates)
    }

    val duplicated = notes
        .groupingBy { it.fileName }
        .eachCount()

    return buildList {
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
