package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

/**
 * The small amount of arithmetic every editing action needs. Kept apart because
 * an off-by-one here is invisible in the middle of a formatting rule.
 */

/** Where the line holding [index] begins. */
fun String.lineStartAt(index: Int): Int =
    lastIndexOf('\n', startIndex = index).let { if (it == -1) 0 else it + 1 }

/** Where the line holding [index] ends, the newline itself not counted. */
fun String.lineEndAt(index: Int): Int =
    indexOf('\n', startIndex = index).let { if (it == -1) length else it }
