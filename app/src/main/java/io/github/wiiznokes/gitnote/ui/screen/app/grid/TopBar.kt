package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.manager.SyncState

/** The bar above the note list: the search, or what to do with a selection. */
internal val ButtonSize = 35.dp

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    selectedNotesNumber: Int,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    onReloadDatabase: () -> Unit,
    query: String,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    syncState: SyncState,
    onSyncClick: () -> Unit,
    isReadOnlyModeActive: Boolean,
    updateSettings: (suspend AppPreferences.() -> Unit) -> Unit,
    unselectAllNotes: () -> Unit,
    deleteSelectedNotes: () -> Unit,
) {

    AnimatedContent(
        modifier = modifier,
        targetState = selectedNotesNumber == 0,
        label = "",
    ) { shouldShowSearchBar ->
        if (shouldShowSearchBar) {
            SearchBar(
                padding = padding,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                onReloadDatabase = onReloadDatabase,
                query = query,
                clearQuery = clearQuery,
                search = search,
                syncState = syncState,
                onSyncClick = onSyncClick,
                isReadOnlyModeActive = isReadOnlyModeActive,
                updateSettings = updateSettings,
            )
        } else {
            SelectableTopBar(
                padding = padding,
                selectedNotesNumber = selectedNotesNumber,
                unselectAllNotes = unselectAllNotes,
                deleteSelectedNotes = deleteSelectedNotes,
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
        onReloadDatabase = { },
        query = "",
        clearQuery = { },
        search = {},
        syncState = SyncState.Error("hello"),
        onSyncClick = {},
        isReadOnlyModeActive = true,
        updateSettings = { },
        selectedNotesNumber = 0,
        unselectAllNotes = { },
        deleteSelectedNotes = {}
    )
}
