package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.common.widgets.StreamingBadge
import com.mobyle.abbay.presentation.common.widgets.coverCornerRadius

@Composable
fun PlayerCover(
    thumbnail: String?,
    playerIcon: PlayingState,
    gesturesEnabled: Boolean,
    onTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    onSwipeToNext: () -> Unit,
    modifier: Modifier = Modifier,
    isStreamingOnly: Boolean = false,
    alignment: Alignment = Alignment.Center,
) {
    val configuration = LocalConfiguration.current
    val coverSizePx = with(LocalDensity.current) { minOf(configuration.screenWidthDp, configuration.screenHeightDp).dp.roundToPx() }
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        val side = minOf(maxWidth, maxHeight)
        val shape = RoundedCornerShape(coverCornerRadius(side))
        Box(modifier = Modifier.size(side)) {
            BookThumbnail(thumbnail, modifier = Modifier.fillMaxSize(), shadow = side > SHADOW_MIN_SIDE, requestSizePx = coverSizePx)
            if (isStreamingOnly && side >= BADGE_MIN_SIDE) {
                StreamingBadge(size = (side * BADGE_FRACTION).coerceIn(BADGE_MIN_SIZE, BADGE_MAX_SIZE), inset = side * BADGE_INSET_FRACTION)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .playerGestures(
                        gesturesEnabled,
                        PlayerGestureHandlers(onTap, onDoubleTapLeft, onDoubleTapRight, onSwipeToPrevious, onSwipeToNext),
                    ),
            ) {
                playerIcon.toIcon()?.let { icon ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(minOf(side * HINT_FRACTION, HINT_MAX_SIDE))
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = HINT_DISC_ALPHA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = playerIcon.label(),
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(HINT_ICON_FRACTION),
                        )
                    }
                }
            }
        }
    }
}

private class PlayerGestureHandlers(
    val onTap: () -> Unit,
    val onDoubleTapLeft: () -> Unit,
    val onDoubleTapRight: () -> Unit,
    val onSwipeToPrevious: () -> Unit,
    val onSwipeToNext: () -> Unit,
)

private fun Modifier.playerGestures(gesturesEnabled: Boolean, handlers: PlayerGestureHandlers): Modifier = composed {
    val current = rememberUpdatedState(handlers)
    pointerInput(gesturesEnabled) {
        detectTapGestures(
            onTap = { current.value.onTap() },
            onDoubleTap = { offset ->
                when {
                    !gesturesEnabled -> current.value.onTap()
                    offset.x < size.width / 2 -> current.value.onDoubleTapLeft()
                    else -> current.value.onDoubleTapRight()
                }
            },
        )
    }.pointerInput(gesturesEnabled) {
        if (!gesturesEnabled) return@pointerInput
        var dragged = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onHorizontalDrag = { _, amount -> dragged += amount },
            onDragEnd = {
                val threshold = SWIPE_THRESHOLD_DP.dp.toPx()
                when {
                    dragged > threshold -> current.value.onSwipeToPrevious()
                    dragged < -threshold -> current.value.onSwipeToNext()
                }
            },
        )
    }
}

private const val SWIPE_THRESHOLD_DP = 64
private val SHADOW_MIN_SIDE = 120.dp
private val HINT_MAX_SIDE = 56.dp
private const val HINT_FRACTION = 0.6f
private const val HINT_ICON_FRACTION = 0.6f
private const val HINT_DISC_ALPHA = 0.35f

private val BADGE_MIN_SIDE = 120.dp
private val BADGE_MIN_SIZE = 24.dp
private val BADGE_MAX_SIZE = 40.dp
private const val BADGE_FRACTION = 0.1f
private const val BADGE_INSET_FRACTION = 0.035f
