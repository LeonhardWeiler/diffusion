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

/**
 * What "make the caret visible" does in the note editor.
 *
 * It is not answered from the requests the field sends out. Those arrive
 * whenever the field feels like it — one comes with the focus, before the tap
 * that placed the caret has been applied — and each carries a rect one cannot
 * ask what it is about. Three rounds of guessing which of them to believe ended
 * in the same jump to the first line.
 *
 * So the caret is not received here, it is worked out: the field's own
 * [TextLayoutResult] says where a given offset stands, and the offset is the
 * selection the view model holds. Every scroll below is therefore a function of
 * where the caret is now, where the column stands and how much of it is left —
 * and there is no frame at which a stale answer could arrive.
 *
 * The field's own requests are swallowed instead (see [caretIntoView]), because
 * the column above must not answer them either.
 *
 * @param scrollState the column's, not the field's — see [GenericTextField].
 */
internal class CaretScroller(private val scrollState: ScrollState) {

    /**
     * The offset the last scroll was about, and the whole of what makes taking
     * focus quiet.
     *
     * A field that is given focus has the caret the note was opened with — the
     * first line — and the tap that moves it is applied a frame or two later.
     * That offset is written in here by [focusGained] without anything being
     * scrolled, so the first thing this ever acts on is the tap itself.
     */
    private var handledOffset: Int? = null

    /** Takes the caret as it stands now, without moving to it. */
    fun focusGained(offset: Int) {
        handledOffset = offset
    }

    fun focusLost() {
        handledOffset = null
    }

    /**
     * Moves the column so that the caret at [offset] can be seen, and no
     * further than that.
     *
     * This is what carries the view along while somebody writes: every
     * keystroke moves the caret, and typing onto a line that would be the first
     * one off the bottom of the screen brings it up instead of leaving the
     * writing to happen out of sight.
     *
     * A line of room is left on either side. A caret pressed against the very
     * edge is one you cannot read the line under, and every further character
     * would ask for the same scroll again — so it goes one line past what it
     * strictly needs, and the next few keystrokes cost nothing.
     */
    suspend fun caretMoved(offset: Int, caret: Rect) {
        if (offset == handledOffset) return
        handledOffset = offset

        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val target = scrollTargetFor(caret, scrollState.value, viewport) ?: return

        scrollTo(target)
    }

    /**
     * Puts the caret in the middle of what is left of the screen, if the
     * keyboard has just covered where it was.
     *
     * This is what the first tap into a note ends in: the tap lands on a line
     * that can be seen, so nothing is scrolled for it, the keyboard then
     * arrives and takes half the view with it, and the line that was tapped is
     * behind it. Bringing it just far enough would leave it lying on the
     * keyboard, so it goes to the middle — where the next few lines of what is
     * being written are visible too.
     *
     * Only when it is really out of sight. A caret the keyboard did not reach
     * stays exactly where it is: the note moving for no reason is the thing
     * this was all about.
     */
    suspend fun keepCaretVisible(caret: Rect) {
        val viewport = scrollState.viewportSize
        if (viewport <= 0) return

        val target = centreTargetFor(caret, scrollState.value, viewport) ?: return

        scrollTo(target)
    }

    /** Instantly, never animated: this is a correction, not a movement. */
    private suspend fun scrollTo(target: Float) {
        scrollState.scrollTo(target.roundToInt().coerceIn(0, scrollState.maxValue))
    }
}

/**
 * Where the caret stands in the scrolled column, or null while the field has
 * not been laid out yet.
 *
 * The field is the only child of the column and sits at its top, so the only
 * thing between the two coordinate systems is the padding the field draws its
 * text inside — [textTop], which the caller reads from the same defaults the
 * reading mode is padded with.
 */
internal fun caretRectOf(
    layout: TextLayoutResult?,
    selection: TextRange,
    textTop: Float,
): Rect? {
    if (layout == null) return null

    val offset = selection.start.coerceIn(0, layout.layoutInput.text.length)

    return layout.getCursorRect(offset).translate(0f, textTop)
}

/**
 * Where the column has to stand for [caret] to be readable, or null when it
 * already is — and a caret one can see is not one to move the note for.
 *
 * A line of the caret's own height is left on either side. A caret pressed
 * against the very edge is one you cannot read the line under, and every
 * further character would ask for the same scroll again.
 */
internal fun scrollTargetFor(caret: Rect, top: Int, viewport: Int): Float? {
    val margin = caret.height

    return when {
        caret.top - margin < top -> caret.top - margin
        caret.bottom + margin > top + viewport -> caret.bottom + margin - viewport
        else -> null
    }
}

/**
 * Where it has to stand for [caret] to sit in the middle of the viewport, or
 * null when the caret can be seen at all — this is the correction for a
 * keyboard that has just covered it, and bringing it just far enough would
 * leave it lying on the keys.
 */
internal fun centreTargetFor(caret: Rect, top: Int, viewport: Int): Float? {
    if (caret.top >= top && caret.bottom <= top + viewport) return null

    return caret.center.y - viewport / 2f
}

/**
 * Swallows every request to make something inside visible.
 *
 * Nothing is passed on and nothing is done: where the caret has to be is worked
 * out from the text layout instead (see [CaretScroller]), and the column above
 * must not answer these either — a field that takes focus asks for the caret
 * the note was opened with, and the column scrolled to the top of the note for
 * it.
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
        // the request stops here
    }
}
