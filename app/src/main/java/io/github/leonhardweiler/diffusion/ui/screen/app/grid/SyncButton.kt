package io.github.leonhardweiler.diffusion.ui.screen.app.grid

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.manager.SyncState
import io.github.leonhardweiler.diffusion.manager.SyncState.Idle
import io.github.leonhardweiler.diffusion.manager.SyncState.Ok
import io.github.leonhardweiler.diffusion.manager.SyncState.Pull
import io.github.leonhardweiler.diffusion.manager.SyncState.Push
import io.github.leonhardweiler.diffusion.manager.SyncState.Starting

private val ChangeDotSize = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncButton(
    state: SyncState,
    hasLocalChanges: Boolean,
    onClick: () -> Unit,
) {
    var iconModifier: Modifier = Modifier

    if (state.isLoading()) {
        val infiniteTransition = rememberInfiniteTransition()
        val alpha = infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500),
                repeatMode = RepeatMode.Reverse
            )
        )

        iconModifier = iconModifier.alpha(alpha.value)
    }

    val tooltipState = rememberTooltipState(isPersistent = true)

    if (state is SyncState.Error && state.announce) {
        LaunchedEffect(state) {
            tooltipState.show()
        }
    }

    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text(
                    if (hasLocalChanges) {
                        stringResource(R.string.sync_not_committed, state.message())
                    } else {
                        state.message()
                    }
                )
            }
        },
        state = tooltipState,
        enableUserInput = true,
    ) {
        IconButton(
            modifier = Modifier.size(ButtonSize),
            enabled = !state.isLoading(),
            onClick = onClick,
        ) {
            val icon = when (state) {
                is SyncState.Error -> painterResource(R.drawable.cloud_alert_24px)
                Idle, Starting -> rememberVectorPainter(Icons.Default.CloudSync)
                Ok -> rememberVectorPainter(Icons.Default.CloudDone)
                Pull -> rememberVectorPainter(Icons.Default.CloudDownload)
                Push -> rememberVectorPainter(Icons.Default.CloudUpload)
            }

            Box {
                Icon(
                    painter = icon,
                    contentDescription = if (hasLocalChanges) {
                        stringResource(
                            R.string.sync_not_committed,
                            stringResource(R.string.sync_with_remote)
                        )
                    } else {
                        stringResource(R.string.sync_with_remote)
                    },
                    modifier = iconModifier,
                )

                if (hasLocalChanges && !state.isLoading()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(ChangeDotSize)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
