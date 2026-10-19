package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.model.Chapter
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

/** "1.4×" / "1,4×" in the user's locale. */
fun formatSpeed(speed: Float): String {
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 2
    }
    return format.format(speed) + "×"
}

/** Chapter title, or "Chapter n" when the file has none. */
@Composable
fun chapterTitle(chapter: Chapter?): String = when {
    chapter == null -> ""
    chapter.title.isNotBlank() -> chapter.title
    else -> stringResource(R.string.player_chapter_untitled, chapter.index + 1)
}

/** "x h yy min left" at the current speed. */
@Composable
fun timeLeftLabel(remainingBookMs: Long, speed: Float): String {
    val wallClockMs = (remainingBookMs / speed.coerceAtLeast(MINIMUM_SPEED)).toLong().coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(wallClockMs).toInt()
    val minutes = (TimeUnit.MILLISECONDS.toMinutes(wallClockMs) % MINUTES_PER_HOUR).toInt()
    val duration = if (hours > 0) {
        stringResource(R.string.player_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.player_minutes, minutes)
    }
    return stringResource(R.string.player_time_left, duration)
}

private const val MINIMUM_SPEED = 0.5f
private const val MINUTES_PER_HOUR = 60
