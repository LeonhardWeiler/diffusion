package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.manager.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal val ButtonSize = 35.dp

private val BarBottomPadding = 8.dp

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    selectionSize: Int,
    selectedFolderCount: Int,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    query: StateFlow<String>,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    syncState: SyncState,
    hasLocalChanges: Boolean,
    onSyncClick: () -> Unit,
    unselectAll: () -> Unit,
    selectAll: () -> Unit,
    deleteSelection: () -> Unit,
) {
    AnimatedContent(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = BarBottomPadding),
        targetState = selectionSize == 0,
        label = "",
    ) { shouldShowSearchBar ->
        if (shouldShowSearchBar) {
            SearchBar(
                padding = padding,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                query = query.collectAsState().value,
                clearQuery = clearQuery,
                search = search,
                syncState = syncState,
                hasLocalChanges = hasLocalChanges,
                onSyncClick = onSyncClick,
            )
        } else {
            SelectableTopBar(
                padding = padding,
                selectionSize = selectionSize,
                selectedFolderCount = selectedFolderCount,
                unselectAll = unselectAll,
                selectAll = selectAll,
                deleteSelection = deleteSelection,
            )
        }
    }
}

@Composable
@Preview
private fun TopBarPreview() {
    TopBar(
        padding = PaddingValues(),
        onSettingsClick = {},
        searchFocusRequester = remember { FocusRequester() },
        query = MutableStateFlow(""),
        clearQuery = { },
        search = {},
        syncState = SyncState.Error("hello"),
        hasLocalChanges = true,
        onSyncClick = {},
        selectionSize = 0,
        selectedFolderCount = 0,
        unselectAll = { },
        selectAll = { },
        deleteSelection = {}
    )
}
