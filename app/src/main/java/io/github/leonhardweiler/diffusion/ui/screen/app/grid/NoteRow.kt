package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.helper.openFileWithAnotherApp
import io.github.leonhardweiler.diffusion.ui.component.GetStringDialog
import io.github.leonhardweiler.diffusion.ui.component.RequestConfirmationDialog
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.holds
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel
import java.text.DateFormat
import java.util.Date

@Composable
internal fun NoteListRow(
    gridNote: GridNote,
    vm: GridViewModel,
    onEditClick: (Note) -> Unit,
    selectedNotes: List<NoteHeader>,
    isSelecting: Boolean,
    isSearching: Boolean,
    dateFormat: DateFormat,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val deleteExpanded = remember { mutableStateOf(false) }
    val renameExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    if (deleteExpanded.value) {
        RequestConfirmationDialog(
            expanded = deleteExpanded,
            text = stringResource(R.string.confirm_delete_note, gridNote.note.fileName),
            onConfirmation = { vm.deleteNote(gridNote.note) },
        )
    }

    if (renameExpanded.value) {
        GetStringDialog(
            expanded = renameExpanded,
            label = stringResource(R.string.new_path_label),
            actionText = stringResource(R.string.save),
            defaultString = gridNote.note.fileName,
            onValidation = { vm.renameNote(gridNote.note, it) },
        )
    }

    // asked by id, because a row is handed a new NoteHeader whenever anything
    // about the note changes
    val selected = selectedNotes.holds(gridNote.note)

    val formattedDate = remember(gridNote.note.lastModifiedTimeMillis) {
        dateFormat.format(Date(gridNote.note.lastModifiedTimeMillis))
    }

    val isNote = remember(gridNote.note.fileName) { gridNote.note.isNote() }

    val title = when {
        isSearching || !gridNote.isUnique -> gridNote.note.relativePath
        isNote -> gridNote.note.nameWithoutExtension()
        else -> gridNote.note.fileName
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground(selected))
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = {
                    when {
                        isSelecting -> vm.selectNote(gridNote.note, add = !selected)
                        isNote -> vm.openNote(gridNote.note, onEditClick)
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
                onDeleteRequest = { deleteExpanded.value = true },
                onRenameRequest = { renameExpanded.value = true },
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
