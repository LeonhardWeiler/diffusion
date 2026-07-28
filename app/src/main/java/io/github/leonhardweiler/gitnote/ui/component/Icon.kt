package io.github.leonhardweiler.gitnote.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [contentDescription] has no default on purpose: it is what a screen reader
 * announces, and an icon button without it is a nameless button. Pass null only
 * for icons that decorate something already labelled next to them.
 */
@Composable
fun SimpleIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = LocalContentColor.current
) {
    Icon(
        modifier = modifier,
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}