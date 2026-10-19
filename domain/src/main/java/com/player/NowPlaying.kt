package com.player

/** What the home screen widgets show. */
data class NowPlaying(
    val bookTitle: String,
    val chapterTitle: String,
    val coverUri: String?,
    val isPlaying: Boolean,
    /** Position in the current chapter, 0…1. */
    val chapterProgress: Float,
    val isSleepTimerRunning: Boolean,
)

/** Receives the playing book for the widgets; implemented by `:widget`, called by the playback service. */
interface NowPlayingPublisher {
    /** [nowPlaying] is null when no book is loaded. */
    suspend fun publish(nowPlaying: NowPlaying?)
}
