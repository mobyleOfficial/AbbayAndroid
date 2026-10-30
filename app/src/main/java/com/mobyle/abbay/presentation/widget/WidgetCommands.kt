package com.mobyle.abbay.presentation.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.player.PlaybackIntents

/**
 * What the widget buttons send: media keys to the playback service, which also
 * work with the app closed (playback resumption loads the last book). The names are in [PlaybackIntents], because
 * `:widget` depends only on `:domain`.
 */
object WidgetCommands {
    const val ACTION_ADD_BOOKMARK = PlaybackIntents.ACTION_ADD_BOOKMARK
    const val ACTION_TOGGLE_SLEEP_TIMER = PlaybackIntents.ACTION_TOGGLE_SLEEP_TIMER

    fun mediaKey(context: Context, keyCode: Int): Intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        .setComponent(ComponentName(context.packageName, PlaybackIntents.SERVICE_CLASS))
        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        .putExtra(PlaybackIntents.EXTRA_FROM_WIDGET, true)

    fun serviceAction(context: Context, action: String): Intent =
        Intent(action).setComponent(ComponentName(context.packageName, PlaybackIntents.SERVICE_CLASS))
}
