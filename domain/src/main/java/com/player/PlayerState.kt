package com.player

/** Snapshot of the player, published by [PlayerController.state]. */
data class PlayerState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrackIndex: Int = 0,
    val trackCount: Int = 0,
    val speed: Float = 1f,
    val sleepTimer: SleepTimerState = SleepTimerState.Off,
    /** Id of the book loaded in the player, or null. */
    val bookId: String? = null,
    /** The queued book about to start, while its countdown runs. */
    val upNext: UpNext? = null,
    /** True while the book plays on a Cast receiver: effects are not applied. */
    val isCasting: Boolean = false,
)
