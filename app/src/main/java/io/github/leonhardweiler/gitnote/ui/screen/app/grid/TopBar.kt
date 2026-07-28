package io.github.leonhardweiler.gitnote.ui.screen.app.grid

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
import io.github.leonhardweiler.gitnote.data.AppPreferences
import io.github.leonhardweiler.gitnote.manager.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The bar above the note list: the search, or what to do with a selection. */
internal val ButtonSize = 35.dp

/**
 * Below the bar, inside its background rather than under it. The bar floats
 * over the list, and without this a row scrolling past stops being covered the
 * moment it clears the search field itself, which reads as the two touching.
 */
private val BarBottomPadding = 8.dp

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    selectedNotesNumber: Int,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    onReloadDatabase: () -> Unit,
    query: StateFlow<String>,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    syncState: SyncState,
    hasLocalChanges: Boolean,
    onSyncClick: () -> Unit,
    isReadOnlyModeActive: Boolean,
    updateSettings: (suspend AppPreferences.() -> Unit) -> Unit,
    unselectAllNotes: () -> Unit,
    deleteSelectedNotes: () -> Unit,
) {

    AnimatedContent(
        // the bar floats above the list, so without a background of its own the
        // rows would be readable through it as they scroll past underneath
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = BarBottomPadding),
        targetState = selectedNotesNumber == 0,
        label = "",
    ) { shouldShowSearchBar ->
        if (shouldShowSearchBar) {
            // collected here rather than by the caller: the query changes with
            // every keystroke, and reading it further up would recompose the
            // whole screen, list included, for each one
            SearchBar(
                padding = padding,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                onReloadDatabase = onReloadDatabase,
                query = query.collectAsState().value,
                clearQuery = clearQuery,
                search = search,
                syncState = syncState,
                hasLocalChanges = hasLocalChanges,
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
        query = MutableStateFlow(""),
        clearQuery = { },
        search = {},
        syncState = SyncState.Error("hello"),
        hasLocalChanges = true,
        onSyncClick = {},
        isReadOnlyModeActive = true,
        updateSettings = { },
        selectedNotesNumber = 0,
        unselectAllNotes = { },
        deleteSelectedNotes = {}
    )
}
