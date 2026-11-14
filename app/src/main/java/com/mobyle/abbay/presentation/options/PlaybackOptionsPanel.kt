package com.mobyle.abbay.presentation.options

import com.player.PlaybackOptions

class PlaybackOptionsPanel(
    val options: PlaybackOptions = PlaybackOptions(),
    val onChange: (PlaybackOptions) -> Unit = {},
    val onSetLoopPoint: (isStart: Boolean) -> Unit = {},
)
