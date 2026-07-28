package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.MarkDownVM

private val CheckBoxSize = 20.dp

/** Between the box and the words belonging to it, which otherwise touch. */
private val CheckBoxTextGap = 8.dp

@Composable
fun MarkDownContent(
    vm: MarkDownVM,
    textFocusRequester: FocusRequester,
    isReadOnlyModeActive: Boolean,
    textContent: TextFieldValue,
    readScrollState: LazyListState,
    writeScrollState: ScrollState,
) {
    // What the reading mode shows, which is the text as it stood when it was
    // last entered. Kept outside the branch below so that it survives a switch
    // to writing and back: rememberMarkdownState re-parses whenever its input
    // changes, and while it does the reader shows nothing at all. Coming back
    // to a note that was not edited therefore costs no parse at all, and typing
    // costs none until the reading mode is asked for again.
    var readText by remember { mutableStateOf(textContent.text) }

    LaunchedEffect(isReadOnlyModeActive) {
        if (isReadOnlyModeActive) readText = textContent.text
    }

    val markdownState = rememberMarkdownState(readText)

    // A checkbox that was ticked is written to the note but not parsed again:
    // the whole tree would be thrown away and rebuilt for one character, and the
    // reader would go blank while it happened. "[ ]" and "[x]" are the same
    // length, so the offsets the parser handed us still point at the right box.
    val ticked = remember(readText) { mutableStateMapOf<Int, Boolean>() }

    if (isReadOnlyModeActive) {
        SelectionContainer {
            Markdown(
                markdownState = markdownState,
                modifier = Modifier.fillMaxSize(),
                components = markdownComponents(
                    checkbox = { model ->
                        val node = model.node
                        val asParsed = model.content
                            .substring(node.startOffset, node.endOffset)
                            .contains('x', ignoreCase = true)

                        Checkbox(
                            // padding before size: the other way round the gap
                            // would be taken out of the box instead of beside it
                            modifier = Modifier
                                .padding(end = CheckBoxTextGap)
                                .size(CheckBoxSize),
                            checked = ticked[node.startOffset] ?: asParsed,
                            onCheckedChange = { checked ->
                                ticked[node.startOffset] = checked
                                vm.toggleCheckBox(node.startOffset, node.endOffset)
                            },
                        )
                    }
                ),
                loading = { modifier ->
                    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                success = { state, components, modifier ->
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        state = readScrollState,
                        contentPadding = PaddingValues(15.dp),
                    ) {
                        items(state.node.children) { node ->
                            MarkdownElement(
                                node = node,
                                components = components,
                                content = state.content,
                            )
                        }
                    }
                }
            )
        }
    } else {
        GenericTextField(
            vm = vm,
            textFocusRequester = textFocusRequester,
            textContent = textContent,
            scrollState = writeScrollState,
        )
    }
}


@Composable
fun TextFormatRow(
    vm: MarkDownVM,
    modifier: Modifier = Modifier,
    textFormatExpanded: MutableState<Boolean>
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SmallButton(
            onClick = { vm.onTitle() },
            imageVector = Icons.Default.Title,
            contentDescription = "title"
        )

        SmallButton(
            onClick = { vm.onBold() },
            imageVector = Icons.Default.FormatBold,
            contentDescription = "bold"
        )
        SmallButton(
            onClick = { vm.onItalic() },
            imageVector = Icons.Default.FormatItalic,
            contentDescription = "italic"
        )

        SmallSeparator()

        SmallButton(
            onClick = { vm.onLink() },
            imageVector = Icons.Default.Link,
            contentDescription = "link"
        )

        SmallButton(
            onClick = { vm.onCode() },
            imageVector = Icons.Default.Code,
            contentDescription = "code"
        )
        SmallButton(
            onClick = { vm.onQuote() },
            imageVector = Icons.Default.FormatQuote,
            contentDescription = "quote"
        )

        SmallSeparator()

        SmallButton(
            onClick = { vm.onUnorderedList() },
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = "unordered list"
        )
        SmallButton(
            onClick = { vm.onNumberedList() },
            imageVector = Icons.Default.FormatListNumbered,
            contentDescription = "list number"
        )
        SmallButton(
            onClick = { vm.onTaskList() },
            imageVector = Icons.Default.Checklist,
            contentDescription = "checklist"
        )


        SmallSeparator()

        SmallButton(
            onClick = {
                textFormatExpanded.value = false
            },
            imageVector = Icons.Default.Close,
            contentDescription = "close"
        )
    }
}