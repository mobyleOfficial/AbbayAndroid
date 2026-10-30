package com.mobyle.abbay.presentation.options

import com.player.PlaybackOptions

/** What the player needs to show and change the repeat and skip options; built from [PlaybackOptionsViewModel]. */
class PlaybackOptionsPanel(
    val options: PlaybackOptions = PlaybackOptions(),
    val onChange: (PlaybackOptions) -> Unit = {},
    val onSetLoopPoint: (isStart: Boolean) -> Unit = {},
)
