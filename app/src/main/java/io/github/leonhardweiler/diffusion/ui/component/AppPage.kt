package io.github.leonhardweiler.diffusion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.ui.utils.conditional

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(
    /**
     * The heading of the bar above the page — and null for a page that has no
     * bar. The setup is all of those: its screens say what they are in the
     * sentence at the top of the page itself, and a second heading above that
     * repeating "Choose method" or "SSH keys" was a strip of chrome saying
     * nothing twice. What is left of the bar there is the way back.
     */
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    onBackClick: (() -> Unit)? = null,
    onBackClickEnabled: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    disableVerticalScroll: Boolean = false,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {

    Scaffold(
        modifier = Modifier
            .imePadding(),
        contentWindowInsets = contentWindowInsets,
        topBar = {
            val back: @Composable () -> Unit = {
                onBackClick?.let {
                    IconButton(
                        onClick = it,
                        enabled = onBackClickEnabled
                    ) {
                        SimpleIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            }

            if (title == null) {
                // no bar, no background, no heading: an arrow standing on the
                // page, and the page beginning where the screen does
                Row(content = { back() })
            } else {
                TopAppBar(
                    actions = actions,
                    title = {
                        Text(
                            text = title,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = titleStyle
                        )
                    },
                    navigationIcon = back,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(15.dp)
                    )
                )
            }
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .conditional(!disableVerticalScroll) {
                        verticalScroll(rememberScrollState())
                    },
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment
            ) {
                content()

            }
            bottomBar()
        }
    }
}
