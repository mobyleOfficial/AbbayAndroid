package com.mobyle.abbay.presentation.playback.extras

import com.statistics.ListeningDay
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Measures listening for the statistics: wall-clock time while audio plays, and
 * book time — wall time × speed, plus the silence the trimmer removed. Times come from a monotonic clock; [take] hands
 * over what was measured since the previous call. Pure logic, tested on the JVM.
 */
class ListeningAccumulator {
    private var bookId: String? = null
    private var segmentStartMs: Long? = null
    private var speed = 1f
    private var wallMs = 0L
    private var spokenMs = 0.0
    private var silenceMs = 0L

    val currentBookId: String? get() = bookId

    /** Audio of [bookId] started (or kept) playing at [nowMs]. A different book must be [take]n first. */
    fun play(bookId: String, nowMs: Long, speed: Float) {
        if (this.bookId != bookId) reset(bookId)
        closeSegment(nowMs)
        this.speed = speed
        segmentStartMs = nowMs
    }

    fun pause(nowMs: Long) {
        closeSegment(nowMs)
        segmentStartMs = null
    }

    fun setSpeed(nowMs: Long, speed: Float) {
        val isPlaying = segmentStartMs != null
        closeSegment(nowMs)
        this.speed = speed
        if (isPlaying) segmentStartMs = nowMs
    }

    fun addSilence(bookId: String, savedMs: Long) {
        if (bookId == this.bookId && savedMs > 0) silenceMs += savedMs
    }

    /** What was heard since the last call, dated [date]; null when nothing was. Playback keeps being measured. */
    fun take(nowMs: Long, date: LocalDate): ListeningDay? {
        val id = bookId ?: return null
        val isPlaying = segmentStartMs != null
        closeSegment(nowMs)
        if (isPlaying) segmentStartMs = nowMs
        val wall = wallMs
        val spoken = spokenMs.roundToLong()
        val silence = silenceMs
        wallMs = 0L
        spokenMs = 0.0
        silenceMs = 0L
        if (wall <= 0L && silence <= 0L) return null
        return ListeningDay(
            bookId = id,
            date = date,
            bookTimeMs = spoken + silence,
            wallTimeMs = wall,
            savedBySpeedMs = (spoken - wall).coerceAtLeast(0L),
            savedBySilenceMs = silence,
        )
    }

    private fun closeSegment(nowMs: Long) {
        val start = segmentStartMs ?: return
        val elapsed = (nowMs - start).coerceAtLeast(0L)
        wallMs += elapsed
        spokenMs += elapsed * speed.toDouble()
        segmentStartMs = nowMs
    }

    private fun reset(newBookId: String) {
        bookId = newBookId
        segmentStartMs = null
        wallMs = 0L
        spokenMs = 0.0
        silenceMs = 0L
    }
}
