package com.mobyle.abbay.presentation.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.effects.NoiseReductionLevel
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.playback.auto.AutoLibrary
import com.mobyle.abbay.presentation.playback.auto.CarControls
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.mobyle.abbay.presentation.playback.command.PlaybackCommands
import com.mobyle.abbay.presentation.playback.diagnostics.PlaybackLog
import com.mobyle.abbay.presentation.playback.effects.EffectsCoordinator
import com.mobyle.abbay.presentation.playback.effects.EffectsState
import com.mobyle.abbay.presentation.playback.effects.NoiseReductionWatchdog
import com.mobyle.abbay.presentation.playback.extras.ListeningStatistics
import com.mobyle.abbay.presentation.playback.extras.QueueContinuation
import com.mobyle.abbay.presentation.playback.extras.UpNextBundle
import com.mobyle.abbay.presentation.playback.listening.MotionGestures
import com.mobyle.abbay.presentation.playback.media.AudiobookExoPlayer
import com.mobyle.abbay.presentation.playback.media.AudiobookNotificationProvider
import com.mobyle.abbay.presentation.playback.media.AudiobookPlayer
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.mobyle.abbay.presentation.playback.media.Media3PlaylistPlayer
import com.mobyle.abbay.presentation.playback.policy.InterruptionPolicy
import com.mobyle.abbay.presentation.playback.policy.MediaButtonRouter
import com.mobyle.abbay.presentation.playback.policy.PauseTimeout
import com.mobyle.abbay.presentation.playback.policy.PlaybackErrorPolicy
import com.mobyle.abbay.presentation.playback.policy.PlaybackErrorPolicy.Decision
import com.mobyle.abbay.presentation.playback.policy.PlaybackRulesCoordinator
import com.mobyle.abbay.presentation.playback.remote.AudioRoute
import com.mobyle.abbay.presentation.playback.remote.LocalPlaybackSnapshot
import com.mobyle.abbay.presentation.playback.remote.ProcessedAudioStream
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackBridge
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackHost
import com.mobyle.abbay.presentation.playback.remote.StreamRestart
import com.mobyle.abbay.presentation.playback.remote.SwitchablePlaylistPlayer
import com.mobyle.abbay.presentation.playback.sleep.SleepTimerBundle
import com.mobyle.abbay.presentation.playback.sleep.TonePlayer
import com.model.Book
import com.player.NowPlayingPublisher
import com.player.PlaybackIntents
import com.player.SleepTimerState
import com.settings.AppSettings
import com.settings.HeadsetLongPressAction
import com.settings.NotificationSkipButtons
import com.usecase.AddBookmark
import com.usecase.AddSilenceSaved
import com.usecase.AddToQueue
import com.usecase.FinishBook
import com.usecase.GetLastBook
import com.usecase.ObserveAppSettings
import com.usecase.ObserveAudioEffects
import com.usecase.ObserveBookmarks
import com.usecase.ObserveBooks
import com.usecase.ObservePlaybackOptions
import com.usecase.RecordHistoryEvent
import com.usecase.RecordListening
import com.usecase.SaveListeningState
import com.usecase.TakeNextFromQueue
import com.usecase.UpdateAudioEffects
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Playback service: one ExoPlayer, one [MediaLibrarySession], the player
 * commands, position saving, auto-stop after a long pause, error handling
 * and the listening features ([ListeningFeatures]). The Android Auto browse tree is added
 * with SYS-03.
 */
