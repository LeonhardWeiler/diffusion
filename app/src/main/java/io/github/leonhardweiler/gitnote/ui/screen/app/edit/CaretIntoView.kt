package io.github.leonhardweiler.gitnote.ui.screen.app.edit

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import kotlin.math.roundToInt

/** How long the caret is left alone after the field takes focus. */
private const val KEEP_SCROLL_FRAMES = 4

/**
 * What "make the caret visible" does in the note editor.
 *
 * The field asks for it, and until now the scrolling column around it answered.
 * That answer was wrong exactly once, and it was the one that showed: a field
 * that is given focus asks straight away, and the caret it asks about is still
 * the one the note was opened with — the first line — because the tap that
 * moves it is applied a frame or two later. The column scrolled to the top for
 * that, and the place being read was put back afterwards, which is the jump
 * that was left over.
 *
 * Answered here instead, the wrong ask can simply be ignored: [hold] takes the
 * next few frames out, and nothing moves while the tap is still on its way.
 *
 * @param scrollState the column's, not the field's — see [GenericTextField].
 */
internal class CaretScroller(private val scrollState: ScrollState) {

    /** Counted down frame by frame; see [holdForOneFrame]. */
    var heldFrames by mutableIntStateOf(0)
        private set

    /** Leaves the caret alone until the tap that placed it has been applied. */
    fun hold() {
        heldFrames = KEEP_SCROLL_FRAMES
    }

    fun holdForOneFrame() {
        if (heldFrames > 0) heldFrames--
    }

    /**
     * Moves the column so that [caret] can be seen, and no further than that.
     *
     * The rect arrives in the field's own coordinates, and the field is the
     * only child of the column and sits at its top — so what it says is where
     * the caret stands in the scrolled content.
     */
    suspend fun bringIntoView(caret: Rect) {
        if (heldFrames > 0) return

        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val top = scrollState.value
        val target = when {
            caret.top < top -> caret.top
            caret.bottom > top + viewport -> caret.bottom - viewport
            // already there, and moving anyway is the other way of being wrong
            else -> return
        }

        scrollState.scrollTo(target.roundToInt().coerceIn(0, scrollState.maxValue))
    }
}

/**
 * Hands every request to make something inside visible to [scroller] instead of
 * to whatever scrolls above. Nothing is passed on: the column must not answer
 * these, that is the whole point.
 */
internal fun Modifier.caretIntoView(scroller: CaretScroller): Modifier =
    this then CaretIntoViewElement(scroller)

private data class CaretIntoViewElement(
    val scroller: CaretScroller,
) : ModifierNodeElement<CaretIntoViewNode>() {

    override fun create() = CaretIntoViewNode(scroller)

    override fun update(node: CaretIntoViewNode) {
        node.scroller = scroller
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "caretIntoView"
        properties["scroller"] = scroller
    }
}

private class CaretIntoViewNode(
    var scroller: CaretScroller,
) : Modifier.Node(), BringIntoViewModifierNode {

    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        val bounds = boundsProvider() ?: return
        val here = requireLayoutCoordinates()
        scroller.bringIntoView(bounds.translate(here.localPositionOf(childCoordinates, Offset.Zero)))
    }
}
