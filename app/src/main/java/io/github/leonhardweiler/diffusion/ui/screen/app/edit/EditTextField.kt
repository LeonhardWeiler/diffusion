package io.github.leonhardweiler.diffusion.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.TextVM

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
