package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.ui.res.stringResource
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FolderModel
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import io.github.wiiznokes.gitnote.utils.getParentPath
import java.text.DateFormat
import java.util.Date

private const val PARENT_FOLDER_KEY = ".."

/**
 * A note row is two lines high, a folder row only one. Without a common floor
 * the list would look ragged wherever the two kinds of row meet, so every row
 * reserves the height of the taller one.
 */
private val ListRowMinHeight = 56.dp

@Composable
internal fun NoteListView(
    gridNotes: LazyPagingItems<GridNote>,
    folders: List<FolderModel>,
    currentFolderPath: String,
    topSpacerHeight: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
    onEditClick: (Note, EditType) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderDelete: (NoteFolder) -> Unit,
    vm: GridViewModel,
) {

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        item {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        if (currentFolderPath.isNotEmpty()) {
            item(key = PARENT_FOLDER_KEY) {
                ParentFolderRow(onClick = { onFolderClick(getParentPath(currentFolderPath)) })
            }
        }

        items(
            items = folders,
            key = { it.noteFolder.id }
        ) { folder ->
            FolderRow(
                folder = folder,
                onClick = { onFolderClick(folder.noteFolder.relativePath) },
                onDelete = { onFolderDelete(folder.noteFolder) },
            )
        }

        items(
            count = gridNotes.itemCount,
            key = gridNotes.itemKey { it.note.id }
        ) { index ->
            val gridNote = gridNotes[index] ?: return@items
            NoteListRow(
                gridNote = gridNote,
                vm = vm,
                onEditClick = onEditClick,
                selectedNotes = selectedNotes,
                showFullPathOfNotes = showFullPathOfNotes,
            )
        }

        item {
            Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
        }
    }
}

@Composable
private fun ParentFolderRow(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // ".." alone says nothing when read out, so the row carries the label
            .clickable(
                onClickLabel = stringResource(R.string.parent_folder),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ListRowMinHeight)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "..",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        RowDivider()
    }
}

@Composable
private fun FolderRow(
    folder: FolderModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = onClick
            )
            .pointerInteropFilter {
                clickPosition.value = Offset(it.x, it.y)
                false
            }
    ) {
        Box {
            // need this box for clickPosition
            Box {
                CustomDropDown(
                    expanded = dropDownExpanded,
                    shape = MaterialTheme.shapes.medium,
                    options = listOf(
                        CustomDropDownModel(
                            text = stringResource(R.string.delete_this_folder),
                            onClick = onDelete
                        ),
                    ),
                    clickPosition = clickPosition
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ListRowMinHeight)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    modifier = Modifier.weight(1f),
                    text = folder.noteFolder.fullName(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = folder.noteCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RowDivider()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(80.dp)
    )
}

@Composable
private fun NoteListRow(
    gridNote: GridNote,
    vm: GridViewModel,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }

    val formattedDate = remember(gridNote.note.lastModifiedTimeMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(gridNote.note.lastModifiedTimeMillis))
    }

    val title = if (showFullPathOfNotes || !gridNote.isUnique) {
        gridNote.note.relativePath
    } else {
        gridNote.note.nameWithoutExtension()
    }

    val rowBackground =
        if (gridNote.selected) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = {
                    if (selectedNotes.isEmpty()) {
                        onEditClick(gridNote.note, EditType.Update)
                    } else {
                        vm.selectNote(gridNote.note, add = !gridNote.selected)
                    }
                }
            )
            .pointerInteropFilter {
                clickPosition.value = Offset(it.x, it.y)
                false
            }
    ) {
        Box {
            NoteActionsDropdown(
                vm = vm,
                gridNote = gridNote,
                selectedNotes = selectedNotes,
                dropDownExpanded = dropDownExpanded,
                clickPosition = clickPosition
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ListRowMinHeight)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        RowDivider()
    }
}