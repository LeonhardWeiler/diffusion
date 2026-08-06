package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.manager.SyncState
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon

@Composable
internal fun SearchBar(
    padding: PaddingValues,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    query: String,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    syncState: SyncState,
    hasLocalChanges: Boolean,
    onSyncClick: () -> Unit,
) {


    val queryTextField = remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }

    val focusManager = LocalFocusManager.current
    fun clearQuery2() {
        queryTextField.value = TextFieldValue("")
        clearQuery()
        focusManager.clearFocus()
    }

    if (query.isNotEmpty()) {
        BackHandler {
            clearQuery2()
        }
    }


    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 10.dp)
            .padding(top = 15.dp)
            .focusRequester(searchFocusRequester),
        value = queryTextField.value,
        onValueChange = {
            queryTextField.value = it
            search(it.text)
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(100),
        placeholder = {
            Text(text = stringResource(R.string.search_in_notes))
        },
        singleLine = true,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                val isEmpty = query.isEmpty()

                if (isEmpty) {
                    SyncButton(
                        state = syncState,
                        hasLocalChanges = hasLocalChanges,
                        onClick = onSyncClick,
                    )
                }

                // The settings, and no menu in front of them. Reading the
                // repository again was the only other thing in here, and it is a
                // row of the settings screen as well — where it is reachable in
                // a release build, which it was not here.
                if (isEmpty) {
                    IconButton(
                        modifier = Modifier.size(ButtonSize),
                        onClick = onSettingsClick,
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!isEmpty) {
                    IconButton(
                        onClick = { clearQuery2() }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )

}
