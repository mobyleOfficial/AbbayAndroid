package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.player.PlaybackStatus
import com.player.PlayerState

enum class PlayingState {
    PAUSED,
    PLAYING,
    LOADING,
}

fun PlayingState.toIcon(): ImageVector? = when (this) {
    PlayingState.PLAYING -> Icons.Rounded.Pause
    PlayingState.PAUSED -> Icons.Rounded.PlayArrow
    PlayingState.LOADING -> null
}

@Composable
fun PlayingState.label(): String = stringResource(if (this == PlayingState.PLAYING) R.string.player_pause else R.string.player_play)

/** The play button's state: loading while a play request waits for the player, playing once it plays. */
fun playingStateOf(playerState: PlayerState, isAwaitingPlayback: Boolean): PlayingState = when {
    isAwaitingPlayback -> PlayingState.LOADING
    playerState.isPlaying && playerState.status == PlaybackStatus.READY -> PlayingState.PLAYING
    else -> PlayingState.PAUSED
}
