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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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

private const val maxOffset = -500f
internal val topBarHeight = 80.dp

internal val topSpacerHeight = topBarHeight + 40.dp + 15.dp

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

    val offset = remember { mutableFloatStateOf(0f) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {

            if (selectedNotes.isEmpty()) {
                FloatingActionButtons(
                    vm = vm,
                    offset = { offset.floatValue },
                    onEditClick = onEditClick,
                )
            }

        }) { padding ->

        val nestedScrollConnection = rememberNestedScrollConnection(
            offset = offset,
        )


        GridView(
            vm = vm,
            onEditClick = onEditClick,
            selectedNotes = selectedNotes,
            nestedScrollConnection = nestedScrollConnection,
            padding = padding,
        )

        TopBar(
            offset = { offset.floatValue },
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
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun GridView(
    vm: GridViewModel,
    nestedScrollConnection: NestedScrollConnection,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
    padding: PaddingValues,
) {
    val gridNotes = vm.gridNotes.collectAsLazyPagingItems()
    val query = vm.query.collectAsState()


    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, {
        Log.d(TAG, "pull refresh")
        vm.refresh()
    })

    val showFullPathOfNotes = vm.prefs.showFullPathOfNotes.getAsState()

    Box {

        // todo: scroll even when there is nothing to scroll
        // todo: add scroll bar

        val commonModifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
            .nestedScroll(nestedScrollConnection)

        val listState = rememberLazyListState()

        LaunchedEffect(query.value) {
            listState.animateScrollToItem(index = 0)
        }

        NoteListView(
            gridNotes = gridNotes,
            folders = vm.folders.collectAsState().value,
            currentFolderPath = vm.currentNoteFolderRelativePath.collectAsState().value,
            listState = listState,
            modifier = commonModifier,
            selectedNotes = selectedNotes,
            showFullPathOfNotes = showFullPathOfNotes.value,
            onEditClick = onEditClick,
            onFolderClick = vm::openFolder,
            onFolderDelete = vm::deleteFolder,
            vm = vm,
        )

        // fix me: https://stackoverflow.com/questions/74594418/pullrefreshindicator-overlaps-with-scrollabletabrow
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarHeight + padding.calculateTopPadding()),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            scale = true
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

// https://stackoverflow.com/questions/73079388/android-jetpack-compose-keyboard-not-close
// https://medium.com/@debdut.saha.1/top-app-bar-animation-using-nestedscrollconnection-like-facebook-jetpack-compose-b446c109ee52
// todo: fix scroll is blocked when the full size of the grid is the screen,
//  the stretching will cause tbe offset to not change
@Composable
private fun rememberNestedScrollConnection(
    offset: MutableFloatState,
): NestedScrollConnection {


    val keyboardController = LocalSoftwareKeyboardController.current

    return remember {
        var shouldBlock = false

        object : NestedScrollConnection {
            fun calculateOffset(delta: Float): Offset {
                offset.floatValue = (offset.floatValue + delta).coerceIn(maxOffset, 0f)
                //Log.d(TAG, "calculateOffset(newOffset: ${offset.floatValue}, delta: $delta)")
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                //Log.d(TAG, "onPreScroll(available: ${available.y})")
                if (!shouldBlock) keyboardController?.hide()

                return calculateOffset(available.y)
            }

            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource
            ): Offset {
                //Log.d(TAG, "onPostScroll(consumed: ${consumed.y}, available: ${available.y})")
                return calculateOffset(available.y)
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
