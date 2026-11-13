package com.mobyle.abbay.presentation.readalong

import com.mobyle.abbay.presentation.booklist.widgets.player.PlayingState

/** What the read-along text needs from the player: where to seek, and its play/pause for the full screen. */
class ReadAlongPlayback(
    val playerIcon: PlayingState,
    val onSeekToBookPosition: (Long) -> Unit,
    val onPlayPause: () -> Unit,
)
