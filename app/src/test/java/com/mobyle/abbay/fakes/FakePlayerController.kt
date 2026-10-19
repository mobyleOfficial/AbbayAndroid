package com.mobyle.abbay.fakes

import com.model.Book
import com.player.PlaybackStatus
import com.player.PlayerController
import com.player.PlayerError
import com.player.PlayerState
import com.player.SleepTimerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Records every command; tests drive [mutableState] and [mutableErrors] to simulate the engine. */
class FakePlayerController : PlayerController {
    val mutableState = MutableStateFlow(PlayerState(isConnected = true))
    override val state: StateFlow<PlayerState> = mutableState

    val mutableErrors = MutableSharedFlow<PlayerError>(extraBufferCapacity = 1)
    override val errors: Flow<PlayerError> = mutableErrors

    val commands = mutableListOf<String>()
    var openedBook: Book? = null
    var positionMs = 0L

    override fun connect() {
        commands += "connect"
    }

    override fun release() {
        commands += "release"
    }

    override fun openBook(book: Book, playWhenReady: Boolean) {
        openedBook = book
        commands += "openBook(${book.id}, progress=${book.progress}, playWhenReady=$playWhenReady)"
    }

    override fun play() {
        commands += "play"
    }

    override fun pause() {
        commands += "pause"
    }

    override fun playPause() {
        commands += "playPause"
    }

    override fun seekBy(deltaMs: Long) {
        commands += "seekBy($deltaMs)"
    }

    override fun seekToBookPosition(bookPositionMs: Long) {
        commands += "seekToBookPosition($bookPositionMs)"
    }

    override fun nextTrack() {
        commands += "nextTrack"
    }

    override fun previousTrack() {
        commands += "previousTrack"
    }

    override fun nextChapter() {
        commands += "nextChapter"
    }

    override fun previousChapter() {
        commands += "previousChapter"
    }

    override fun bookPositionMs(): Long = positionMs

    var trackIndex = 0

    override fun currentTrackIndex(): Int = trackIndex

    override fun seekTo(positionMs: Long) {
        commands += "seekTo($positionMs)"
    }

    override fun seekTo(trackIndex: Int, positionMs: Long) {
        commands += "seekTo($trackIndex, $positionMs)"
    }

    override fun setSpeed(speed: Float) {
        commands += "setSpeed($speed)"
    }

    override fun startSleepTimer(mode: SleepTimerMode) {
        commands += "startSleepTimer($mode)"
    }

    override fun cancelSleepTimer() {
        commands += "cancelSleepTimer"
    }

    override fun cancelUpNext() {
        commands += "cancelUpNext"
    }

    override fun stop() {
        commands += "stop"
    }

    override fun currentPositionMs(): Long = positionMs

    fun emitPlaying() {
        mutableState.value = mutableState.value.copy(isPlaying = true, playWhenReady = true, status = PlaybackStatus.READY)
    }

    fun emitPausedAndReady() {
        mutableState.value = mutableState.value.copy(isPlaying = false, playWhenReady = false, status = PlaybackStatus.READY)
    }
}