@Suppress("TooManyFunctions") // Service lifecycle plus position saving, errors and session plumbing.
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var commands: PlaybackCommandHandler
    private lateinit var listeningFeatures: ListeningFeatures
    private lateinit var effects: EffectsCoordinator
    private lateinit var interruptions: InterruptionPolicy
    private lateinit var mediaButtons: MediaButtonRouter
    private lateinit var statistics: ListeningStatistics
    private lateinit var motionGestures: MotionGestures
    private lateinit var localSessionPlayer: AudiobookPlayer

    /** While casting, the remote player; the commands and position saves follow it. */
    private var remotePlayer: Player? = null

    /** Where the local player's sound goes: the device, or a Cast receiver. */
    private val audioRoute = AudioRoute()
    private val activePlaylist by lazy { SwitchablePlaylistPlayer(Media3PlaylistPlayer(exoPlayer)) }
    private lateinit var queueContinuation: QueueContinuation
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Position saves must finish even when the service is being destroyed, so they do not use [serviceScope]. */
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var settings = AppSettings()
    private var periodicSaveJob: Job? = null
    private val failedTrackIndices = mutableSetOf<Int>()
    private val pauseTimeout = PauseTimeout(serviceScope, { settings.autoStopAfterPauseMinutes }, ::stopAfterLongPause)

    @Inject
    lateinit var saveListeningState: SaveListeningState

    @Inject
    lateinit var observeAppSettings: ObserveAppSettings

    @Inject
    lateinit var recordHistoryEvent: RecordHistoryEvent

    @Inject
    lateinit var addBookmark: AddBookmark

    @Inject
    lateinit var finishBook: FinishBook

    @Inject
    lateinit var observeAudioEffects: ObserveAudioEffects

    @Inject
    lateinit var addSilenceSaved: AddSilenceSaved

    @Inject
    lateinit var getLastBook: GetLastBook

    @Inject
    lateinit var observeBooks: ObserveBooks

    @Inject
    lateinit var observeBookmarks: ObserveBookmarks

    @Inject
    lateinit var nowPlayingPublisher: NowPlayingPublisher

    @Inject
    lateinit var observePlaybackOptions: ObservePlaybackOptions

    @Inject
    lateinit var recordListening: RecordListening

    @Inject
    lateinit var updateAudioEffects: UpdateAudioEffects

    /** Cast (`:cast`); absent in builds without it. */
    @Inject
    lateinit var remotePlayback: java.util.Optional<RemotePlaybackBridge>

    @Inject
    lateinit var takeNextFromQueue: TakeNextFromQueue

    @Inject
    lateinit var addToQueue: AddToQueue

    private lateinit var widgetFeed: WidgetFeed

    private val autoLibrary by lazy { AutoLibrary(this, observeBooks, observeBookmarks, getLastBook) }

    private val carControls by lazy {
        CarControls(this, exoPlayer, { mediaSession }, { settings }, ::notificationButtons, onSpeedChanged = { savePosition() })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onCreate() {
        super.onCreate()
        PlaybackLog.add("Service created")
        observeSettings()

        val effectsState = EffectsState()
        exoPlayer = AudiobookExoPlayer.build(this, effectsState, audioRoute)
        createLibraryExtras(effectsState)
        commands = PlaybackCommandHandler(
            player = activePlaylist,
            smallBackMs = { settings.smallBackSeconds * MILLISECONDS_PER_SECOND },
            smallForwardMs = { settings.smallForwardSeconds * MILLISECONDS_PER_SECOND },
        )
        listeningFeatures = ListeningFeatures(
            context = this,
            player = exoPlayer,
            commands = commands,
            scope = serviceScope,
            persistenceScope = persistenceScope,
            settings = { settings },
            useCases = ListeningFeatures.UseCases(recordHistoryEvent, addBookmark, finishBook),
            callbacks = ListeningCallbacks(),
            isInCar = { carControls.isConnected },
        )
        widgetFeed = WidgetFeed(exoPlayer, commands, serviceScope, nowPlayingPublisher) { isSleepTimerRunning }
        exoPlayer.addListener(widgetFeed)
        exoPlayer.addListener(PlayerEvents())
        exoPlayer.addListener(listeningFeatures)
        exoPlayer.addListener(listeningFeatures.history)
        exoPlayer.addListener(
            PlaybackRulesCoordinator(exoPlayer, serviceScope, observePlaybackOptions, listeningFeatures.history::withoutMoves)
        )
        setMediaNotificationProvider(AudiobookNotificationProvider(this))

        val sessionPlayer = AudiobookPlayer(
            exoPlayer,
            commands,
            beforeResume = { listeningFeatures.beforeResume() },
            showsCover = { settings.coverOnLockScreen },
        )
        localSessionPlayer = sessionPlayer
        motionGestures = createMotionGestures(sessionPlayer)
        interruptions = InterruptionPolicy(this, exoPlayer, { settings }).also { it.applySettings() }
        exoPlayer.addListener(interruptions)
        mediaButtons = MediaButtonRouter(serviceScope, sessionPlayer, commands, { settings }) { action ->
            val command = if (action ==
                HeadsetLongPressAction.BOOKMARK
            ) {
                PlaybackCommands.ADD_BOOKMARK
            } else {
                PlaybackCommands.SLEEP_TIMER_TOGGLE
            }
            listeningFeatures.handleCommand(command, Bundle.EMPTY)
        }
        createSession(sessionPlayer)
    }

    /** Android 13+ applies a new per-app or system language here: the buttons' names follow it. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mediaSession?.setCustomLayout(notificationButtons())
    }

    private fun observeSettings() {
        observeAppSettings().onEach { newSettings ->
            val skipsChanged = newSettings.smallBackSeconds != settings.smallBackSeconds ||
                newSettings.smallForwardSeconds != settings.smallForwardSeconds ||
                newSettings.skipButtonsInNotification != settings.skipButtonsInNotification ||
                newSettings.language != settings.language
            val coverChanged = newSettings.coverOnLockScreen != settings.coverOnLockScreen
            settings = newSettings
            if (::interruptions.isInitialized) interruptions.applySettings()
            if (::motionGestures.isInitialized) motionGestures.update()
            if (skipsChanged) mediaSession?.setCustomLayout(notificationButtons())
            if (coverChanged && ::localSessionPlayer.isInitialized) localSessionPlayer.republishMetadata()
        }.launchIn(serviceScope)
    }

    /**
     * Back · Play/Pause · Forward in the notification (or previous/next chapter, Settings → Playback), plus Bookmark
     * and Sleep timer in the expanded view.
     */
    private fun notificationButtons(): ImmutableList<CommandButton> = ImmutableList.copyOf(
        sideButtons() + expandedButtons(),
    )

    private fun expandedButtons(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_BOOKMARK_UNFILLED)
            .setDisplayName(getString(R.string.playback_add_bookmark))
            .setSessionCommand(SessionCommand(PlaybackCommands.ADD_BOOKMARK, Bundle.EMPTY))
            .build(),
        CommandButton.Builder()
            .setIconResId(if (isSleepTimerRunning) R.drawable.ic_sleep_timer_off else R.drawable.ic_sleep_timer)
            .setDisplayName(
                getString(if (isSleepTimerRunning) R.string.playback_sleep_timer_cancel else R.string.playback_sleep_timer_start)
            )
            .setSessionCommand(SessionCommand(PlaybackCommands.SLEEP_TIMER_TOGGLE, Bundle.EMPTY))
            .build(),
    )

    private fun sideButtons(): List<CommandButton> = when (settings.skipButtonsInNotification) {
        NotificationSkipButtons.TIME_SKIP -> listOf(
            CommandButton.Builder(skipBackIcon(settings.smallBackSeconds))
                .setDisplayName(getString(R.string.playback_skip_back, settings.smallBackSeconds))
                .setSessionCommand(SessionCommand(PlaybackCommands.SKIP_BACK, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(skipForwardIcon(settings.smallForwardSeconds))
                .setDisplayName(getString(R.string.playback_skip_forward, settings.smallForwardSeconds))
                .setSessionCommand(SessionCommand(PlaybackCommands.SKIP_FORWARD, Bundle.EMPTY))
                .build(),
        )
        NotificationSkipButtons.PREVIOUS_AND_NEXT_CHAPTER -> listOf(
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setDisplayName(getString(R.string.playback_previous_chapter))
                .setSessionCommand(SessionCommand(PlaybackCommands.PREVIOUS_CHAPTER, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setDisplayName(getString(R.string.playback_next_chapter))
                .setSessionCommand(SessionCommand(PlaybackCommands.NEXT_CHAPTER, Bundle.EMPTY))
                .build(),
        )
    }

    private val isSleepTimerRunning: Boolean
        get() = ::listeningFeatures.isInitialized && listeningFeatures.sleepTimerState is SleepTimerState.Running

    private fun skipBackIcon(seconds: Int): Int = SKIP_BACK_ICONS[seconds] ?: CommandButton.ICON_SKIP_BACK

    private fun skipForwardIcon(seconds: Int): Int = SKIP_FORWARD_ICONS[seconds] ?: CommandButton.ICON_SKIP_FORWARD

    /** Opens the app's launcher activity when the notification is tapped. */
    private fun sessionActivityIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Widget buttons: bookmark and sleep timer arrive as actions; a skip key with
     * no book loaded becomes Play, which resumes the last book, since there is nothing to skip in.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PlaybackIntents.ACTION_ADD_BOOKMARK -> listeningFeatures.handleCommand(PlaybackCommands.ADD_BOOKMARK, Bundle.EMPTY)
            PlaybackIntents.ACTION_TOGGLE_SLEEP_TIMER -> listeningFeatures.handleCommand(PlaybackCommands.SLEEP_TIMER_TOGGLE, Bundle.EMPTY)
            Intent.ACTION_MEDIA_BUTTON -> if (intent.getBooleanExtra(PlaybackIntents.EXTRA_FROM_WIDGET, false) &&
                exoPlayer.mediaItemCount == 0
            ) {
                intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * The user dismissed the app from the recent tasks. "Play when app is closed" off: pause, save the
     * position and stop the service; on: keep playing, and stop only when nothing is playing.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!settings.playWhenAppIsClosed) {
            exoPlayer.pause()
            savePosition()
            stopSelf()
        } else if (!exoPlayer.playWhenReady || exoPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        PlaybackLog.add("Service destroyed")
        remotePlayback.ifPresent(RemotePlaybackBridge::release)
        savePosition()
        statistics.flush()
        listeningFeatures.release()
        motionGestures.release()
        interruptions.release()
        widgetFeed.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Saves the current book's track, position and speed; [pausedAt] is set when
     * playback just paused (smart rewind).
     */
    private fun savePosition(pausedAt: Long? = null) {
        if (exoPlayer.mediaItemCount == 0) return
        val bookId = BookMediaItems.bookIdOf(exoPlayer.currentMediaItem) ?: return
        // While casting, the local player stays loaded with the same book, paused; the position is the receiver's.
        val playing = remotePlayer ?: exoPlayer
        val trackIndex = playing.currentMediaItemIndex
        val positionMs = playing.currentPosition
        val speed = playing.playbackParameters.speed
        effects.flushSavedSilence()
        persistenceScope.launch { saveListeningState(bookId, trackIndex, positionMs, speed, pausedAt) }
    }

    /** Every 10 s while playing, so a crash loses at most 10 s. */
    private fun startPeriodicSaves() {
        periodicSaveJob?.cancel()
        periodicSaveJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_SAVE_INTERVAL_MS)
                savePosition()
            }
        }
    }

    /** Effects, statistics and the queue. */
    private fun createLibraryExtras(effectsState: EffectsState) {
        statistics = ListeningStatistics(exoPlayer, serviceScope, persistenceScope, recordListening)
        effects = EffectsCoordinator(
            exoPlayer,
            effectsState,
            serviceScope,
            persistenceScope,
            observeAudioEffects,
            addSilenceSaved,
            onSilenceSaved = statistics::addSilence,
        )
        exoPlayer.addListener(effects)
        exoPlayer.addListener(statistics)
        exoPlayer.addAnalyticsListener(NoiseReductionWatchdog(effectsState, ::turnOffNoiseReduction))
        queueContinuation = QueueContinuation(
            scope = serviceScope,
            isEnabled = { settings.continueWithQueue },
            takeNext = { takeNextFromQueue() },
            putBack = { bookId -> addToQueue(bookId, playNext = true) },
            onChanged = { publishSessionExtras() },
            start = ::startQueuedBook,
        )
    }

    /** The media session over [sessionPlayer]; Cast may borrow it later. */
    private fun createSession(sessionPlayer: Player) {
        mediaSession = MediaLibrarySession.Builder(this, sessionPlayer, SessionCallback())
            .apply { sessionActivityIntent()?.let(::setSessionActivity) }
            .setCustomLayout(notificationButtons())
            .build()
        remotePlayback.ifPresent { it.attach(RemoteHost()) }
    }

    /** Face down / flip; resuming goes through the session player, so smart rewind applies. */
    private fun createMotionGestures(sessionPlayer: Player) = MotionGestures(
        context = this,
        player = exoPlayer,
        settings = { settings },
        tones = TonePlayer(serviceScope),
        actions = MotionGestures.Actions(pause = exoPlayer::pause, resume = sessionPlayer::play, rewind = commands::seekBy),
    ).also(exoPlayer::addListener)

    /** Underruns with noise reduction on: turn it off for this book and tell the app once. */
    private fun turnOffNoiseReduction() {
        val bookId = BookMediaItems.bookIdOf(exoPlayer.currentMediaItem) ?: return
        PlaybackLog.add("Noise reduction turned off after audio underruns")
        persistenceScope.launch {
            val current = observeAudioEffects(bookId).first()
            updateAudioEffects(bookId, current.copy(noiseReduction = NoiseReductionLevel.OFF))
        }
        broadcastProblem(PROBLEM_NOISE_REDUCTION_TOO_SLOW, "")
    }

    /** Lends the session to Cast: the receiver plays the files; effects are not applied. */
    private inner class RemoteHost : RemotePlaybackHost {
        override fun snapshot(): LocalPlaybackSnapshot? {
            if (exoPlayer.mediaItemCount == 0) return null
            return LocalPlaybackSnapshot(
                items = (0 until exoPlayer.mediaItemCount).map(exoPlayer::getMediaItemAt),
                trackIndex = exoPlayer.currentMediaItemIndex,
                positionMs = exoPlayer.currentPosition,
                speed = exoPlayer.playbackParameters.speed,
                playWhenReady = exoPlayer.playWhenReady,
                processedAudio = settings.castProcessedAudio,
            )
        }

        override fun onRemoteStarted(player: Player) {
            exoPlayer.pause()
            savePosition()
            PlaybackLog.add("Casting started")
            val items = (0 until exoPlayer.mediaItemCount).map(exoPlayer::getMediaItemAt)
            remotePlayer = player
            activePlaylist.current = Media3PlaylistPlayer(player, items)
            mediaSession?.player = AudiobookPlayer(player, commands)
        }

        override fun onRemoteEnded(trackIndex: Int, positionMs: Long, playWhenReady: Boolean) {
            PlaybackLog.add("Casting ended")
            remotePlayer = null
            activePlaylist.current = Media3PlaylistPlayer(exoPlayer)
            mediaSession?.player = localSessionPlayer
            if (exoPlayer.mediaItemCount > trackIndex) exoPlayer.seekTo(trackIndex, positionMs)
            savePosition()
            if (playWhenReady) exoPlayer.play()
        }

        override fun saveRemotePosition() = savePosition()

        override fun startProcessedStream(stream: ProcessedAudioStream): Boolean {
            if (exoPlayer.mediaItemCount == 0) return false
            PlaybackLog.add("Casting started (with audio effects)")
            audioRoute.stream = stream
            // The phone keeps decoding while the screen is off; Wi-Fi must stay awake. No audio focus: the phone's
            // own sounds and calls should not pause the receiver.
            exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK)
            exoPlayer.setAudioAttributes(exoPlayer.audioAttributes, false)
            exoPlayer.addListener(restartStreamOnSpeedChange)
            restartAtCurrentPosition()
            return true
        }

        override fun stopProcessedStream(playWhenReady: Boolean) {
            if (audioRoute.stream == null) return
            PlaybackLog.add("Casting ended (with audio effects)")
            exoPlayer.removeListener(restartStreamOnSpeedChange)
            if (!playWhenReady) exoPlayer.pause()
            audioRoute.stream = null
            exoPlayer.setWakeMode(C.WAKE_MODE_LOCAL)
            exoPlayer.setAudioAttributes(exoPlayer.audioAttributes, true)
            restartAtCurrentPosition()
            savePosition()
        }
    }

    /** While casting with effects a new speed or pitch restarts the stream, so the receiver drops the old audio. */
    private val restartStreamOnSpeedChange = object : Player.Listener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            if (audioRoute.stream != null) restartAtCurrentPosition()
        }
    }

    /**
     * A seek to where playback is: the audio sink flushes, which is where the route changes. One millisecond
     * ahead, because ExoPlayer ignores a seek to exactly the current millisecond (no flush would happen).
     */
    private fun restartAtCurrentPosition() {
        if (exoPlayer.mediaItemCount == 0) return
        val target = StreamRestart.positionAfter(exoPlayer.currentPosition, exoPlayer.duration)
        listeningFeatures.history.withoutMoves { exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, target) }
    }

    /** The book after the countdown of [QueueContinuation]: loaded at its saved position and played. */
    private fun startQueuedBook(book: Book) {
        val (items, trackIndex) = BookMediaItems.of(book)
        if (items.isEmpty()) return
        exoPlayer.setMediaItems(items, trackIndex, book.progress)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    /** The sleep timer and the queue countdown travel to controllers as session extras. */
    private fun publishSessionExtras() {
        val extras = SleepTimerBundle.of(listeningFeatures.sleepTimerState)
        UpNextBundle.putInto(extras, queueContinuation.upNext)
        mediaSession?.setSessionExtras(extras)
    }

    private fun stopAfterLongPause() {
        savePosition()
        exoPlayer.stop()
        stopSelf()
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val kind = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            -> PlaybackErrorPolicy.ErrorKind.STORAGE

            else -> PlaybackErrorPolicy.ErrorKind.UNPLAYABLE
        }
        val failedIndex = exoPlayer.currentMediaItemIndex
        val failedFileName = BookMediaItems.fileNameOf(exoPlayer.currentMediaItem)
        PlaybackLog.add("Error ${error.errorCodeName} in track ${failedIndex + 1}:", failedFileName)

        when (val decision = PlaybackErrorPolicy.decide(kind, failedIndex, exoPlayer.mediaItemCount, failedTrackIndices)) {
            is Decision.SkipToNext -> {
                failedTrackIndices += decision.failedTrackIndex
                exoPlayer.seekTo(decision.failedTrackIndex + 1, 0L)
                exoPlayer.prepare()
                exoPlayer.play()
                broadcastProblem(PROBLEM_TRACK_SKIPPED, failedFileName)
            }

            Decision.StopAtEnd -> {
                savePosition()
                broadcastProblem(PROBLEM_TRACK_SKIPPED, failedFileName)
            }

            Decision.StopBookUnplayable -> {
                failedTrackIndices.clear()
                exoPlayer.stop()
                broadcastProblem(PROBLEM_BOOK_UNPLAYABLE, failedFileName)
            }

            Decision.StopStorageUnavailable -> {
                exoPlayer.stop()
                broadcastProblem(PROBLEM_STORAGE_UNAVAILABLE, failedFileName)
            }
        }
    }

    private fun broadcastProblem(problem: String, fileName: String) {
        mediaSession?.broadcastCustomCommand(
            SessionCommand(PlaybackCommands.EVENT_PLAYBACK_PROBLEM, Bundle.EMPTY),
            Bundle().apply {
                putString(PlaybackCommands.EXTRA_PROBLEM, problem)
                putString(PlaybackCommands.EXTRA_FILE_NAME, fileName)
            },
        )
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            PlaybackLog.add(
                if (isPlaying) "Playing at ${exoPlayer.playbackParameters.speed}x" else "Not playing (state ${exoPlayer.playbackState})",
            )
            if (isPlaying) {
                startPeriodicSaves()
                pauseTimeout.onPlaying()
            } else {
                periodicSaveJob?.cancel()
                // A pause is saved once, with its time, by ListeningFeatures; this covers stalls and interruptions.
                if (exoPlayer.playWhenReady) savePosition()
                if (exoPlayer.playbackState != Player.STATE_IDLE && exoPlayer.mediaItemCount > 0) pauseTimeout.onPaused()
            }
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            savePosition()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            PlaybackLog.add(
                "Track ${exoPlayer.currentMediaItemIndex + 1}/${exoPlayer.mediaItemCount} (reason $reason):",
                BookMediaItems.fileNameOf(mediaItem)
            )
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                failedTrackIndices.clear()
                // Another book was opened during the countdown: the queued one waits.
                queueContinuation.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) queueContinuation.onBookFinished()
        }

        override fun onPlayerError(error: PlaybackException) = handlePlaybackError(error)
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {
        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) = carControls.onDisconnected(controller)

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val isCarController = carControls.onConnected(session, controller)
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .apply { PlaybackCommands.ALL.forEach { add(SessionCommand(it, Bundle.EMPTY)) } }
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(if (isCarController) carControls.buttons() else notificationButtons())
                .setSessionExtras(
                    SleepTimerBundle.of(listeningFeatures.sleepTimerState).also { UpNextBundle.putInto(it, queueContinuation.upNext) },
                )
                .build()
        }

        /** Bluetooth and headset NEXT/PREVIOUS keys skip in book time. */
        override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
            val keyEvent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                ?: return false
            // Without a book loaded, Media3 resumes the last one (onPlaybackResumption).
            if (exoPlayer.mediaItemCount == 0) return false
            return mediaButtons.handle(keyEvent)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == PlaybackCommands.SPEED_CYCLE) {
                carControls.cycleSpeed()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == PlaybackCommands.UP_NEXT_CANCEL) {
                queueContinuation.cancel()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            val isHandled = listeningFeatures.handleCommand(customCommand.customAction, args) ||
                commands.execute(
                    action = customCommand.customAction,
                    deltaMs = args.getLong(PlaybackCommands.EXTRA_DELTA_MS),
                    positionMs = args.getLong(PlaybackCommands.EXTRA_POSITION_MS),
                )
            val resultCode = if (isHandled) SessionResult.RESULT_SUCCESS else SessionError.ERROR_NOT_SUPPORTED
            return Futures.immediateFuture(SessionResult(resultCode))
        }

        /**
         * Play with nothing loaded (a Bluetooth key with the app closed, a widget, the Quick Settings tile, Android Auto):
         * load the last book at its saved position.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                val book = getLastBook()
                if (book == null) {
                    result.setException(IllegalStateException("No book to resume"))
                } else {
                    val (items, trackIndex) = BookMediaItems.of(book)
                    result.set(MediaSession.MediaItemsWithStartPosition(items, trackIndex, book.progress))
                }
            }
            return result
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(LibraryResult.ofItem(autoLibrary.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            val children = autoLibrary.children(parentId, BookMediaItems.bookIdOf(exoPlayer.currentMediaItem), browser.packageName)
            LibraryResult.ofItemList(ImmutableList.copyOf(children.page(page, pageSize)), params)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = serviceScope.future {
            session.notifySearchResultChanged(browser, query, autoLibrary.search(query, browser.packageName).size, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            LibraryResult.ofItemList(ImmutableList.copyOf(autoLibrary.search(query, browser.packageName).page(page, pageSize)), params)
        }

        /**
         * A car (or any browser) asks to play an item by media id or by a spoken query: it becomes the book's playlist at
         * the right position. The app's own controller sends full playlists, which pass through unchanged.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val requested = mediaItems.singleOrNull()
            if (requested == null || requested.localConfiguration != null) {
                return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            }
            return serviceScope.future { autoLibrary.resolve(requested) ?: error("Nothing to play for ${requested.mediaId}") }
        }
    }

    private inner class ListeningCallbacks : ListeningFeatures.Callbacks {
        private var wasSleepTimerRunning = false

        override fun onSleepTimerChanged(state: SleepTimerState) {
            publishSessionExtras()
            val isRunning = state is SleepTimerState.Running
            if (isRunning != wasSleepTimerRunning) {
                wasSleepTimerRunning = isRunning
                widgetFeed.publish()
                mediaSession?.setCustomLayout(notificationButtons())
            }
        }

        override fun onPaused(pausedAt: Long) = savePosition(pausedAt)

        override fun onBookmarkAdded() {
            serviceScope.launch { Toast.makeText(this@PlaybackService, R.string.playback_bookmark_added, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        const val PROBLEM_TRACK_SKIPPED = "trackSkipped"
        const val PROBLEM_BOOK_UNPLAYABLE = "bookUnplayable"
        const val PROBLEM_STORAGE_UNAVAILABLE = "storageUnavailable"
        const val PROBLEM_NOISE_REDUCTION_TOO_SLOW = "noiseReductionTooSlow"
        private const val PERIODIC_SAVE_INTERVAL_MS = 10_000L
        private const val MILLISECONDS_PER_SECOND = 1_000L

        /** Media3 has numbered skip icons for these amounts (seconds); other amounts use the plain icon. */
        private val SKIP_BACK_ICONS = mapOf(
            5 to CommandButton.ICON_SKIP_BACK_5,
            10 to CommandButton.ICON_SKIP_BACK_10,
            15 to CommandButton.ICON_SKIP_BACK_15,
            30 to CommandButton.ICON_SKIP_BACK_30,
        )
        private val SKIP_FORWARD_ICONS = mapOf(
            5 to CommandButton.ICON_SKIP_FORWARD_5,
            10 to CommandButton.ICON_SKIP_FORWARD_10,
            15 to CommandButton.ICON_SKIP_FORWARD_15,
            30 to CommandButton.ICON_SKIP_FORWARD_30,
        )
    }
}
