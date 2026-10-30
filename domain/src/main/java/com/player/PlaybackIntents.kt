package com.player

/**
 * Intents that reach the playback service from outside the app's UI (home screen widgets). Shared
 * here because `:widget` and `:playback` do not depend on each other.
 */
object PlaybackIntents {
    const val SERVICE_CLASS = "com.mobyle.abbay.presentation.playback.PlaybackService"
    const val ACTION_ADD_BOOKMARK = "com.mobyle.abbay.action.ADD_BOOKMARK"
    const val ACTION_TOGGLE_SLEEP_TIMER = "com.mobyle.abbay.action.TOGGLE_SLEEP_TIMER"

    /** Marks media keys sent by a widget: with nothing loaded, a skip resumes the last book instead. */
    const val EXTRA_FROM_WIDGET = "com.mobyle.abbay.extra.FROM_WIDGET"
}
