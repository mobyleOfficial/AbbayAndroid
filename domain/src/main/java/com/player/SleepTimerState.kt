package com.player

sealed interface SleepTimerMode {
    data class Duration(val minutes: Int) : SleepTimerMode

    data object EndOfChapter : SleepTimerMode

    data object EndOfTrack : SleepTimerMode
}

sealed interface SleepTimerState {
    data object Off : SleepTimerState

    data class Running(val mode: SleepTimerMode, val remainingMs: Long, val isFading: Boolean) : SleepTimerState
}
