package com.listening

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToLong

object SmartRewind {
    const val MINIMUM_PAUSE_MS = 1_000L

    const val MOVED_TOLERANCE_MS = 1_000L

    fun rewindMs(pauseMs: Long, level: SmartRewindLevel): Long {
        if (level == SmartRewindLevel.OFF || pauseMs < MINIMUM_PAUSE_MS) return 0L
        val pauseSeconds = pauseMs / MILLISECONDS_PER_SECOND
        val seconds = min(level.maximumSeconds, level.baseSeconds + level.growth * ln(1 + pauseSeconds / PAUSE_SCALE_SECONDS))
        return (seconds * MILLISECONDS_PER_SECOND).roundToLong()
    }

    fun resumePositionMs(
        bookPositionMs: Long,
        pausedBookPositionMs: Long,
        pauseMs: Long,
        level: SmartRewindLevel,
        extraRewindMs: Long = 0L,
    ): Long {
        if (abs(bookPositionMs - pausedBookPositionMs) > MOVED_TOLERANCE_MS) return bookPositionMs
        val rewind = rewindMs(pauseMs, level) + extraRewindMs.coerceAtLeast(0L)
        return (bookPositionMs - rewind).coerceAtLeast(0L)
    }

    private const val MILLISECONDS_PER_SECOND = 1_000.0
    private const val PAUSE_SCALE_SECONDS = 10.0
}
