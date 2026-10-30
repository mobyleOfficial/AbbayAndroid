package com.mobyle.abbay.presentation.playback.sleep

import android.os.Bundle
import com.player.SleepTimerMode
import com.player.SleepTimerState

object SleepTimerBundle {
    private const val KEY_MODE = "sleepTimerMode"
    private const val KEY_MINUTES = "sleepTimerMinutes"
    private const val KEY_REMAINING_MS = "sleepTimerRemainingMs"
    private const val KEY_IS_FADING = "sleepTimerIsFading"

    const val MODE_DURATION = "duration"
    const val MODE_END_OF_CHAPTER = "endOfChapter"
    const val MODE_END_OF_TRACK = "endOfTrack"

    fun of(state: SleepTimerState): Bundle = Bundle().apply {
        val running = state as? SleepTimerState.Running ?: return@apply
        putString(KEY_MODE, modeName(running.mode))
        putInt(KEY_MINUTES, (running.mode as? SleepTimerMode.Duration)?.minutes ?: 0)
        putLong(KEY_REMAINING_MS, running.remainingMs)
        putBoolean(KEY_IS_FADING, running.isFading)
    }

    fun stateOf(extras: Bundle): SleepTimerState {
        val mode = modeOf(extras.getString(KEY_MODE), extras.getInt(KEY_MINUTES)) ?: return SleepTimerState.Off
        return SleepTimerState.Running(mode, extras.getLong(KEY_REMAINING_MS), extras.getBoolean(KEY_IS_FADING))
    }

    fun modeName(mode: SleepTimerMode): String = when (mode) {
        is SleepTimerMode.Duration -> MODE_DURATION
        SleepTimerMode.EndOfChapter -> MODE_END_OF_CHAPTER
        SleepTimerMode.EndOfTrack -> MODE_END_OF_TRACK
    }

    fun modeOf(name: String?, minutes: Int): SleepTimerMode? = when (name) {
        MODE_DURATION -> SleepTimerMode.Duration(minutes)
        MODE_END_OF_CHAPTER -> SleepTimerMode.EndOfChapter
        MODE_END_OF_TRACK -> SleepTimerMode.EndOfTrack
        else -> null
    }
}
