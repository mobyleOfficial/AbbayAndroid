package com.player

/** Sleep timer modes. */
sealed interface SleepTimerMode {
    data class Duration(val minutes: Int) : SleepTimerMode

    data object EndOfChapter : SleepTimerMode

    data object EndOfTrack : SleepTimerMode
}

/** What the player shows about the sleep timer. */
sealed interface SleepTimerState {
    data object Off : SleepTimerState

    /** [remainingMs] is listening time; it only goes down while audio plays. */
    data class Running(val mode: SleepTimerMode, val remainingMs: Long, val isFading: Boolean) : SleepTimerState
}
