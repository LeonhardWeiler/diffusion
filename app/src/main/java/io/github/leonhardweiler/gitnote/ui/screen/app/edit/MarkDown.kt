package io.github.leonhardweiler.gitnote.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTableBasicText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import io.github.leonhardweiler.gitnote.ui.viewmodel.edit.MarkDownVM
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

private val CheckBoxSize = 20.dp

/** Between the box and the words belonging to it, which otherwise touch. */
private val CheckBoxTextGap = 8.dp

/** The most a column is given before what stands in it is made to wrap. */
private val MaxTableColumnWidth = 280.dp

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
    //
    // It starts empty when the note is opened to be written in, because then
    // nobody has asked to read it: parsing the whole of a long note anyway is
    // the wait between tapping a row and being able to type in it.
    var readText by remember {
        mutableStateOf(if (isReadOnlyModeActive) textContent.text else "")
    }

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
                    table = { model -> WideTable(model) },
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
                        items(
                            items = state.node.children,
                            // Where a block starts names it for as long as the
                            // text stands still, which it does here — the
                            // reader parses once and is not typed into. Without
                            // it a block is known by its position in the list,
                            // so scrolling threw away and rebuilt blocks that
                            // had not changed at all.
                            key = { node -> node.startOffset },
                            // A paragraph and a heading lay out differently, two
                            // paragraphs do not: saying which is which lets one
                            // that scrolled off the top be filled in again with
                            // the one coming in at the bottom instead of built
                            // from nothing.
                            contentType = { node -> node.type },
                        ) { node ->
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


/**
 * A table whose columns are as wide as what stands in them.
 *
 * The renderer's own table gives every column the same share of the screen and
 * cuts whatever does not fit on one line off with an ellipsis, so a column of
 * dates and a column of sentences were given the same room and neither could be
 * read. Here every column is measured by its widest cell, text too long for the
 * widest a column may get wraps instead of disappearing, and a table wider than
 * the screen is moved sideways rather than squeezed into it.
 */
@Composable
private fun WideTable(model: MarkdownComponentModel) {

    // The header is the first of the rows, not something beside them: it is laid
    // out with the same columns and only reads differently.
    val rows = remember(model.node) {
        model.node.children
            .filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
            .map { row -> row.children.filter { it.type == GFMTokenTypes.CELL } }
    }

    val columnCount = remember(rows) { rows.maxOfOrNull { it.size } ?: 0 }

    if (columnCount == 0) return

    val cellPadding = LocalMarkdownDimens.current.tableCellPadding
    val cornerSize = LocalMarkdownDimens.current.tableCornerSize
    val background = LocalMarkdownColors.current.tableBackground
    val style = model.typography.table

    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Layout(
            modifier = Modifier.background(background, RoundedCornerShape(cornerSize)),
            content = {
                rows.forEachIndexed { rowIndex, cells ->
                    // every row lays out the same number of cells, so that a
                    // short one does not shift the columns of the whole table
                    repeat(columnCount) { columnIndex ->
                        Box(modifier = Modifier.padding(cellPadding)) {
                            cells.getOrNull(columnIndex)?.let { cell ->
                                MarkdownTableBasicText(
                                    content = model.content,
                                    cell = cell,
                                    style = if (rowIndex == 0) {
                                        style.copy(fontWeight = FontWeight.Bold)
                                    } else {
                                        style
                                    },
                                    maxLines = Int.MAX_VALUE,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                    }
                }

                // last, so that the cells stay one block of children the index
                // arithmetic below can walk
                HorizontalDivider()
            }
        ) { measurables, constraints ->

            val cells = measurables.subList(0, measurables.size - 1)
            val divider = measurables.last()

            val maxColumnWidth = MaxTableColumnWidth.roundToPx()

            // what the widest cell of a column asks for, up to the point where
            // one long sentence would push the rest of the table off the screen
            val columnWidths = IntArray(columnCount) { column ->
                rows.indices.maxOf { row ->
                    cells[row * columnCount + column].maxIntrinsicWidth(Constraints.Infinity)
                }.coerceAtMost(maxColumnWidth)
            }

            val tableWidth = columnWidths.sum()

            // measured again against the column they ended up in: a cell that
            // was given less than it asked for wraps, and only then is its
            // height known
            val placeables = cells.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixedWidth(columnWidths[index % columnCount]))
            }

            val rowHeights = IntArray(rows.size) { row ->
                (0 until columnCount).maxOf { column ->
                    placeables[row * columnCount + column].height
                }
            }

            val dividerPlaceable = divider.measure(Constraints.fixedWidth(tableWidth))

            layout(
                width = tableWidth.coerceAtLeast(constraints.minWidth),
                height = rowHeights.sum() + dividerPlaceable.height
            ) {
                var y = 0
                rowHeights.forEachIndexed { row, rowHeight ->
                    var x = 0
                    columnWidths.forEachIndexed { column, columnWidth ->
                        placeables[row * columnCount + column].place(x, y)
                        x += columnWidth
                    }
                    y += rowHeight

                    // under the header, where the line of dashes stands in the
                    // note itself
                    if (row == 0) {
                        dividerPlaceable.place(0, y)
                        y += dividerPlaceable.height
                    }
                }
            }
        }
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