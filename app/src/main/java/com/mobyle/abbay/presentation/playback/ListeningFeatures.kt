package com.mobyle.abbay.presentation.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.mobyle.abbay.presentation.playback.command.PlaybackCommands
import com.mobyle.abbay.presentation.playback.history.HistoryRecorder
import com.mobyle.abbay.presentation.playback.listening.ResumeRewinder
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.mobyle.abbay.presentation.playback.sleep.ShakeSensor
import com.mobyle.abbay.presentation.playback.sleep.SleepTimerBundle
import com.mobyle.abbay.presentation.playback.sleep.SleepTimerController
import com.mobyle.abbay.presentation.playback.sleep.TonePlayer
import com.model.HistoryEventType
import com.player.SleepTimerMode
import com.player.SleepTimerState
import com.settings.AppSettings
import com.usecase.AddBookmark
import com.usecase.FinishBook
import com.usecase.RecordHistoryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class ListeningFeatures(
    context: Context,
    private val player: Player,
    private val commands: PlaybackCommandHandler,
    scope: CoroutineScope,
    private val persistenceScope: CoroutineScope,
    private val settings: () -> AppSettings,
    private val useCases: UseCases,
    private val callbacks: Callbacks,
    private val isInCar: () -> Boolean = { false },
) : Player.Listener {
    class UseCases(val recordHistoryEvent: RecordHistoryEvent, val addBookmark: AddBookmark, val finishBook: FinishBook)

    interface Callbacks {
        fun onSleepTimerChanged(state: SleepTimerState)

        fun onPaused(pausedAt: Long)

        fun onBookmarkAdded()
    }

    private val rewinder = ResumeRewinder()
    val history = HistoryRecorder(player, commands, ::record)
    private val sleepTimer = SleepTimerController(
        player = player,
        commands = commands,
        scope = scope,
        settings = settings,
        tones = TonePlayer(scope),
        listener = SleepEvents(),
    ).apply { attachShakeSensor(ShakeSensor(context, ::onShake)) }

    private var isTransientlyInterrupted = false

    val sleepTimerState: SleepTimerState get() = sleepTimer.state

    fun beforeResume() {
        val bookId = currentBookId() ?: return
        val position = commands.bookPositionMs()
        val target =
            rewinder.resumePositionMs(bookId, position, settings().smartRewind, BookMediaItems.storedPauseOf(player.currentMediaItem))
                ?: return
        history.withoutMoves { commands.seekToBookPosition(target) }
    }

    fun handleCommand(action: String, args: Bundle): Boolean {
        when (action) {
            PlaybackCommands.ADD_BOOKMARK -> addBookmark()
            PlaybackCommands.SLEEP_TIMER_TOGGLE -> sleepTimer.toggle()
            PlaybackCommands.SLEEP_TIMER_CANCEL -> sleepTimer.cancel()
            PlaybackCommands.SLEEP_TIMER_START -> {
                val mode =
                    SleepTimerBundle.modeOf(
                        args.getString(PlaybackCommands.EXTRA_SLEEP_MODE),
                        args.getInt(PlaybackCommands.EXTRA_SLEEP_MINUTES)
                    )
                        ?: return false
                sleepTimer.start(mode)
            }

            else -> return false
        }
        return true
    }

    fun release() = sleepTimer.release()

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            if (!isInCar() || settings().sleepInCar) sleepTimer.onPlaybackStarted()
            return
        }
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) return
        val bookId = currentBookId() ?: return
        rewinder.onPaused(bookId, commands.bookPositionMs())
        callbacks.onPaused(System.currentTimeMillis())
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        val bookId = currentBookId() ?: return
        if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            isTransientlyInterrupted = true
            rewinder.onPaused(bookId, commands.bookPositionMs())
        } else if (isTransientlyInterrupted && playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
            isTransientlyInterrupted = false
            if (player.playWhenReady) beforeResume()
        }
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) sleepTimer.onSeek()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) rewinder.onPlaylistChanged()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState != Player.STATE_ENDED) return
        sleepTimer.cancel()
        val bookId = currentBookId() ?: return
        val position = commands.bookPositionMs()
        persistenceScope.launch { useCases.finishBook(bookId, position) }
    }

    private fun addBookmark() {
        val bookId = currentBookId() ?: return
        val position = commands.bookPositionMs()
        persistenceScope.launch {
            if (useCases.addBookmark(bookId, position) != null) callbacks.onBookmarkAdded()
        }
    }

    private fun onShake() = sleepTimer.onShake()

    private fun record(bookId: String, type: HistoryEventType, positionMs: Long, targetPositionMs: Long?) {
        persistenceScope.launch { useCases.recordHistoryEvent(bookId, type, positionMs, targetPositionMs) }
    }

    private fun currentBookId(): String? = BookMediaItems.bookIdOf(player.currentMediaItem)

    private inner class SleepEvents : SleepTimerController.Listener {
        override fun onStateChanged(state: SleepTimerState) = callbacks.onSleepTimerChanged(state)

        override fun onTimeUp(fadeMs: Long) {
            currentBookId()?.let { bookId -> rewinder.onSleepTimerStopped(bookId, commands.bookPositionMs(), fadeMs) }
            history.markNextPauseAs(HistoryEventType.SLEEP_TIMER_STOP)
            player.pause()
        }

        override fun onExtended() {
            val bookId = currentBookId() ?: return
            record(bookId, HistoryEventType.SLEEP_TIMER_EXTEND, commands.bookPositionMs(), null)
        }
    }

    companion object {
        fun sleepTimerStartArguments(mode: SleepTimerMode): Bundle = Bundle().apply {
            putString(PlaybackCommands.EXTRA_SLEEP_MODE, SleepTimerBundle.modeName(mode))
            putInt(PlaybackCommands.EXTRA_SLEEP_MINUTES, (mode as? SleepTimerMode.Duration)?.minutes ?: 0)
        }
    }
}
