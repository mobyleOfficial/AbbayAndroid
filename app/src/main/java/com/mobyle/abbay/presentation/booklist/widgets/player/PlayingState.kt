package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R

enum class PlayingState {
    PAUSED,
    PLAYING,
    LOADING,
}

fun PlayingState.toIcon(): ImageVector? = when (this) {
    PlayingState.PLAYING -> Icons.Default.Pause
    PlayingState.PAUSED -> Icons.Default.PlayArrow
    PlayingState.LOADING -> null
}

@Composable
fun PlayingState.label(): String = stringResource(if (this == PlayingState.PLAYING) R.string.player_pause else R.string.player_play)
