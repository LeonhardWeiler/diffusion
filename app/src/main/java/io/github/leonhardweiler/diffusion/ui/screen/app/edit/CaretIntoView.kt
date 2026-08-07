package io.github.leonhardweiler.diffusion.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.roundToInt

internal class CaretScroller(private val scrollState: ScrollState) {
    private var handledOffset: Int? = null

    fun focusGained(offset: Int) {
        handledOffset = offset
    }

    fun focusLost() {
        handledOffset = null
    }

    suspend fun caretMoved(offset: Int, caret: Rect) {
        if (offset == handledOffset) return
        handledOffset = offset

        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val target = scrollTargetFor(caret, scrollState.value, viewport) ?: return

        scrollTo(target)
    }

    suspend fun keepCaretVisible(caret: Rect) {
        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val target = centreTargetFor(caret, scrollState.value, viewport) ?: return

        scrollTo(target)
    }

    private suspend fun scrollTo(target: Float) {
        scrollState.scrollTo(target.roundToInt().coerceIn(0, scrollState.maxValue))
    }
}

internal fun caretRectOf(
    layout: TextLayoutResult?,
    selection: TextRange,
    textTop: Float,
): Rect? {
    if (layout == null) return null

    val offset = selection.start.coerceIn(0, layout.layoutInput.text.length)

    return layout.getCursorRect(offset).translate(0f, textTop)
}

internal fun scrollTargetFor(caret: Rect, top: Int, viewport: Int): Float? {
    val margin = caret.height

    return when {
        caret.top - margin < top -> caret.top - margin
        caret.bottom + margin > top + viewport -> caret.bottom + margin - viewport
        else -> null
    }
}

internal fun centreTargetFor(caret: Rect, top: Int, viewport: Int): Float? {
    if (caret.top >= top && caret.bottom <= top + viewport) return null

    return caret.center.y - viewport / 2f
}

/**
 * Swallows every bringIntoView request the field sends out, so that where the
 * caret is, is worked out here rather than received: the request arriving with
 * the focus is about the caret as the note was opened, and telling that one
 * apart from the tap that follows a frame later never worked.
 */
internal fun Modifier.caretIntoView(): Modifier = this then CaretIntoViewElement

private data object CaretIntoViewElement : ModifierNodeElement<CaretIntoViewNode>() {
    override fun create() = CaretIntoViewNode()

    override fun update(node: CaretIntoViewNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "caretIntoView"
    }
}

private class CaretIntoViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
    }
}
