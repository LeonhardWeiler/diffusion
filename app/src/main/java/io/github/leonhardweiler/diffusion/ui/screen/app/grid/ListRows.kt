package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.helper.openFileWithAnotherApp
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDown
import io.github.leonhardweiler.diffusion.ui.component.GetStringDialog
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDownModel
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel
import java.text.DateFormat
import java.util.Date

/**
 * A note row is two lines high, a folder row only one. Without a common floor
 * the list would look ragged wherever the two kinds of row meet, so every row
 * reserves the height of the taller one.
 */
private val ListRowMinHeight = 56.dp

/** The way out of a folder, which is the row above everything in it. */
@Composable
internal fun ParentFolderRow(
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
internal fun FolderRow(
    folder: FolderModel,
    selected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onSelect: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val renameExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }

    if (renameExpanded.value) {
        GetStringDialog(
            expanded = renameExpanded,
            label = stringResource(R.string.folder_new_path_label),
            actionText = stringResource(R.string.save),
            defaultString = folder.noteFolder.fullName(),
            onValidation = onRename,
        )
    }

    val rowBackground =
        if (selected) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                // while something is selected, tapping a row is how the
                // selection is changed — opening the folder would take the list
                // out from under what was marked
                onClick = { if (isSelecting) onSelect(!selected) else onClick() }
            )
            .pointerInteropFilter {
                clickPosition.value = Offset(it.x, it.y)
                false
            }
    ) {
        Box {
            // need this box for clickPosition
            Box {
                if (dropDownExpanded.value) CustomDropDown(
                    expanded = dropDownExpanded,
                    shape = MaterialTheme.shapes.medium,
                    options = listOfNotNull(
                        CustomDropDownModel(
                            text = stringResource(R.string.rename_this_folder),
                            onClick = { renameExpanded.value = true }
                        ),
                        CustomDropDownModel(
                            text = stringResource(R.string.delete_this_folder),
                            onClick = onDelete
                        ),
                        if (!isSelecting) CustomDropDownModel(
                            text = stringResource(R.string.select_multiple_notes),
                            onClick = { onSelect(true) }
                        ) else null,
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
internal fun NoteListRow(
    gridNote: GridNote,
    vm: GridViewModel,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<NoteHeader>,
    isSelecting: Boolean,
    isSearching: Boolean,
    dateFormat: DateFormat,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    // Asked of the selection here rather than folded into the row by the view
    // model: a PagingData may be collected once, and combining the selection
    // into the paged list re-wrapped a stream that had already been read.
    val selected = selectedNotes.contains(gridNote.note)

    val formattedDate = remember(gridNote.note.lastModifiedTimeMillis) {
        dateFormat.format(Date(gridNote.note.lastModifiedTimeMillis))
    }

    // Everything in the repository is a row, not only what this app can read.
    val isNote = remember(gridNote.note.fileName) { gridNote.note.isNote() }

    // A search spans the whole repository, so the name alone does not say which
    // note was found: results are named by their path. A file that is not a
    // note keeps its extension either way — a row saying "holiday" for a jpeg
    // is a row that lies about what tapping it will do.
    val title = when {
        isSearching || !gridNote.isUnique -> gridNote.note.relativePath
        isNote -> gridNote.note.nameWithoutExtension()
        else -> gridNote.note.fileName
    }

    val rowBackground =
        if (selected) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = {
                    when {
                        isSelecting -> vm.selectNote(gridNote.note, add = !selected)
                        isNote -> vm.openNote(gridNote.note) { onEditClick(it, EditType.Update) }
                        else -> vm.openExternally(gridNote.note) {
                            openFileWithAnotherApp(context, it)
                        }
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
                isSelecting = isSelecting,
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
                    imageVector = if (isNote) {
                        Icons.Rounded.Description
                    } else {
                        Icons.Rounded.AttachFile
                    },
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

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(80.dp)
    )
}
