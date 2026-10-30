package com.mobyle.abbay.presentation.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.mobyle.abbay.presentation.playback.command.PlaybackCommands
import com.mobyle.abbay.presentation.playback.extras.UpNextBundle
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.mobyle.abbay.presentation.playback.media.Media3PlaylistPlayer
import com.mobyle.abbay.presentation.playback.sleep.SleepTimerBundle
import com.model.Book
import com.player.PlaybackStatus
import com.player.PlayerController
import com.player.PlayerError
import com.player.PlayerState
import com.player.SleepTimerMode
import com.player.SleepTimerState
import com.player.UpNext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PlayerController] backed by a Media3 [MediaController] connected to [PlaybackService].
 * Every call must happen on the main thread, as required by [MediaController].
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerControllerImpl @Inject constructor(@ApplicationContext private val context: Context) : PlayerController {
    private val mutableState = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = mutableState.asStateFlow()

    private val mutableErrors = MutableSharedFlow<PlayerError>(extraBufferCapacity = 1)
    override val errors: Flow<PlayerError> = mutableErrors.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var sleepTimerState: SleepTimerState = SleepTimerState.Off
    private var upNext: UpNext? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }
    }

    /** Problems are decided by the service (it already skipped or stopped) and sent as a session event. */
    private val controllerListener = object : MediaController.Listener {
        override fun onCustomCommand(controller: MediaController, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            if (command.customAction == PlaybackCommands.EVENT_PLAYBACK_PROBLEM) {
                val error = when (args.getString(PlaybackCommands.EXTRA_PROBLEM)) {
                    PlaybackService.PROBLEM_TRACK_SKIPPED ->
                        PlayerError.TrackSkipped(args.getString(PlaybackCommands.EXTRA_FILE_NAME).orEmpty())
                    PlaybackService.PROBLEM_BOOK_UNPLAYABLE -> PlayerError.BookUnplayable
                    PlaybackService.PROBLEM_NOISE_REDUCTION_TOO_SLOW -> PlayerError.NoiseReductionTooSlow
                    else -> PlayerError.StorageUnavailable
                }
                mutableErrors.tryEmit(error)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /** The service sends the sleep timer state and the queue countdown as session extras. */
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            sleepTimerState = SleepTimerBundle.stateOf(extras)
            upNext = UpNextBundle.upNextOf(extras)
            publishState(controller)
        }
    }

    override fun connect() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).setListener(controllerListener).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture !== future) return@addListener
                val connectedController = runCatching { future.get() }.getOrNull() ?: run {
                    controllerFuture = null
                    return@addListener
                }
                mediaController = connectedController
                sleepTimerState = SleepTimerBundle.stateOf(connectedController.sessionExtras)
                upNext = UpNextBundle.upNextOf(connectedController.sessionExtras)
                connectedController.addListener(playerListener)
                publishState(connectedController)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun release() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        mediaController = null
        sleepTimerState = SleepTimerState.Off
        upNext = null
        mutableState.value = PlayerState()
    }

    override fun openBook(book: Book, playWhenReady: Boolean) {
        val controller = mediaController ?: return
        val (items, trackIndex) = BookMediaItems.of(book)
        if (items.isEmpty()) return

        controller.pause()
        controller.setMediaItems(items, trackIndex, book.progress)
        controller.prepare()
        controller.playWhenReady = playWhenReady
    }

    override fun play() {
        mediaController?.play()
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun playPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    // Book-time commands run here with the same handler the service uses for the notification and media keys, so the
    // controller's position updates immediately.
    override fun seekBy(deltaMs: Long) {
        commands()?.seekBy(deltaMs)
    }

    override fun seekToBookPosition(bookPositionMs: Long) {
        commands()?.seekToBookPosition(bookPositionMs)
    }

    override fun nextTrack() {
        commands()?.nextTrack()
    }

    override fun previousTrack() {
        commands()?.previousTrack()
    }

    override fun nextChapter() {
        commands()?.nextChapter()
    }

    override fun previousChapter() {
        commands()?.previousChapter()
    }

    override fun bookPositionMs(): Long = commands()?.bookPositionMs() ?: 0L

    override fun currentTrackIndex(): Int = mediaController?.currentMediaItemIndex ?: 0

    private fun commands(): PlaybackCommandHandler? = mediaController?.let { controller ->
        PlaybackCommandHandler(Media3PlaylistPlayer(controller), smallBackMs = { 0L }, smallForwardMs = { 0L })
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun seekTo(trackIndex: Int, positionMs: Long) {
        mediaController?.seekTo(trackIndex, positionMs)
    }

    override fun setSpeed(speed: Float) {
        mediaController?.let { controller ->
            controller.playbackParameters = controller.playbackParameters.withSpeed(speed)
        }
    }

    override fun startSleepTimer(mode: SleepTimerMode) {
        sendCommand(PlaybackCommands.SLEEP_TIMER_START, ListeningFeatures.sleepTimerStartArguments(mode))
    }

    override fun cancelSleepTimer() {
        sendCommand(PlaybackCommands.SLEEP_TIMER_CANCEL, Bundle.EMPTY)
    }

    override fun cancelUpNext() {
        sendCommand(PlaybackCommands.UP_NEXT_CANCEL, Bundle.EMPTY)
    }

    private fun sendCommand(action: String, arguments: Bundle) {
        mediaController?.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), arguments)
    }

    override fun stop() {
        mediaController?.stop()
    }

    override fun currentPositionMs(): Long = mediaController?.currentPosition ?: 0L

    private fun publishState(player: Player) {
        mutableState.value = PlayerState(
            isConnected = true,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            status = player.playbackState.toPlaybackStatus(),
            currentTrackIndex = player.currentMediaItemIndex,
            trackCount = player.mediaItemCount,
            speed = player.playbackParameters.speed,
            sleepTimer = sleepTimerState,
            bookId = BookMediaItems.bookIdOf(player.currentMediaItem),
            upNext = upNext,
            isCasting = player.deviceInfo.playbackType == androidx.media3.common.DeviceInfo.PLAYBACK_TYPE_REMOTE,
        )
    }

    private fun Int.toPlaybackStatus(): PlaybackStatus = when (this) {
        Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
        Player.STATE_READY -> PlaybackStatus.READY
        Player.STATE_ENDED -> PlaybackStatus.ENDED
        else -> PlaybackStatus.IDLE
    }
}
