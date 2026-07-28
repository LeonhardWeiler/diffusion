package io.github.wiiznokes.gitnote.ui.screen.app.grid

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import io.github.wiiznokes.gitnote.utils.getParentPath


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
    val currentFolderPath by vm.currentNoteFolderRelativePath.collectAsState()

    if (selectedNotes.isNotEmpty()) {
        BackHandler {
            vm.unselectAllNotes()
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

            if (selectedNotes.isEmpty()) {
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
            currentFolderPath = currentFolderPath,
            nestedScrollConnection = nestedScrollConnection,
            padding = padding,
        )

        TopBar(
            modifier = Modifier.onSizeChanged { topBarSize = it },
            selectedNotesNumber = selectedNotes.size,
            onSettingsClick = onSettingsClick,
            searchFocusRequester = searchFocusRequester,
            padding = padding,
            onReloadDatabase = {
                vm.reloadDatabase()
            },
            query = vm.query.collectAsState().value,
            clearQuery = vm::clearQuery,
            search = vm::search,
            syncState = vm.syncState.collectAsState().value,
            consumeOkSyncState = vm::consumeOkSyncState,
            isReadOnlyModeActive = vm.prefs.isReadOnlyModeActive.getAsState().value,
            updateSettings = vm::updateSettings,
            unselectAllNotes = vm::unselectAllNotes,
            deleteSelectedNotes = vm::deleteSelectedNotes,
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
    selectedNotes: List<Note>,
    currentFolderPath: String,
    topSpacerHeight: Dp,
    padding: PaddingValues,
) {
    val gridNotes = vm.gridNotes.collectAsLazyPagingItems()
    val query = vm.query.collectAsState()


    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    val showFullPathOfNotes = vm.prefs.showFullPathOfNotes.getAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            Log.d(TAG, "pull refresh")
            vm.refresh()
        },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topBarHeight + padding.calculateTopPadding()),
                containerColor = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    ) {

        // todo: scroll even when there is nothing to scroll
        // todo: add scroll bar

        val listState = rememberLazyListState()

        LaunchedEffect(query.value) {
            listState.scrollToItem(index = 0)
        }

        NoteListView(
            gridNotes = gridNotes,
            folders = vm.folders.collectAsState().value,
            currentFolderPath = currentFolderPath,
            topSpacerHeight = topSpacerHeight,
            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            selectedNotes = selectedNotes,
            showFullPathOfNotes = showFullPathOfNotes.value,
            onEditClick = onEditClick,
            onFolderClick = vm::openFolder,
            onFolderDelete = vm::deleteFolder,
            vm = vm,
        )
    }
}


@Composable
internal fun NoteActionsDropdown(
    vm: GridViewModel,
    gridNote: GridNote,
    selectedNotes: List<Note>,
    dropDownExpanded: MutableState<Boolean>,
    clickPosition: MutableState<Offset>,
) {

    // need this box for clickPosition
    Box {
        CustomDropDown(
            expanded = dropDownExpanded,
            shape = MaterialTheme.shapes.medium,
            options = listOf(
                CustomDropDownModel(
                    text = stringResource(R.string.delete_this_note),
                    onClick = { vm.deleteNote(gridNote.note) }),
                if (selectedNotes.isEmpty()) CustomDropDownModel(
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
