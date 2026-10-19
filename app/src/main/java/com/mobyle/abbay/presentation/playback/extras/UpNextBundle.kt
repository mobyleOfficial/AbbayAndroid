package com.mobyle.abbay.presentation.playback.extras

import android.os.Bundle
import com.player.UpNext

/** The queue countdown as session extras, next to the sleep timer's. */
object UpNextBundle {
    private const val KEY_BOOK_ID = "upNextBookId"
    private const val KEY_TITLE = "upNextTitle"
    private const val KEY_SECONDS_LEFT = "upNextSecondsLeft"

    fun putInto(extras: Bundle, upNext: UpNext?) {
        upNext ?: return
        extras.putString(KEY_BOOK_ID, upNext.bookId)
        extras.putString(KEY_TITLE, upNext.title)
        extras.putInt(KEY_SECONDS_LEFT, upNext.secondsLeft)
    }

    fun upNextOf(extras: Bundle): UpNext? {
        val bookId = extras.getString(KEY_BOOK_ID) ?: return null
        return UpNext(bookId, extras.getString(KEY_TITLE).orEmpty(), extras.getInt(KEY_SECONDS_LEFT))
    }
}
