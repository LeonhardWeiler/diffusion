package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.ui.model.GridItem
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.holds
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel
import java.text.DateFormat

@Composable
internal fun NoteListView(
    gridItems: List<GridItem>,
    topSpacerHeight: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    selectedNotes: List<NoteHeader>,
    selectedFolders: List<NoteFolder>,
    onEditClick: (Note) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderDelete: (NoteFolder) -> Unit,
    isSearching: Boolean,
    vm: GridViewModel,
) {
    val isSelecting = selectedNotes.isNotEmpty() || selectedFolders.isNotEmpty()

    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        item {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        items(
            items = gridItems,
            key = { it.key() },
            contentType = { it::class }
        ) { gridItem ->
            when (gridItem) {
                is GridItem.ParentFolder -> ParentFolderRow(
                    onClick = { onFolderClick(gridItem.relativePath) }
                )

                is GridItem.Folder -> FolderRow(
                    folder = gridItem.folder,
                    selected = selectedFolders.holds(gridItem.folder.noteFolder),
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
        }
    }
}
