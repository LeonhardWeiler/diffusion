package io.github.leonhardweiler.diffusion.ui.screen.app.edit

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
        caretAtFocus = null
    }

    fun holdForOneFrame() {
        if (heldFrames > 0) heldFrames--
    }

    /**
     * Where the caret last was, in the field's coordinates.
     *
     * The field is the only child of the column and sits at its top, so what
     * the rect says is where the caret stands in the scrolled content — and
     * that does not move when the keyboard takes half the screen away, which is
     * why it is worth keeping.
     */
    private var caret: Rect? = null

    /**
     * The rect of the ask that arrives with the focus, kept apart from [caret].
     *
     * That ask is about the caret as the note was opened — the first line —
     * because the tap that moves it is applied a frame or two later. Letting it
     * into [caret] was the whole of the jump that came back: it does not scroll
     * anything by itself, but the keyboard arriving right afterwards asks for
     * the remembered caret to be made visible, and that was the top of the note.
     *
     * It is still worth keeping for the one case where it is right: a tap that
     * lands where the caret already was changes no selection, so no second ask
     * ever comes, and this is then the only thing that knows which line to keep
     * out from under the keyboard.
     */
    private var caretAtFocus: Rect? = null

    /**
     * Moves the column so that [caret] can be seen, and no further than that.
     *
     * This is what carries the view along while somebody writes: every
     * keystroke asks again, and typing onto a line that would be the first one
     * off the bottom of the screen brings it up instead of leaving the writing
     * to happen out of sight.
     *
     * A line of room is left on either side. A caret pressed against the very
     * edge is one you cannot read the line under, and every further character
     * would ask for the same scroll again — so it goes one line past what it
     * strictly needs, and the next few keystrokes cost nothing.
     */
    suspend fun bringIntoView(caret: Rect) {
        if (heldFrames > 0) {
            caretAtFocus = caret
            return
        }

        this.caret = caret

        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val margin = caret.height
        val top = scrollState.value
        val target = when {
            caret.top - margin < top -> caret.top - margin
            caret.bottom + margin > top + viewport -> caret.bottom + margin - viewport
            // already there, and moving anyway is the other way of being wrong
            else -> return
        }

        scrollTo(target)
    }

    /**
     * Puts the caret in the middle of what is left of the screen, if the
     * keyboard has just covered where it was.
     *
     * This is what the first tap into a note ends in: the tap itself moves
     * nothing (see [hold]), the keyboard then arrives and takes half the view
     * with it, and the line that was tapped is behind it. Bringing it just far
     * enough would leave it lying on the keyboard, so it goes to the middle —
     * where the next few lines of what is being written are visible too.
     *
     * Only when it is really out of sight. A caret the keyboard did not reach
     * stays exactly where it is: the note moving for no reason is the thing
     * this was all about.
     */
    suspend fun keepCaretVisible() {
        val caret = caret ?: caretAtFocus ?: return

        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val top = scrollState.value
        if (caret.top >= top && caret.bottom <= top + viewport) return

        scrollTo(caret.center.y - viewport / 2f)
    }

    /** Instantly, never animated: this is a correction, not a movement. */
    private suspend fun scrollTo(target: Float) {
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
