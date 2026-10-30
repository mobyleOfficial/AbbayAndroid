package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors

/**
 * Book cover with the player gestures: tap plays or pauses, double-tap on the left or right
 * half skips back or forward, a horizontal swipe goes to the previous or next chapter (only when [gesturesEnabled]).
 */
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
) {
    // The cover grows from the mini player to the full player, but Coil sizes a request once, from the first layout:
    // it is asked for the largest size the cover can take (the screen's shorter side), not the mini player's.
    val configuration = LocalConfiguration.current
    val coverSizePx = with(LocalDensity.current) { minOf(configuration.screenWidthDp, configuration.screenHeightDp).dp.roundToPx() }
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            contentScale = ContentScale.FillBounds,
            model = ImageRequest.Builder(LocalContext.current).data(thumbnail)
                .size(coverSizePx)
                // Exact size: otherwise Coil's memory cache may hand back the small decode of the list thumbnail.
                .precision(Precision.EXACT)
                .fallback(R.drawable.file_music)
                .error(R.drawable.file_music)
                .crossfade(true).build(),
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(percent = 10)),
            contentDescription = null,
            // The fallback icon follows the theme so it stays visible and quiet behind the play icon.
            colorFilter = if (thumbnail == null) ColorFilter.tint(AbbayColors.content.copy(alpha = FALLBACK_ALPHA)) else null,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(percent = 10))
                .background(if (thumbnail != null) Color.Black.copy(alpha = 0.2f) else Color.Transparent)
                .playerGestures(
                    gesturesEnabled,
                    PlayerGestureHandlers(onTap, onDoubleTapLeft, onDoubleTapRight, onSwipeToPrevious, onSwipeToNext),
                ),
        ) {
            playerIcon.toIcon()?.let { icon ->
                Icon(
                    icon,
                    contentDescription = playerIcon.label(),
                    // White reads on a cover image under the scrim; without a cover the icon sits on the theme surface.
                    tint = if (thumbnail != null) Color.White else AbbayColors.content,
                    modifier = Modifier.align(Alignment.Center),
                )
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
private const val FALLBACK_ALPHA = 0.3f
