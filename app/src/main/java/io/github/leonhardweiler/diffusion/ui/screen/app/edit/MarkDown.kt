package io.github.leonhardweiler.diffusion.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material3.TextFieldDefaults
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTableBasicText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.MarkDownVM
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

private val CheckBoxSize = 20.dp

private val CheckBoxTextGap = 8.dp

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
    // the parser shows nothing while it runs, so what it is given only follows
    // the note when the reading mode is entered — and the state lives out here
    // so that it survives switching to writing and back
    var readText by remember {
        mutableStateOf(if (isReadOnlyModeActive) textContent.text else "")
    }

    LaunchedEffect(isReadOnlyModeActive) {
        if (isReadOnlyModeActive) readText = textContent.text
    }

    val markdownState = rememberMarkdownState(readText)

    // a tick must not reach the parser: it is remembered per offset here and
    // written to the note separately, and "[ ]" and "[x]" are the same length
    // so the offsets stay valid
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
                        contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(),
                    ) {
                        items(
                            items = state.node.children,
                            key = { node -> node.startOffset },
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

@Composable
private fun WideTable(model: MarkdownComponentModel) {
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

                HorizontalDivider()
            }
        ) { measurables, constraints ->

            val cells = measurables.subList(0, measurables.size - 1)
            val divider = measurables.last()

            val maxColumnWidth = MaxTableColumnWidth.roundToPx()

            val columnWidths = IntArray(columnCount) { column ->
                rows.indices.maxOf { row ->
                    cells[row * columnCount + column].maxIntrinsicWidth(Constraints.Infinity)
                }.coerceAtMost(maxColumnWidth)
            }

            val tableWidth = columnWidths.sum()

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

                    if (row == 0) {
                        dividerPlaceable.place(0, y)
                        y += dividerPlaceable.height
                    }
                }
            }
        }
    }
}
