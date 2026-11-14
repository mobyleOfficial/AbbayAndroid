package com.player

data class PlayerState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrackIndex: Int = 0,
    val trackCount: Int = 0,
    val speed: Float = 1f,
    val sleepTimer: SleepTimerState = SleepTimerState.Off,
    val bookId: String? = null,
    val upNext: UpNext? = null,
    val isCasting: Boolean = false,
)
