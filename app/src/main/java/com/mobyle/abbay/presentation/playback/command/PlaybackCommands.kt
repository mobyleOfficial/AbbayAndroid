package com.mobyle.abbay.presentation.playback.command

object PlaybackCommands {
    const val SEEK_BY = "com.mobyle.abbay.command.SEEK_BY"
    const val SEEK_TO_BOOK_POSITION = "com.mobyle.abbay.command.SEEK_TO_BOOK_POSITION"
    const val NEXT_TRACK = "com.mobyle.abbay.command.NEXT_TRACK"
    const val PREVIOUS_TRACK = "com.mobyle.abbay.command.PREVIOUS_TRACK"
    const val NEXT_CHAPTER = "com.mobyle.abbay.command.NEXT_CHAPTER"
    const val PREVIOUS_CHAPTER = "com.mobyle.abbay.command.PREVIOUS_CHAPTER"
    const val SKIP_BACK = "com.mobyle.abbay.command.SKIP_BACK"
    const val SKIP_FORWARD = "com.mobyle.abbay.command.SKIP_FORWARD"
    const val ADD_BOOKMARK = "com.mobyle.abbay.command.ADD_BOOKMARK"
    const val SLEEP_TIMER_START = "com.mobyle.abbay.command.SLEEP_TIMER_START"
    const val SLEEP_TIMER_CANCEL = "com.mobyle.abbay.command.SLEEP_TIMER_CANCEL"
    const val SLEEP_TIMER_TOGGLE = "com.mobyle.abbay.command.SLEEP_TIMER_TOGGLE"
    const val SPEED_CYCLE = "com.mobyle.abbay.command.SPEED_CYCLE"
    const val UP_NEXT_CANCEL = "com.mobyle.abbay.command.UP_NEXT_CANCEL"

    const val EXTRA_DELTA_MS = "deltaMs"
    const val EXTRA_POSITION_MS = "positionMs"
    const val EXTRA_SLEEP_MODE = "sleepMode"
    const val EXTRA_SLEEP_MINUTES = "sleepMinutes"

    const val EVENT_PLAYBACK_PROBLEM = "com.mobyle.abbay.event.PLAYBACK_PROBLEM"
    const val EXTRA_PROBLEM = "problem"
    const val EXTRA_FILE_NAME = "fileName"

    val ALL = listOf(
        SEEK_BY,
        SEEK_TO_BOOK_POSITION,
        NEXT_TRACK,
        PREVIOUS_TRACK,
        NEXT_CHAPTER,
        PREVIOUS_CHAPTER,
        SKIP_BACK,
        SKIP_FORWARD,
        ADD_BOOKMARK,
        SLEEP_TIMER_START,
        SLEEP_TIMER_CANCEL,
        SLEEP_TIMER_TOGGLE,
        SPEED_CYCLE,
        UP_NEXT_CANCEL,
    )
}
