package com.listening

import com.player.SleepTimerMode
import com.player.SleepTimerState

/**
 * Sleep timer state machine. The playback service feeds it the listening time
 * that passed and, in the end-of-chapter and end-of-track modes, the listening time left to that boundary; it answers
 * with the volume to apply and whether to stop. Pure logic, tested on the JVM.
 */
class SleepTimer {
    var state: SleepTimerState = SleepTimerState.Off
        private set

    private var fadeMs = 0L
    private var remainingDurationMs = 0L
    private var hasAnnouncedFade = false

    val isRunning: Boolean get() = state is SleepTimerState.Running

    val isFading: Boolean get() = (state as? SleepTimerState.Running)?.isFading == true

    /** Starts (or restarts) the timer; [fadeMs] is the length of the fade out at the end. */
    fun start(mode: SleepTimerMode, fadeMs: Long, boundaryRemainingMs: Long? = null) {
        this.fadeMs = fadeMs.coerceAtLeast(0L)
        hasAnnouncedFade = false
        remainingDurationMs = (mode as? SleepTimerMode.Duration)?.let { it.minutes * MILLISECONDS_PER_MINUTE } ?: 0L
        val remaining = if (mode is SleepTimerMode.Duration) remainingDurationMs else boundaryRemainingMs ?: Long.MAX_VALUE
        state = SleepTimerState.Running(mode, remaining, isFading = isInFade(remaining))
    }

    fun cancel() {
        state = SleepTimerState.Off
    }

    /**
     * [listenedMs] of audio played since the last call. [boundaryRemainingMs] is the listening time left to the end of
     * the chapter or track (ignored in duration mode; null when unknown).
     */
    fun advance(listenedMs: Long, boundaryRemainingMs: Long?): Tick {
        val running = state as? SleepTimerState.Running ?: return Tick.IDLE
        val remaining = if (running.mode is SleepTimerMode.Duration) {
            remainingDurationMs = (remainingDurationMs - listenedMs.coerceAtLeast(0L)).coerceAtLeast(0L)
            remainingDurationMs
        } else {
            boundaryRemainingMs ?: running.remainingMs
        }

        if (remaining <= 0L) {
            state = SleepTimerState.Off
            return Tick(volume = 1f, shouldStop = true, hasFadeStarted = false)
        }
        val isFading = isInFade(remaining)
        val hasFadeStarted = isFading && !hasAnnouncedFade
        if (hasFadeStarted) hasAnnouncedFade = true
        state = running.copy(remainingMs = remaining, isFading = isFading)
        return Tick(volume = volumeAt(remaining), shouldStop = false, hasFadeStarted = hasFadeStarted)
    }

    /**
     * Shake to extend: duration mode starts its full length again; the boundary modes switch to the default
     * duration. Returns false when the timer is off or [onlyDuringFade] forbids it now.
     */
    fun extend(defaultMinutes: Int, onlyDuringFade: Boolean): Boolean {
        val running = state as? SleepTimerState.Running ?: return false
        if (onlyDuringFade && !running.isFading) return false
        val mode = running.mode as? SleepTimerMode.Duration ?: SleepTimerMode.Duration(defaultMinutes)
        start(mode, fadeMs)
        return true
    }

    /** Linear ramp to 0 over the last [fadeMs]. */
    fun volumeAt(remainingMs: Long): Float =
        if (fadeMs <= 0L || remainingMs >= fadeMs) 1f else (remainingMs.toFloat() / fadeMs).coerceIn(0f, 1f)

    private fun isInFade(remainingMs: Long): Boolean = fadeMs > 0L && remainingMs < fadeMs

    /** Result of [advance]. [hasFadeStarted] is true once, on the tick that enters the fade (for the warning chime). */
    data class Tick(val volume: Float, val shouldStop: Boolean, val hasFadeStarted: Boolean) {
        companion object {
            val IDLE = Tick(volume = 1f, shouldStop = false, hasFadeStarted = false)
        }
    }

    companion object {
        const val MILLISECONDS_PER_MINUTE = 60_000L
        const val MINIMUM_CUSTOM_MINUTES = 1
        const val MAXIMUM_CUSTOM_MINUTES = 600
        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)

        /** A Play this soon after the timer stopped also rewinds the fade. */
        const val REWIND_AFTER_STOP_WINDOW_MS = 5 * MILLISECONDS_PER_MINUTE
    }
}
