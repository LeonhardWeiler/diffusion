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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.manager.ExtensionType
import io.github.leonhardweiler.diffusion.manager.extensionType
import io.github.leonhardweiler.diffusion.ui.component.SimpleIcon
import io.github.leonhardweiler.diffusion.ui.destination.EditParams
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.Draft
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.MarkDownVM
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.TextVM
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.newEditViewModel
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.newMarkDownVM


private const val TAG = "EditScreen"

/** Two strings, which is all a [Draft] is. */
private val DraftSaver = listSaver<Draft?, String>(
    save = { draft -> draft?.let { listOf(it.name, it.content) } ?: emptyList() },
    restore = { list -> if (list.size == 2) Draft(list[0], list[1]) else null }
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    editParams: EditParams,
    onFinished: () -> Unit,
) {


    val extension = editParams.fileExtension()

    // What the editor held when the process was last stopped, if the disk could
    // not be given it — a name no file can carry is the only way that happens.
    // Kept here rather than in a file of its own: this is saved state of the
    // screen, so Android writes it out with everything else and hands it back
    // when the process comes up again.
    //
    // Before the view model, because the view model is built from it.
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf<Draft?>(null) }

    // Markdown gets the editor that knows about lists and headings; everything
    // else gets a text field. Not a failure for an extension nobody knows: the
    // list refuses to open those, but a draft restored after a crash can still
    // name one, and a note is better shown as plain text than not at all.
    val vm = when (extensionType(extension.text)) {
        ExtensionType.Markdown -> newMarkDownVM(editParams, draft)
        else -> newEditViewModel(editParams, draft)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vm.saveNow()
        // after the write, so that this is only what the write could not take
        draft = vm.draft()
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

    val nameFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }

    // Whether this screen was opened to write a note that does not exist yet.
    // Asked once and kept, because vm.editType turns into Update the moment the
    // first save lands — and a field that goes read-only under the finger
    // typing in it is worse than one that never was.
    val isNewNote = rememberSaveable { vm.editType == EditType.Create }

    // tricks to request focus only one time
    var lastId: Boolean by rememberSaveable { mutableStateOf(false) }
    if (!lastId) {
        lastId = true
        LaunchedEffect(null) {
            if (vm.editType == EditType.Create) {
                nameFocusRequester.requestFocus()
            }
        }
    }

    // Reading mode is markdown being rendered. A plain text file has nothing to
    // render — it was shown in a field that refused to be typed in, and the only
    // way back was the very button that put it there.
    val hasReadingMode = vm is MarkDownVM

    val isReadOnlyModeActive = hasReadingMode &&
            !vm.shouldForceNotReadOnlyMode.value &&
            vm.prefs.isReadOnlyModeActive.getAsState().value

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

                    // The name of a note that exists is what it is called, not
                    // a field: renaming it is one act, done from its row in the
                    // list, and not something that happened halfway through
                    // whichever save came next. A note being written for the
                    // first time has no name yet, and typing one here is how it
                    // gets one.
                    TextField(
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester),
                        value = vm.name.value,
                        onValueChange = {
                            vm.onNameChange(it)
                        },
                        readOnly = isReadOnlyModeActive || !isNewNote,
                        singleLine = true,
                        placeholder = {
                            Text(text = stringResource(R.string.note_name))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                            focusedContainerColor = backgroundColor,
                            unfocusedContainerColor = backgroundColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                textFocusRequester.requestFocus()
                            }
                        )
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