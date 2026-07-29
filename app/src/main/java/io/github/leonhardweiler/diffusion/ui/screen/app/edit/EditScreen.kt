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
import androidx.compose.material.icons.filled.TextFormat
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
import androidx.compose.runtime.saveable.rememberSaveable
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

    // Markdown gets the editor that knows about lists and headings; everything
    // else gets a text field. Not a failure for an extension nobody knows: the
    // list opens those with another app rather than here, but a note is better
    // shown as plain text than not at all.
    val vm = when (extensionType(note.fileExtension().text)) {
        ExtensionType.Markdown -> newMarkDownVM(note)
        else -> newEditViewModel(note)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vm.saveNow()
    }

    // there is no save button: leaving the editor is what ends an edit
    fun finish() {
        vm.saveNow()
        onFinished()
    }

    BackHandler {
        finish()
    }

    // both survive the switch between reading and editing, and the keyboard
    // coming and going
    val readScrollState = rememberLazyListState()
    val writeScrollState = rememberScrollState()

    val textFocusRequester = remember { FocusRequester() }

    // Reading mode is markdown being rendered. A plain text file has nothing to
    // render — it was shown in a field that refused to be typed in, and the only
    // way back was the very button that put it there.
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

                    // What the note is called, and nothing to type in. Renaming
                    // is one act, done from the note's row in the list — as a
                    // field here it was a rename that happened halfway through
                    // whichever save came next, and it was also the one way to
                    // give a note a name no file can carry. A note is written
                    // and named before it is opened now.
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
                        // the icon shows what the tap does, not what the note is
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Box(
                modifier = Modifier.weight(1f)
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

            when (vm) {
                is MarkDownVM -> {
                    val textFormatExpanded =
                        rememberSaveable(isReadOnlyModeActive) { mutableStateOf(false) }

                    if (textFormatExpanded.value) {
                        TextFormatRow(vm = vm, textFormatExpanded = textFormatExpanded)
                    } else {
                        DefaultRow(
                            vm = vm,
                            isReadOnlyModeActive = isReadOnlyModeActive,
                            leftContent = {
                                SmallButton(
                                    onClick = {
                                        textFormatExpanded.value = true
                                    },
                                    enabled = !isReadOnlyModeActive,
                                    imageVector = Icons.Default.TextFormat,
                                    contentDescription = "text format"
                                )
                            }
                        )
                    }

                }

                else -> {
                    DefaultRow(
                        vm = vm,
                        isReadOnlyModeActive = isReadOnlyModeActive,
                    )
                }
            }
        }


    }
}

/**
 * The note as something to type in.
 *
 * The scrolling is the column's, not the field's. A TextField that scrolls
 * itself is measured again when the keyboard takes half the screen away, and
 * comes back at the first line — so tapping into a note that was scrolled down
 * threw away the place being read, every time, unless the keyboard happened to
 * be open already. A scroll state held out here does not notice the resize.
 *
 * Where the caret has to be for it to be seen is decided by [CaretScroller],
 * not by the column.
 */
@Composable
fun GenericTextField(
    vm: TextVM,
    textFocusRequester: FocusRequester,
    isReadOnlyModeActive: Boolean = false,
    textContent: TextFieldValue,
    scrollState: ScrollState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // the field is as tall as its text, but never shorter than the screen:
        // below the last line is where one taps to start writing
        val minHeight = maxHeight

        val caretScroller = remember(scrollState) { CaretScroller(scrollState) }

        // Where every line of the note stands, which is what makes the caret
        // something to work out rather than something to be told about.
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

        var isFocused by remember { mutableStateOf(false) }

        // The field draws its text inside its own padding, and the column
        // measures from the top of the field — so this is all there is between
        // the two, and the reader is padded with the same thing.
        val textTop = with(LocalDensity.current) {
            TextFieldDefaults.contentPaddingWithoutLabel().calculateTopPadding().toPx()
        }

        fun caretRect() = caretRectOf(layout, textContent.selection, textTop)

        // Typing and tapping both come through here: the caret is at a new
        // offset, and the column follows it if it has left the screen. Taking
        // focus does not — the offset it arrives with was written down as
        // handled while the tap that moves it was still on its way.
        LaunchedEffect(textContent.selection, layout, isFocused) {
            if (!isFocused) return@LaunchedEffect
            caretScroller.caretMoved(textContent.selection.start, caretRect() ?: return@LaunchedEffect)
        }

        // The keyboard arriving is what makes the tapped line disappear: the
        // scaffold takes the ime inset off, this box is measured smaller, and
        // the caret that was in the lower half is now behind the keys. One
        // frame later, so that the new size has reached the scroll state.
        LaunchedEffect(scrollState.viewportSize) {
            withFrameNanos { }
            if (!isFocused) return@LaunchedEffect
            caretScroller.keepCaretVisible(caretRect() ?: return@LaunchedEffect)
        }

        Column(modifier = Modifier.verticalScroll(scrollState)) {

            // A field without a decoration, which is what this one always drew:
            // no label, no placeholder, no indicator, and the container in the
            // colour of the page behind it. What the plain one gives instead is
            // where its lines are — the material one keeps that to itself, and
            // without it the caret is something to be guessed at.
            //
            // The padding is the one the material field uses when it carries no
            // label, so the note starts at the same height as in the reader.
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