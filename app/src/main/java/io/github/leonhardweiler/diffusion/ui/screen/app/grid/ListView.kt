package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.data.room.NoteFolder
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.model.GridItem
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel
import java.text.DateFormat

/**
 * The note list: the way out of the folder, its subfolders and its notes, all
 * of them rows of the one list. The rows themselves are in ListRows.kt.
 */
@Composable
internal fun NoteListView(
    gridItems: LazyPagingItems<GridItem>,
    topSpacerHeight: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    selectedNotes: List<NoteHeader>,
    selectedFolders: List<NoteFolder>,
    onEditClick: (Note, EditType) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderDelete: (NoteFolder) -> Unit,
    isSearching: Boolean,
    vm: GridViewModel,
) {

    /** While anything is marked, a tap marks rather than opens. */
    val isSelecting = selectedNotes.isNotEmpty() || selectedFolders.isNotEmpty()

    // one formatter for the whole list rather than one per row
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        item {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        items(
            count = gridItems.itemCount,
            key = gridItems.itemKey { it.key() },
            // folders and notes lay out differently; telling them apart lets a
            // scrolled row be reused instead of built again
            contentType = gridItems.itemContentType { it::class }
        ) { index ->
            when (val gridItem = gridItems[index]) {
                is GridItem.ParentFolder -> ParentFolderRow(
                    onClick = { onFolderClick(gridItem.relativePath) }
                )

                is GridItem.Folder -> FolderRow(
                    folder = gridItem.folder,
                    selected = selectedFolders.contains(gridItem.folder.noteFolder),
                    isSelecting = isSelecting,
                    onClick = { onFolderClick(gridItem.folder.noteFolder.relativePath) },
                    onSelect = { add -> vm.selectFolder(gridItem.folder.noteFolder, add) },
                    onDelete = { onFolderDelete(gridItem.folder.noteFolder) },
                    onRename = { typed -> vm.renameFolder(gridItem.folder.noteFolder, typed) },
                )

                is GridItem.Note -> NoteListRow(
                    gridNote = gridItem.gridNote,
                    vm = vm,
                    onEditClick = onEditClick,
                    selectedNotes = selectedNotes,
                    isSelecting = isSelecting,
                    isSearching = isSearching,
                    dateFormat = dateFormat,
                )

                null -> Unit
            }
        }

        item {
            Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
        }
    }
}
