package io.github.leonhardweiler.diffusion

import androidx.compose.ui.geometry.Rect
import io.github.leonhardweiler.diffusion.ui.screen.app.edit.centreTargetFor
import io.github.leonhardweiler.diffusion.ui.screen.app.edit.scrollTargetFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The three rules the editor's scrolling is made of, without a screen.
 *
 * A caret is one line high; the numbers below are pixels in the scrolled
 * content, and the viewport is 500 of them.
 */
class CaretScrollerTest {

    private val lineHeight = 20f
    private val viewport = 500

    private fun caretAt(top: Float) = Rect(0f, top, 2f, top + lineHeight)

    @Test
    fun a_caret_one_can_already_see_is_not_moved() {
        assertNull(scrollTargetFor(caretAt(300f), top = 0, viewport = viewport))
        // right in the middle of a scrolled column
        assertNull(scrollTargetFor(caretAt(1200f), top = 1000, viewport = viewport))
    }

    @Test
    fun a_caret_above_the_view_comes_back_with_a_line_of_room() {
        // 990 is above a column standing at 1000
        assertEquals(970f, scrollTargetFor(caretAt(990f), top = 1000, viewport = viewport))
    }

    @Test
    fun a_caret_below_the_view_comes_back_with_a_line_of_room() {
        // its bottom is 1520, the view ends at 1500
        assertEquals(1040f, scrollTargetFor(caretAt(1500f), top = 1000, viewport = viewport))
    }

    @Test
    fun the_room_leaves_a_line_under_what_is_being_written() {
        val caret = caretAt(1500f)
        val target = scrollTargetFor(caret, top = 1000, viewport = viewport)!!

        // the view now ends a full line below the caret, so the line being
        // typed is not the one lying on the bottom edge
        assertEquals(lineHeight, (target + viewport) - caret.bottom)

        // and every further character on that line asks for nothing
        assertNull(scrollTargetFor(caret, top = target.toInt(), viewport = viewport))
    }

    @Test
    fun the_keyboard_correction_leaves_a_visible_caret_alone() {
        assertNull(centreTargetFor(caretAt(1200f), top = 1000, viewport = viewport))
    }

    @Test
    fun a_caret_the_keyboard_covered_goes_to_the_middle() {
        // the view is 1000..1500 and the caret sits below it
        val target = centreTargetFor(caretAt(1600f), top = 1000, viewport = viewport)!!

        // its centre is 1610, so the column stands 250 above that
        assertEquals(1360f, target)
        // and from there the caret is in the middle of what is left
        assertEquals(1610f, target + viewport / 2f)
    }
}
