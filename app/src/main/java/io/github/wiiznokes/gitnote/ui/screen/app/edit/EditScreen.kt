package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.manager.ExtensionType
import io.github.wiiznokes.gitnote.manager.extensionType
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.MarkDownVM
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.TextVM
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.newEditViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.newMarkDownVM


private const val TAG = "EditScreen"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    editParams: EditParams,
    onFinished: () -> Unit,
) {


    val extension = editParams.fileExtension()

    val vm = when (extensionType(extension.text)) {
        ExtensionType.Text -> newEditViewModel(editParams)
        ExtensionType.Markdown -> newMarkDownVM(editParams)
        null -> throw Exception("file extension not supported, but present in the database?? $extension")
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

    val nameFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }

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

    val isReadOnlyModeActive =
        !vm.shouldForceNotReadOnlyMode.value && vm.prefs.isReadOnlyModeActive.getAsState().value

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

                    TextField(
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester),
                        value = vm.name.value,
                        onValueChange = {
                            vm.onNameChange(it)
                        },
                        readOnly = isReadOnlyModeActive,
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

/** How long the place being read is held against the caret being brought into view. */
private const val KEEP_SCROLL_FRAMES = 4

/**
 * The note as something to type in.
 *
 * The scrolling is the column's, not the field's. A TextField that scrolls
 * itself is measured again when the keyboard takes half the screen away, and
 * comes back at the first line — so tapping into a note that was scrolled down
 * threw away the place being read, every time, unless the keyboard happened to
 * be open already. A scroll state held out here does not notice the resize.
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

        // Where the note was being read when the field took focus.
        //
        // A field that is given focus asks for its caret to be brought into
        // view, and the caret of a note that has only been scrolled through is
        // still on the first line — so the ask arrives as "scroll back to the
        // top". The tap sets the caret afterwards and lands where it was aimed,
        // which is why the caret was right and the note was not where it had
        // been. Once the caret is somewhere on screen the ask is answered by
        // doing nothing, so this only ever concerns the first tap.
        var placeBeingRead by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(placeBeingRead) {
            val place = placeBeingRead ?: return@LaunchedEffect

            // The ask is made over the next frames and it animates, so putting
            // the note back once would only race it. Held for a few frames it
            // is the last word — and short enough that the keyboard, which
            // arrives later, can still move a caret it would cover into sight.
            repeat(KEEP_SCROLL_FRAMES) {
                withFrameNanos { }
                if (scrollState.value != place) scrollState.scrollTo(place)
            }
            placeBeingRead = null
        }

        Column(modifier = Modifier.verticalScroll(scrollState)) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .onFocusChanged { state ->
                        if (state.isFocused) placeBeingRead = scrollState.value
                    }
                    .focusRequester(textFocusRequester),
                value = textContent,
                onValueChange = { vm.onValueChange(it) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { vm.saveNow() }
                ),
                readOnly = isReadOnlyModeActive
            )
        }
    }
}