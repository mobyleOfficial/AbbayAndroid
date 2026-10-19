package com.mobyle.abbay.presentation.playback.history

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.mobyle.abbay.presentation.playback.command.TrackPosition
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.model.HistoryEventType

/**
 * Turns player events into history entries: Play, Pause, Seek and
 * ChapterJump. Sleep-timer, bookmark and finished events are recorded where they happen.
 */
@OptIn(UnstableApi::class)
class HistoryRecorder(
    private val player: Player,
    private val commands: PlaybackCommandHandler,
    private val record: (bookId: String, type: HistoryEventType, positionMs: Long, targetPositionMs: Long?) -> Unit,
) : Player.Listener {
    private var isRecordingMoves = true
    private var nextPauseType: HistoryEventType? = null

    /** Runs a seek made by the app itself (smart rewind) without logging it as a user move. */
    fun withoutMoves(seek: () -> Unit) {
        isRecordingMoves = false
        try {
            seek()
        } finally {
            isRecordingMoves = true
        }
    }

    /** The next pause is logged as [type] instead of Pause (the sleep timer stopping playback). */
    fun markNextPauseAs(type: HistoryEventType) {
        nextPauseType = type
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) return
        val bookId = BookMediaItems.bookIdOf(player.currentMediaItem) ?: return
        val type = if (playWhenReady) HistoryEventType.PLAY else nextPauseType ?: HistoryEventType.PAUSE
        nextPauseType = null
        record(bookId, type, commands.bookPositionMs(), null)
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK || !isRecordingMoves) return
        val bookId = BookMediaItems.bookIdOf(newPosition.mediaItem) ?: return
        if (BookMediaItems.bookIdOf(oldPosition.mediaItem) != bookId) return

        val fromMs = commands.bookPositionOf(TrackPosition(oldPosition.mediaItemIndex, oldPosition.positionMs))
        val toMs = commands.bookPositionOf(TrackPosition(newPosition.mediaItemIndex, newPosition.positionMs))
        val type = MoveClassifier.classify(fromMs, toMs, commands.isChapterStart(toMs)) ?: return
        record(bookId, type, fromMs, toMs)
    }
}
