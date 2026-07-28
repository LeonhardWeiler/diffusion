package io.github.leonhardweiler.gitnote.ui.screen.app.grid

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.leonhardweiler.gitnote.R
import io.github.leonhardweiler.gitnote.data.room.Note
import io.github.leonhardweiler.gitnote.data.room.NoteFolder
import io.github.leonhardweiler.gitnote.ui.component.CustomDropDown
import io.github.leonhardweiler.gitnote.ui.component.CustomDropDownModel
import io.github.leonhardweiler.gitnote.ui.model.EditType
import io.github.leonhardweiler.gitnote.ui.model.GridNote
import io.github.leonhardweiler.gitnote.ui.model.NoteHeader
import io.github.leonhardweiler.gitnote.ui.viewmodel.GridViewModel
import io.github.leonhardweiler.gitnote.utils.getParentPath


private const val TAG = "GridScreen"

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
            modifier = Modifier.onSizeChanged { topBarSize = it },
            selectionSize = selectionSize,
            onSettingsClick = onSettingsClick,
            searchFocusRequester = searchFocusRequester,
            padding = padding,
            onReloadDatabase = {
                vm.reloadDatabase()
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
    val gridItems = vm.gridItems.collectAsLazyPagingItems()
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
    clickPosition: MutableState<Offset>,
) {

    // building the options means reading strings, which is worth doing only for
    // the row whose menu is actually open
    if (!dropDownExpanded.value) return

    // need this box for clickPosition
    Box {
        CustomDropDown(
            expanded = dropDownExpanded,
            shape = MaterialTheme.shapes.medium,
            options = listOf(
                CustomDropDownModel(
                    text = stringResource(R.string.delete_this_note),
                    onClick = { vm.deleteNote(gridNote.note) }),
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
