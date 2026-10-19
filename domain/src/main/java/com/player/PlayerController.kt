package com.player

import com.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Player command layer. The UI sends every playback action through this
 * interface and never talks to the media engine directly. The implementation lives in `:playback`.
 */
@Suppress("TooManyFunctions") // One function per player command.
interface PlayerController {
    val state: StateFlow<PlayerState>

    val errors: Flow<PlayerError>

    /** Connects to the playback service. Safe to call more than once. */
    fun connect()

    /** Disconnects from the playback service. [connect] can be called again later. */
    fun release()

    /**
     * Loads [book] at its saved track and position. Playback starts as soon as the media is ready
     * when [playWhenReady] is true.
     */
    fun openBook(book: Book, playWhenReady: Boolean)

    fun play()

    fun pause()

    fun playPause()

    /** Seeks inside the current track. */
    fun seekTo(positionMs: Long)

    fun seekTo(trackIndex: Int, positionMs: Long)

    /** Moves by [deltaMs] (signed) in book time, crossing file boundaries and clamping to the book. */
    fun seekBy(deltaMs: Long)

    fun seekToBookPosition(bookPositionMs: Long)

    fun nextTrack()

    /** Restarts the current track when more than 3 s in, otherwise goes to the previous track. */
    fun previousTrack()

    /** Chapters are the tracks until chapter parsing exists. */
    fun nextChapter()

    fun previousChapter()

    fun setSpeed(speed: Float)

    /** Starts or replaces the sleep timer; it runs in the playback service. */
    fun startSleepTimer(mode: SleepTimerMode)

    fun cancelSleepTimer()

    /** Stops the countdown to the next queued book; the book stays first in the queue. */
    fun cancelUpNext()

    fun stop()

    /** Position inside the current track, read from the player at call time. */
    fun currentPositionMs(): Long

    /** Index of the current track, read from the player at call time. */
    fun currentTrackIndex(): Int

    /** Position from the start of the book. */
    fun bookPositionMs(): Long
}
