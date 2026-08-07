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
import androidx.compose.material.icons.rounded.Folder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDown
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDownModel
import io.github.leonhardweiler.diffusion.ui.component.GetStringDialog
import io.github.leonhardweiler.diffusion.ui.component.RequestConfirmationDialog
import io.github.leonhardweiler.diffusion.ui.model.FolderModel

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
    val deleteExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(Offset.Zero) }

    // the dialog state lives in the row, not in the menu, which returns early
    // while closed and would take the state with it
    if (deleteExpanded.value) {
        RequestConfirmationDialog(
            expanded = deleteExpanded,
            text = stringResource(
                R.string.confirm_delete_folder,
                folder.noteFolder.fullName()
            ),
            onConfirmation = onDelete,
        )
    }

    if (renameExpanded.value) {
        GetStringDialog(
            expanded = renameExpanded,
            label = stringResource(R.string.new_path_label),
            actionText = stringResource(R.string.save),
            defaultString = folder.noteFolder.fullName(),
            onValidation = onRename,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground(selected))
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = { if (isSelecting) onSelect(!selected) else onClick() }
            )
            .pointerInteropFilter {
                clickPosition.value = Offset(it.x, it.y)
                false
            }
    ) {
        Box {
            Box {
                if (dropDownExpanded.value) CustomDropDown(
                    expanded = dropDownExpanded,
                    shape = MaterialTheme.shapes.medium,
                    options = listOfNotNull(
                        if (!isSelecting) CustomDropDownModel(
                            text = stringResource(R.string.rename_or_move),
                            onClick = { renameExpanded.value = true }
                        ) else null,
                        CustomDropDownModel(
                            text = stringResource(R.string.delete_this_folder),
                            onClick = { deleteExpanded.value = true }
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
