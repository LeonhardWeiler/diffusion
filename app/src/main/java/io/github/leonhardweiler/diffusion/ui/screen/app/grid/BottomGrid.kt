package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDown
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDownModel
import io.github.leonhardweiler.diffusion.ui.component.GetStringDialog
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel


@Composable
fun FloatingActionButtons(
    vm: GridViewModel,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val showCreateFolder = rememberSaveable { mutableStateOf(false) }
    val showCreateNote = rememberSaveable { mutableStateOf(false) }

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

    // The name of a note is asked for here, and the note is written and left in
    // the list. Creating one used to open the editor on a note with no file
    // behind it yet — see GridViewModel.createNote.
    //
    // Composed only while it is open, the way the dialogs of a row are.
    if (showCreateNote.value) {
        GetStringDialog(
            expanded = showCreateNote,
            label = stringResource(R.string.note_name),
            actionText = stringResource(R.string.create_new_note),
            defaultString = vm.defaultNewNoteName(),
            unExpandedOnValidation = false
        ) { name ->
            if (vm.createNote(name)) {
                showCreateNote.value = false
            }
        }
    }

    Box {
        CustomDropDown(
            expanded = dropDownExpanded,
            options = listOf(
                CustomDropDownModel(
                    text = stringResource(R.string.create_new_note),
                    onClick = { showCreateNote.value = true }
                ),
                CustomDropDownModel(
                    text = stringResource(R.string.create_new_folder),
                    onClick = { showCreateFolder.value = true }
                ),
            )
        )

        FloatingActionButton(
            onClick = { dropDownExpanded.value = true },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.create_new_note),
            )
        }
    }
}
