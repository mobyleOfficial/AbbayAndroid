package com.player

data class NowPlaying(
    val bookTitle: String,
    val chapterTitle: String,
    val coverUri: String?,
    val isPlaying: Boolean,
    val chapterProgress: Float,
    val isSleepTimerRunning: Boolean,
)

interface NowPlayingPublisher {
    suspend fun publish(nowPlaying: NowPlaying?)
}
