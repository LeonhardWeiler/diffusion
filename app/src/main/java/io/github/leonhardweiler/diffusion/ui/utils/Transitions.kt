package io.github.leonhardweiler.diffusion.ui.utils

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** For a move that has no direction to it — a screen being replaced, not entered. */
fun crossFade() = fadeIn(tween()) togetherWith fadeOut(tween())

/** Going one screen further in, or with [backWard], one screen back out. */
fun slide(backWard: Boolean = false) = slideInHorizontally(
    initialOffsetX = {
        if (backWard) -it else it
    }
) togetherWith slideOutHorizontally(
    targetOffsetX = {
        if (backWard) it else -it
    }
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
