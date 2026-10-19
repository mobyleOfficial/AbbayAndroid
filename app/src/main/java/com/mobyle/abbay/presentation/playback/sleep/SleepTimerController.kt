package com.mobyle.abbay.presentation.playback.sleep

import android.os.SystemClock
import androidx.media3.common.Player
import com.listening.SleepSchedule
import com.listening.SleepTimer
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.player.SleepTimerMode
import com.player.SleepTimerState
import com.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Runs the sleep timer inside the playback service: counts listening time,
 * fades with `player.volume`, plays the chime, listens for shakes and starts itself on the daily schedule. The
 * decisions are made by [SleepTimer]; this class connects it to the player, the clock and the sensors.
 */
@Suppress("LongParameterList", "TooManyFunctions") // The timer's collaborators; one function per timer event.
class SleepTimerController(
    private val player: Player,
    private val commands: PlaybackCommandHandler,
    private val scope: CoroutineScope,
    private val settings: () -> AppSettings,
    private val tones: TonePlayer,
    private val listener: Listener,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    /** What the service does when the timer acts. */
    interface Listener {
        fun onStateChanged(state: SleepTimerState)

        /** Time is up: pause playback; [fadeMs] is the fade that just ended. */
        fun onTimeUp(fadeMs: Long)

        /** A shake extended the timer. */
        fun onExtended()
    }

    private val timer = SleepTimer()
    private var loopJob: Job? = null
    private var chapterEndTargetMs = 0L
    private var lastPublishedAt = 0L
    private var lastPublishedState: SleepTimerState = SleepTimerState.Off
    private var shakeSensor: ShakeSensor? = null

    val state: SleepTimerState get() = timer.state

    /** Created lazily by the service, which owns the context. */
    fun attachShakeSensor(sensor: ShakeSensor) {
        shakeSensor = sensor
    }

    fun start(mode: SleepTimerMode) {
        val current = settings()
        if (mode is SleepTimerMode.EndOfChapter) chapterEndTargetMs = commands.currentChapterEndMs()
        timer.start(mode, fadeMs(), boundaryRemainingMs(mode))
        player.volume = 1f
        shakeSensor?.stop()
        if (current.shakeToExtend) shakeSensor?.start(current.shakeSensitivity)
        startLoop()
        publish(force = true)
    }

    fun cancel() {
        if (!timer.isRunning) return
        timer.cancel()
        stopRunning()
    }

    /** Notification button: starts the default duration, or cancels a running timer. */
    fun toggle() = if (timer.isRunning) cancel() else start(SleepTimerMode.Duration(settings().sleepDefaultMinutes))

    fun onShake() {
        val current = settings()
        if (!timer.extend(current.sleepDefaultMinutes, current.extendOnlyDuringFade)) return
        player.volume = 1f
        tones.playConfirmation(current.sleepChimeVolumePercent / PERCENT)
        listener.onExtended()
        publish(force = true)
    }

    /** The user moved: "End of chapter" now means the chapter of the new position. */
    fun onSeek() {
        if ((timer.state as? SleepTimerState.Running)?.mode == SleepTimerMode.EndOfChapter) {
            chapterEndTargetMs = commands.currentChapterEndMs()
        }
    }

    /** Playback started: the daily schedule may start the timer ("Auto-enable"). */
    fun onPlaybackStarted(now: LocalTime = LocalTime.now()) {
        val current = settings()
        if (!current.autoSleepEnabled || timer.isRunning) return
        val schedule = SleepSchedule(current.autoSleepStartMinute, current.autoSleepEndMinute)
        if (schedule.contains(now.hour * MINUTES_PER_HOUR + now.minute)) start(SleepTimerMode.Duration(current.sleepDefaultMinutes))
    }

    fun release() {
        loopJob?.cancel()
        shakeSensor?.stop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            var lastTickAt = clock()
            var running = timer.state as? SleepTimerState.Running
            while (isActive && running != null) {
                val now = clock()
                val listenedMs = if (player.isPlaying) now - lastTickAt else 0L
                lastTickAt = now
                val tick = timer.advance(listenedMs, boundaryRemainingMs(running.mode))
                running = timer.state as? SleepTimerState.Running

                if (tick.shouldStop) {
                    listener.onTimeUp(fadeMs())
                    stopRunning()
                } else {
                    if (tick.hasFadeStarted && settings().sleepChime) tones.playWarningChime(settings().sleepChimeVolumePercent / PERCENT)
                    player.volume = tick.volume
                    publish(force = false)
                    delay(nextDelayMs())
                }
            }
        }
    }

    private fun stopRunning() {
        loopJob?.cancel()
        loopJob = null
        shakeSensor?.stop()
        player.volume = 1f
        publish(force = true)
    }

    /** Listening time left to the boundary, at the current speed; null in duration mode or when unknown. */
    private fun boundaryRemainingMs(mode: SleepTimerMode): Long? {
        val speed = player.playbackParameters.speed.coerceAtLeast(MINIMUM_SPEED)
        val mediaRemaining = when (mode) {
            SleepTimerMode.EndOfChapter -> chapterEndTargetMs - commands.bookPositionMs()
            SleepTimerMode.EndOfTrack -> commands.currentTrackRemainingMs()
            is SleepTimerMode.Duration -> null
        }
        return mediaRemaining?.let { (it / speed).toLong() }
    }

    /** Ticks land on the start of the fade and on the end; during the fade they are short so the ramp is smooth. */
    private fun nextDelayMs(): Long {
        val running = timer.state as? SleepTimerState.Running ?: return MAXIMUM_TICK_MS
        if (!player.isPlaying) return MAXIMUM_TICK_MS
        if (running.isFading) return FADE_TICK_MS
        val fade = fadeMs()
        val untilNextEdge = if (fade > 0L && running.remainingMs > fade) running.remainingMs - fade else running.remainingMs
        return untilNextEdge.coerceIn(MINIMUM_TICK_MS, MAXIMUM_TICK_MS)
    }

    /** Remaining time is shown in seconds, so the state is sent at most once per second unless something changed. */
    private fun publish(force: Boolean) {
        val now = clock()
        val state = timer.state
        val modeOrFadeChanged = (state as? SleepTimerState.Running)?.let { running ->
            val previous = lastPublishedState as? SleepTimerState.Running
            previous == null || previous.mode != running.mode || previous.isFading != running.isFading
        } ?: (lastPublishedState != state)
        if (!force && !modeOrFadeChanged && now - lastPublishedAt < PUBLISH_INTERVAL_MS) return
        lastPublishedAt = now
        lastPublishedState = state
        listener.onStateChanged(state)
    }

    private fun fadeMs(): Long = settings().sleepFadeSeconds.coerceIn(0, MAXIMUM_FADE_SECONDS) * MILLISECONDS_PER_SECOND

    private companion object {
        const val PERCENT = 100f
        const val MINUTES_PER_HOUR = 60
        const val MILLISECONDS_PER_SECOND = 1_000L
        const val MAXIMUM_FADE_SECONDS = 120
        const val MINIMUM_SPEED = 0.1f
        const val MINIMUM_TICK_MS = 20L
        const val MAXIMUM_TICK_MS = 1_000L
        const val FADE_TICK_MS = 100L
        const val PUBLISH_INTERVAL_MS = 1_000L
    }
}
