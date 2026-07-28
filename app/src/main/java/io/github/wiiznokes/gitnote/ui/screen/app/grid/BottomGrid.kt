package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import kotlin.math.roundToInt


@Composable
fun FloatingActionButtons(
    vm: GridViewModel,
    offset: Float,
    onEditClick: (Note, EditType) -> Unit,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val showCreateFolder = rememberSaveable { mutableStateOf(false) }

    GetStringDialog(
        expanded = showCreateFolder,
        label = stringResource(R.string.new_folder_label),
        actionText = stringResource(R.string.create_new_folder),
        unExpandedOnValidation = false
    ) { name ->
        if (vm.createNoteFolder(vm.currentNoteFolderRelativePath.value, name)) {
            showCreateFolder.value = false
        }
    }

    Box {
        CustomDropDown(
            expanded = dropDownExpanded,
            options = listOf(
                CustomDropDownModel(
                    text = stringResource(R.string.create_new_note),
                    onClick = { onEditClick(vm.defaultNewNote(), EditType.Create) }
                ),
                CustomDropDownModel(
                    text = stringResource(R.string.create_new_folder),
                    onClick = { showCreateFolder.value = true }
                ),
            )
        )

        FloatingActionButton(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = -offset.roundToInt()) },
            onClick = { dropDownExpanded.value = true },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.create_new_note),
            )
        }
    }
}
