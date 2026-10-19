package com.mobyle.abbay.presentation.playback.extras

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.usecase.RecordListening
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Records listening time per book and local day while audio actually plays.
 * What was measured is written every minute, on pause, when another book starts and when the service stops.
 */
class ListeningStatistics(
    private val player: Player,
    private val scope: CoroutineScope,
    private val persistenceScope: CoroutineScope,
    private val recordListening: RecordListening,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val today: () -> LocalDate = LocalDate::now,
) : Player.Listener {
    private val accumulator = ListeningAccumulator()
    private var flushJob: Job? = null

    /** Silence the trimmer removed from [bookId], reported by the effects with every position save. */
    fun addSilence(bookId: String, savedMs: Long) = accumulator.addSilence(bookId, savedMs)

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            startMeasuring()
        } else {
            flushJob?.cancel()
            accumulator.pause(clock())
            flush()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
        accumulator.setSpeed(clock(), playbackParameters.speed)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val bookId = BookMediaItems.bookIdOf(mediaItem)
        if (bookId == accumulator.currentBookId) return
        accumulator.pause(clock())
        flush()
        if (player.isPlaying) startMeasuring()
    }

    /** Writes what was measured so far; called by the service before it stops. */
    fun flush() {
        val day = accumulator.take(clock(), today()) ?: return
        persistenceScope.launch { recordListening(day) }
    }

    private fun startMeasuring() {
        val bookId = BookMediaItems.bookIdOf(player.currentMediaItem) ?: return
        if (bookId != accumulator.currentBookId) flush()
        accumulator.play(bookId, clock(), player.playbackParameters.speed)
        flushJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    private companion object {
        const val FLUSH_INTERVAL_MS = 60_000L
    }
}
