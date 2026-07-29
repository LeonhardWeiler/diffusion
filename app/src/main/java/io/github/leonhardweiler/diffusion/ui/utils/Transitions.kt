package io.github.leonhardweiler.diffusion.ui.utils

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * For a move that has no direction to it — a screen being replaced, not entered.
 *
 * Written out rather than composed with `togetherWith`, for the one thing that
 * hands out by default: a [androidx.compose.animation.SizeTransform]. See below.
 */
fun crossFade() = ContentTransform(
    targetContentEnter = fadeIn(tween()),
    initialContentExit = fadeOut(tween()),
    sizeTransform = null,
)

/**
 * Going one screen further in, or with [backWard], one screen back out.
 *
 * Nothing about the size is animated. Every screen fills the window, so between
 * two of them there is no size to animate — except while the keyboard is up,
 * where the one being left is as tall as the window and the one arriving is not.
 * The default animates the second from the height of the first, which reads as
 * the new screen sliding in and lifting at the same time.
 */
fun slide(backWard: Boolean = false) = ContentTransform(
    targetContentEnter = slideInHorizontally(
        initialOffsetX = {
            if (backWard) -it else it
        }
    ),
    initialContentExit = slideOutHorizontally(
        targetOffsetX = {
            if (backWard) it else -it
        }
    ),
    sizeTransform = null,
)

/** Applies [modifier] only when [condition] holds, and nothing otherwise. */
@Composable
fun Modifier.conditional(
    condition: Boolean,
    modifier: @Composable Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
