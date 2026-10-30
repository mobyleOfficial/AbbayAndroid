package com.mobyle.abbay.presentation.widget

import android.content.Context
import com.player.NowPlaying

internal class NowPlayingStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): NowPlaying? {
        val title = preferences.getString(KEY_TITLE, null) ?: return null
        return NowPlaying(
            bookTitle = title,
            chapterTitle = preferences.getString(KEY_CHAPTER, "").orEmpty(),
            coverUri = preferences.getString(KEY_COVER, null),
            isPlaying = preferences.getBoolean(KEY_PLAYING, false),
            chapterProgress = preferences.getFloat(KEY_PROGRESS, 0f),
            isSleepTimerRunning = preferences.getBoolean(KEY_SLEEP, false),
        )
    }

    fun write(nowPlaying: NowPlaying?) {
        preferences.edit().apply {
            if (nowPlaying == null) {
                clear()
            } else {
                putString(KEY_TITLE, nowPlaying.bookTitle)
                putString(KEY_CHAPTER, nowPlaying.chapterTitle)
                putString(KEY_COVER, nowPlaying.coverUri)
                putBoolean(KEY_PLAYING, nowPlaying.isPlaying)
                putFloat(KEY_PROGRESS, nowPlaying.chapterProgress)
                putBoolean(KEY_SLEEP, nowPlaying.isSleepTimerRunning)
            }
        }.apply()
    }

    private companion object {
        const val FILE_NAME = "now_playing_widget"
        const val KEY_TITLE = "title"
        const val KEY_CHAPTER = "chapter"
        const val KEY_COVER = "cover"
        const val KEY_PLAYING = "playing"
        const val KEY_PROGRESS = "progress"
        const val KEY_SLEEP = "sleep"
    }
}
