package com.mobyle.abbay.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.player.NowPlaying
import com.player.NowPlayingPublisher

/** Stores what is playing and redraws the three widgets. */
class WidgetNowPlayingPublisher(private val context: Context) : NowPlayingPublisher {
    private val store = NowPlayingStore(context)

    override suspend fun publish(nowPlaying: NowPlaying?) {
        if (store.read() == nowPlaying) return
        store.write(nowPlaying)
        MiniPlayerWidget().updateAll(context)
        StandardPlayerWidget().updateAll(context)
        LargePlayerWidget().updateAll(context)
    }
}
