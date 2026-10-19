package com.mobyle.abbay.presentation.playback.command

/**
 * Book-time arithmetic over the track durations of the loaded book. Pure logic, unit-tested on the JVM.
 */
@Suppress("TooManyFunctions") // One function per book-time operation.
class BookNavigator(private val trackDurationsMs: List<Long>, private val chapterStartsMs: List<Long> = emptyList()) {
    /** False when a duration is unknown; book-time seeks then stay inside the current track. */
    private val areDurationsKnown = trackDurationsMs.isNotEmpty() && trackDurationsMs.all { it > 0L }

    val bookDurationMs: Long = trackDurationsMs.sumOf { it.coerceAtLeast(0L) }

    fun bookPositionOf(position: TrackPosition): Long =
        trackDurationsMs.take(position.trackIndex).sumOf { it.coerceAtLeast(0L) } + position.positionMs

    /** The track position at [bookPositionMs], clamped to the book; the very end is the end of the last track. */
    fun positionAt(bookPositionMs: Long): TrackPosition {
        if (trackDurationsMs.isEmpty()) return TrackPosition(0, 0L)
        var remaining = bookPositionMs.coerceIn(0L, bookDurationMs)

        trackDurationsMs.forEachIndexed { index, duration ->
            if (remaining < duration || index == trackDurationsMs.lastIndex) {
                return TrackPosition(index, remaining.coerceAtMost(duration))
            }
            remaining -= duration
        }
        return TrackPosition(trackDurationsMs.lastIndex, trackDurationsMs.last())
    }

    fun seekBy(current: TrackPosition, deltaMs: Long): TrackPosition =
        if (areDurationsKnown) {
            positionAt(bookPositionOf(current) + deltaMs)
        } else {
            current.copy(positionMs = (current.positionMs + deltaMs).coerceAtLeast(0L))
        }

    /** The start of the next track, or null at the last track. */
    fun nextTrack(current: TrackPosition): TrackPosition? =
        if (current.trackIndex < trackDurationsMs.lastIndex) TrackPosition(current.trackIndex + 1, 0L) else null

    /** More than 3 s into a track restarts it; otherwise goes to the previous track (or the start of the first). */
    fun previousTrack(current: TrackPosition): TrackPosition = when {
        current.positionMs > RESTART_THRESHOLD_MS -> TrackPosition(current.trackIndex, 0L)
        current.trackIndex > 0 -> TrackPosition(current.trackIndex - 1, 0L)
        else -> TrackPosition(0, 0L)
    }

    /** Index of the chapter at [bookPositionMs]; tracks are the chapters when the book has no chapter list. */
    fun chapterIndexAt(bookPositionMs: Long): Int = chapterStarts().indexOfLast { it <= bookPositionMs }.coerceAtLeast(0)

    /** The start of the next chapter, or null in the last chapter. */
    fun nextChapter(current: TrackPosition): TrackPosition? {
        val starts = chapterStarts()
        val next = starts.getOrNull(chapterIndexAt(bookPositionOf(current)) + 1) ?: return null
        return positionAt(next)
    }

    /** More than 3 s into a chapter restarts it; otherwise goes to the previous chapter. */
    fun previousChapter(current: TrackPosition): TrackPosition {
        val starts = chapterStarts()
        val position = bookPositionOf(current)
        val index = chapterIndexAt(position)
        val target = if (position - starts[index] > RESTART_THRESHOLD_MS || index == 0) starts[index] else starts[index - 1]
        return positionAt(target)
    }

    /** Start of chapter [index] in book time (tracks are the chapters when the book has no chapter list). */
    fun chapterStartAt(index: Int): Long = chapterStarts().getOrElse(index) { 0L }

    /** Where the chapter at [bookPositionMs] ends: the next chapter's start, or the end of the book. */
    fun chapterEndAt(bookPositionMs: Long): Long = chapterStarts().getOrNull(chapterIndexAt(bookPositionMs) + 1) ?: bookDurationMs

    /** True when [bookPositionMs] is the start of a chapter (within [toleranceMs]). */
    fun isChapterStart(bookPositionMs: Long, toleranceMs: Long = CHAPTER_START_TOLERANCE_MS): Boolean =
        chapterStarts().any { kotlin.math.abs(it - bookPositionMs) <= toleranceMs }

    private fun chapterStarts(): List<Long> = chapterStartsMs.ifEmpty {
        trackDurationsMs.runningFold(0L) { start, duration -> start + duration.coerceAtLeast(0L) }.dropLast(1).ifEmpty { listOf(0L) }
    }

    companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
        const val CHAPTER_START_TOLERANCE_MS = 50L
    }
}
