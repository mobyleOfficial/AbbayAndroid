package com.readalong

import kotlin.math.abs

/** One recognised word. Times are book time, so a transcript stays valid whatever file or playback speed produced it. */
data class ReadAlongWord(val text: String, val startMs: Long, val endMs: Long)

/**
 * A run of words shown and scrolled as one line. The text is derived from the words, so the two never disagree.
 * [fromBook] when the words are the book's own text (from its EPUB) timed against the audio, rather than recognised.
 */
data class ReadAlongLine(val startMs: Long, val endMs: Long, val words: List<ReadAlongWord>, val fromBook: Boolean = false) {
    val text: String get() = words.joinToString(" ") { it.text }

    /** The same line [deltaMs] later: the engine times from where it began reading, the transcript in book time. */
    fun shifted(deltaMs: Long): ReadAlongLine = copy(
        startMs = startMs + deltaMs,
        endMs = endMs + deltaMs,
        words = words.map { it.copy(startMs = it.startMs + deltaMs, endMs = it.endMs + deltaMs) },
    )

    /** The part of the line before [limitMs]: words starting at or after it dropped. Null when the line starts there or later. */
    fun clipped(limitMs: Long): ReadAlongLine? {
        if (startMs >= limitMs) return null
        val kept = words.filter { it.startMs < limitMs }.map { it.copy(endMs = minOf(it.endMs, limitMs)) }
        return copy(endMs = minOf(endMs, limitMs), words = kept)
    }
}

/** A stretch of book time already transcribed, half-open: `startMs <= time < endMs`. */
data class ReadAlongSpan(val startMs: Long, val endMs: Long) {
    operator fun contains(timeMs: Long): Boolean = timeMs in startMs until endMs
}

/** What to highlight: a line, and the word being spoken when there is one. */
data class ReadAlongPosition(val line: Int, val word: Int?)

/** What the text shows at a moment: the line being read, the next one coming (dimmed), or ♪ for a long stretch without speech. */
sealed interface ReadAlongDisplay {
    data class Line(val position: ReadAlongPosition) : ReadAlongDisplay

    data class Upcoming(val line: Int) : ReadAlongDisplay

    data object Music : ReadAlongDisplay
}

/**
 * Everything transcribed so far for one book, in book time, for one language. Transcription arrives in runs (ahead of
 * the playhead, after a seek, resumed after a pause), so [adding] accepts runs in any order, overlapping ones too, and
 * [covered] records which stretches are done. Immutable: each chunk makes a new transcript, which is cheap next to the
 * recognition that produced it and lets the UI read it from any thread.
 */
data class ReadAlongTranscript(
    val language: String,
    /** Sorted by start, no duplicates. */
    val lines: List<ReadAlongLine> = emptyList(),
    /** Merged and sorted: book time already transcribed. */
    val covered: List<ReadAlongSpan> = emptyList(),
) {
    /**
     * Merges a run in. An incoming line replaces any stored line starting within [DUPLICATE_TOLERANCE_MS] of it, so
     * re-transcribing the same audio never duplicates lines. Spans that touch or overlap are merged. Only the stored
     * lines near the incoming ones are looked at (found by binary search); the rest are carried over untouched.
     */
    fun adding(newLines: List<ReadAlongLine>, span: ReadAlongSpan): ReadAlongTranscript {
        val mergedCovered = mergeCovered(span)
        val incoming = newLines.sortedBy { it.startMs }
        if (incoming.isEmpty()) return copy(covered = mergedCovered)
        val low = firstLine { it.startMs >= incoming.first().startMs - DUPLICATE_TOLERANCE_MS }
        val high = firstLine { it.startMs > incoming.last().startMs + DUPLICATE_TOLERANCE_MS }
        val window = ArrayList<ReadAlongLine>(high - low + incoming.size)
        var next = 0
        for (existing in lines.subList(low, high)) {
            while (next < incoming.size && incoming[next].startMs <= existing.startMs) window += incoming[next++]
            val isReplaced = listOf(next - 1, next).any { index ->
                index in incoming.indices && abs(incoming[index].startMs - existing.startMs) <= DUPLICATE_TOLERANCE_MS
            }
            if (!isReplaced) window += existing
        }
        window += incoming.subList(next, incoming.size)
        val merged = ArrayList<ReadAlongLine>(lines.size - (high - low) + window.size)
        merged += lines.subList(0, low)
        merged += window
        merged += lines.subList(high, lines.size)
        return ReadAlongTranscript(language, merged, mergedCovered)
    }

    fun isCovered(timeMs: Long): Boolean = covered.any { timeMs in it }

    fun coveringSpan(timeMs: Long): ReadAlongSpan? = covered.firstOrNull { timeMs in it }

    /**
     * What the text shows at [timeMs]. Inside a line, or a short pause after one, that line is current. Before the first
     * line or after a longer pause the next line is shown coming, unless it is more than [MUSIC_BEFORE_LINE_MS] away
     * (or there is none): that is music.
     */
    fun display(timeMs: Long): ReadAlongDisplay {
        val position = position(timeMs)?.takeIf { timeMs < lines[it.line].endMs + HOLD_AFTER_LINE_MS }
        val next = firstLine { it.startMs > timeMs }
        return when {
            position != null -> ReadAlongDisplay.Line(position)
            next < lines.size && lines[next].startMs - timeMs <= MUSIC_BEFORE_LINE_MS -> ReadAlongDisplay.Upcoming(next)
            else -> ReadAlongDisplay.Music
        }
    }

    /** The line being spoken is the last one that has started; [ReadAlongPosition.word] is null between words. */
    fun position(timeMs: Long): ReadAlongPosition? {
        val started = firstLine { it.startMs > timeMs }
        if (started == 0) return null
        val index = started - 1
        val word = lines[index].words.indexOfFirst { timeMs >= it.startMs && timeMs < it.endMs }.takeIf { it >= 0 }
        return ReadAlongPosition(index, word)
    }

    /** The index of the first line for which [isAtOrPast] holds (it must hold for every later line too); `lines.size` when none. */
    private inline fun firstLine(isAtOrPast: (ReadAlongLine) -> Boolean): Int {
        var low = 0
        var high = lines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (isAtOrPast(lines[middle])) high = middle else low = middle + 1
        }
        return low
    }

    private fun mergeCovered(span: ReadAlongSpan): List<ReadAlongSpan> {
        if (span.endMs <= span.startMs) return covered
        val merged = ArrayList<ReadAlongSpan>(covered.size + 1)
        for (next in (covered + span).sortedBy { it.startMs }) {
            val last = merged.lastOrNull()
            if (last != null && next.startMs <= last.endMs) {
                merged[merged.lastIndex] = ReadAlongSpan(last.startMs, maxOf(last.endMs, next.endMs))
            } else {
                merged += next
            }
        }
        return merged
    }

    companion object {
        /** How close two line starts must be to count as the same line seen by two runs. */
        const val DUPLICATE_TOLERANCE_MS = 50L

        /** A pause this long after a line ends is a break, not a breath: the line stops being current. */
        const val HOLD_AFTER_LINE_MS = 5_000L

        /** Silence until a line further away than this shows ♪ rather than the line coming (a long intro or interlude). */
        const val MUSIC_BEFORE_LINE_MS = 30_000L
    }
}
