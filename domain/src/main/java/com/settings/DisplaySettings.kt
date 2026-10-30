package com.settings

/** What the library does when the app starts (`rescanOnStart`). */
enum class RescanOnStart {
    /** Only checks that the folders are still reachable. */
    OFF,
    QUICK,
    FULL,
}

/** The two side buttons of the notification and lock screen. */
enum class NotificationSkipButtons {
    /** Back 15 s · Forward 30 s (the small skips). */
    TIME_SKIP,
    PREVIOUS_AND_NEXT_CHAPTER,
}

/** The labels under the player's chapter seek bar (`progressLabels`). */
enum class ProgressLabels {
    ELAPSED_AND_REMAINING,
    ELAPSED_AND_TOTAL,
}

/** The player's quick actions that can be hidden (Speed lives in the top bar). */
enum class QuickAction {
    SLEEP_TIMER,
    BOOKMARK,
    AUDIO_EFFECTS,
    REPEAT,
    SKIP_INTRO_OUTRO,
}
