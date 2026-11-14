package com.player

import com.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Suppress("TooManyFunctions")
interface PlayerController {
    val state: StateFlow<PlayerState>

    val errors: Flow<PlayerError>

    fun connect()

    fun release()

    fun openBook(book: Book, playWhenReady: Boolean)

    fun play()

    fun pause()

    fun playPause()

    fun seekTo(positionMs: Long)

    fun seekTo(trackIndex: Int, positionMs: Long)

    fun seekBy(deltaMs: Long)

    fun seekToBookPosition(bookPositionMs: Long)

    fun nextTrack()

    fun previousTrack()

    fun nextChapter()

    fun previousChapter()

    fun setSpeed(speed: Float)

    fun startSleepTimer(mode: SleepTimerMode)

    fun cancelSleepTimer()

    fun cancelUpNext()

    fun stop()

    fun currentPositionMs(): Long

    fun currentTrackIndex(): Int

    fun bookPositionMs(): Long
}
