package io.github.leonhardweiler.diffusion.ui.utils

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// written out rather than built with togetherWith, which hands out a
// SizeTransform: every screen fills the window, so there is nothing to animate
fun crossFade() = ContentTransform(
    targetContentEnter = fadeIn(tween()),
    initialContentExit = fadeOut(tween()),
    sizeTransform = null,
)

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
