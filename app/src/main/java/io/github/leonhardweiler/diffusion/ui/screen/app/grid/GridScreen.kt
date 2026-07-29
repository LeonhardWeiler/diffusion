package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.helper.openFileWithAnotherApp
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDown
import io.github.leonhardweiler.diffusion.ui.component.CustomDropDownModel
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.viewmodel.GridViewModel
import io.github.leonhardweiler.diffusion.helper.getParentPath



internal val topBarHeight = 80.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GridScreen(
    onSettingsClick: () -> Unit,
    onEditClick: (Note, EditType) -> Unit,
) {

    val vm: GridViewModel = viewModel()

    val selectedNotes by vm.selectedNotes.collectAsState()
    val selectedFolders by vm.selectedFolders.collectAsState()
    val selectionSize by vm.selectionSize.collectAsState()
    val currentFolderPath by vm.currentNoteFolderRelativePath.collectAsState()

    if (selectionSize > 0) {
        BackHandler {
            vm.unselectAll()
        }
    } else if (currentFolderPath.isNotEmpty()) {
        BackHandler {
            vm.openFolder(getParentPath(currentFolderPath))
        }
    }

    val searchFocusRequester = remember { FocusRequester() }

    // the bar floats above the list, so the list needs to start below it
    var topBarSize by remember { mutableStateOf(IntSize.Zero) }
    val topSpacerHeight = with(LocalDensity.current) { topBarSize.height.toDp() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {

            if (selectionSize == 0) {
                FloatingActionButtons(
                    vm = vm,
                    onEditClick = onEditClick,
                )
            }

        }) { padding ->

        val nestedScrollConnection = rememberNestedScrollConnection()

        GridView(
            vm = vm,
            topSpacerHeight = topSpacerHeight,
            onEditClick = onEditClick,
            selectedNotes = selectedNotes,
            selectedFolders = selectedFolders,
            nestedScrollConnection = nestedScrollConnection,
            padding = padding,
        )

        TopBar(
            modifier = Modifier.onSizeChanged { size ->
                // The list begins below the bar, and the two bars are not the
                // same height to the pixel — the search bar is a text field
                // with its own padding, the selection bar a fixed row. Letting
                // the spacer follow whichever is showing meant the whole list
                // slid up a little the moment a row was selected, and back down
                // when the selection was cleared.
                //
                // So the offset is the search bar's, which is the one the list
                // is under for all but a moment.
                if (selectionSize == 0) topBarSize = size
            },
            selectionSize = selectionSize,
            selectedFolderCount = selectedFolders.size,
            onSettingsClick = onSettingsClick,
            searchFocusRequester = searchFocusRequester,
            padding = padding,
            onReloadIndex = {
                vm.reloadIndex()
            },
            query = vm.query,
            clearQuery = vm::clearQuery,
            search = vm::search,
            syncState = vm.syncState.collectAsState().value,
            hasLocalChanges = vm.hasLocalChanges.collectAsState().value,
            onSyncClick = vm::syncWithRemote,
            isReadOnlyModeActive = vm.prefs.isReadOnlyModeActive.getAsState().value,
            updateSettings = vm::updateSettings,
            unselectAll = vm::unselectAll,
            selectAll = vm::selectAll,
            deleteSelection = vm::deleteSelection,
        )
    }
}


@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun GridView(
    vm: GridViewModel,
    nestedScrollConnection: NestedScrollConnection,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<NoteHeader>,
    selectedFolders: List<NoteFolder>,
    topSpacerHeight: Dp,
    padding: PaddingValues,
) {
    val gridItems by vm.gridItems.collectAsState()
    val query = vm.query.collectAsState()


    val listState = rememberLazyListState()

    LaunchedEffect(query.value) {
        listState.scrollToItem(index = 0)
    }

    NoteListView(
        gridItems = gridItems,
        topSpacerHeight = topSpacerHeight,
        listState = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        selectedNotes = selectedNotes,
        selectedFolders = selectedFolders,
        onEditClick = onEditClick,
        onFolderClick = vm::openFolder,
        onFolderDelete = vm::deleteFolder,
        isSearching = query.value.isNotEmpty(),
        vm = vm,
    )
}


@Composable
internal fun NoteActionsDropdown(
    vm: GridViewModel,
    gridNote: GridNote,
    isSelecting: Boolean,
    dropDownExpanded: MutableState<Boolean>,
    /** Asks, rather than deletes: the dialog belongs to the row, which outlives this menu. */
    onDeleteRequest: () -> Unit,
    /** Same again for the rename dialog, which is the row's as well. */
    onRenameRequest: () -> Unit,
    clickPosition: MutableState<Offset>,
) {

    // building the options means reading strings, which is worth doing only for
    // the row whose menu is actually open
    if (!dropDownExpanded.value) return

    val context = LocalContext.current

    // need this box for clickPosition
    Box {
        CustomDropDown(
            expanded = dropDownExpanded,
            shape = MaterialTheme.shapes.medium,
            options = listOf(
                // Not while a selection is on: a rename is about one note, and
                // the marked rows are the ones every other entry here is about.
                if (!isSelecting) CustomDropDownModel(
                    text = stringResource(R.string.rename_or_move),
                    onClick = onRenameRequest) else null,
                CustomDropDownModel(
                    text = stringResource(R.string.delete_this_file),
                    onClick = onDeleteRequest),
                // On every row, not only on the ones this app cannot read: a
                // note above the size limit is opened here by nobody, and a
                // markdown file is something a user may well want to hand to
                // something else.
                CustomDropDownModel(
                    text = stringResource(R.string.open_with_another_app_action),
                    onClick = {
                        vm.openExternally(gridNote.note) { openFileWithAnotherApp(context, it) }
                    }),
                if (!isSelecting) CustomDropDownModel(
                    text = stringResource(R.string.select_multiple_notes),
                    onClick = { vm.selectNote(gridNote.note, true) }) else null,
            ),
            clickPosition = clickPosition
        )
    }
}

/**
 * Hides the keyboard once the user starts scrolling the list. The scroll that
 * carries a fling is left alone, otherwise the keyboard would close again right
 * after the user reopened it by tapping the search field.
 *
 * https://stackoverflow.com/questions/73079388/android-jetpack-compose-keyboard-not-close
 */
@Composable
private fun rememberNestedScrollConnection(): NestedScrollConnection {

    val keyboardController = LocalSoftwareKeyboardController.current

    return remember {
        var shouldBlock = false

        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!shouldBlock) keyboardController?.hide()
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                shouldBlock = true
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                shouldBlock = false
                return super.onPostFling(consumed, available)
            }

        }
    }
}
