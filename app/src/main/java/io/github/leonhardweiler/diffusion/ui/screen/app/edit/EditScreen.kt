package io.github.leonhardweiler.diffusion.ui.screen.app.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.manager.ExtensionType
import io.github.leonhardweiler.diffusion.manager.extensionType
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.MarkDownVM
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.TextVM
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.newEditViewModel
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.newMarkDownVM

private const val TAG = "EditScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    note: Note,
    onFinished: () -> Unit,
) {
    val vm = when (extensionType(note.fileExtension().text)) {
        ExtensionType.Markdown -> newMarkDownVM(note)
        else -> newEditViewModel(note)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vm.saveNow()
    }

    fun finish() {
        vm.saveNow()
        onFinished()
    }

    BackHandler {
        finish()
    }

    val readScrollState = rememberLazyListState()
    val writeScrollState = rememberScrollState()

    val textFocusRequester = remember { FocusRequester() }

    val hasReadingMode = vm is MarkDownVM

    val isReadOnlyModeActive = hasReadingMode && vm.isReading.value

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(15.dp)

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { finish() },
                    ) {
                        SimpleIcon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = vm.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    if (!hasReadingMode) return@TopAppBar

                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            vm.setReadOnlyMode(!isReadOnlyModeActive)
                        },
                    ) {
                        SimpleIcon(
                            imageVector = if (isReadOnlyModeActive) {
                                Icons.Default.Edit
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (isReadOnlyModeActive) {
                                stringResource(R.string.edit_note)
                            } else {
                                stringResource(R.string.view_note)
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val textContent = vm.content.value

            when (vm) {
                is MarkDownVM -> {
                    MarkDownContent(
                        vm = vm,
                        textFocusRequester = textFocusRequester,
                        isReadOnlyModeActive = isReadOnlyModeActive,
                        textContent = textContent,
                        readScrollState = readScrollState,
                        writeScrollState = writeScrollState,
                    )
                }

                else -> {
                    GenericTextField(
                        vm = vm,
                        textFocusRequester = textFocusRequester,
                        isReadOnlyModeActive = isReadOnlyModeActive,
                        textContent = textContent,
                        scrollState = writeScrollState,
                    )
                }
            }
        }
    }
}

@Composable
fun GenericTextField(
    vm: TextVM,
    textFocusRequester: FocusRequester,
    isReadOnlyModeActive: Boolean = false,
    textContent: TextFieldValue,
    scrollState: ScrollState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minHeight = maxHeight

        val caretScroller = remember(scrollState) { CaretScroller(scrollState) }

        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

        var isFocused by remember { mutableStateOf(false) }

        val textTop = with(LocalDensity.current) {
            TextFieldDefaults.contentPaddingWithoutLabel().calculateTopPadding().toPx()
        }

        fun caretRect() = caretRectOf(layout, textContent.selection, textTop)

        LaunchedEffect(textContent.selection, layout, isFocused) {
            if (!isFocused) return@LaunchedEffect
            caretScroller.caretMoved(textContent.selection.start, caretRect() ?: return@LaunchedEffect)
        }

        LaunchedEffect(scrollState.viewportSize) {
            withFrameNanos { }
            if (!isFocused) return@LaunchedEffect
            caretScroller.keepCaretVisible(caretRect() ?: return@LaunchedEffect)
        }

        // the column scrolls, not the field: a TextField that scrolls itself is
        // re-measured when the keyboard takes half the screen and comes back at
        // the first line. BasicTextField because the material one keeps its
        // text layout to itself, and the padding is what the reader pads its
        // list with, so reading and writing start at the same height.
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(MaterialTheme.colorScheme.background)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        if (state.isFocused) {
                            caretScroller.focusGained(textContent.selection.start)
                        } else {
                            caretScroller.focusLost()
                        }
                    }
                    .caretIntoView()
                    .focusRequester(textFocusRequester)
                    .padding(TextFieldDefaults.contentPaddingWithoutLabel()),
                value = textContent,
                onValueChange = { vm.onValueChange(it) },
                onTextLayout = { layout = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardActions = KeyboardActions(
                    onDone = { vm.saveNow() }
                ),
                readOnly = isReadOnlyModeActive
            )
        }
    }
}
