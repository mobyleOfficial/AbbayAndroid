package com.readalong

/** One recognised word. Times are book time in milliseconds, so a transcript stays valid whatever track produced it. */
data class ReadAlongWord(val text: String, val startMs: Long, val endMs: Long)

/** A run of words the recogniser returned as one phrase: the unit the text pane shows and scrolls. */
data class ReadAlongLine(val startMs: Long, val endMs: Long, val words: List<ReadAlongWord>) {
    /** Derived from the words, which are the single source of truth, so the two can never disagree. */
    val text: String get() = words.joinToString(separator = " ") { it.text }
}

/** A stretch of book time that has been transcribed, half-open: `startMs <= time < endMs`. */
data class ReadAlongSpan(val startMs: Long, val endMs: Long)

/** What to highlight: a line, and the word being spoken when there is one. */
data class ReadAlongPosition(val line: Int, val word: Int?)

/** What the pane shows at a moment: the line being read, the next one coming, or a long stretch with no speech. */
sealed interface ReadAlongDisplay {
    /** Inside a line, or a pause short enough that the line stays current. */
    data class Line(val position: ReadAlongPosition) : ReadAlongDisplay

    /** Before the next line, which is close enough to show dimmed and light up in place. */
    data class Upcoming(val line: Int) : ReadAlongDisplay

    /** Nothing said for a long stretch: an intro, an interlude, the music after a chapter. */
    data object Music : ReadAlongDisplay
}

/**
 * Everything transcribed so far for one book, in book time. Transcription arrives in runs (ahead of the
 * playhead, after a seek, resumed after a pause), so [add] accepts runs in any order and overlapping ones,
 * and [covered] records which stretches are already done.
 *
 * Mutable on purpose: a run hands over a chunk every few seconds and a whole book's transcript is thousands
 * of lines, so merging in place keeps the work proportional to the chunk.
 */
class ReadAlongTranscript private constructor(
    val language: ReadAlongLanguage,
    lines: List<ReadAlongLine>,
    covered: List<ReadAlongSpan>,
) {
    private val mutableLines = ArrayList(lines)
    private val mutableCovered = ArrayList(covered)

    /** Sorted by [ReadAlongLine.startMs], no duplicates. */
    val lines: List<ReadAlongLine> get() = mutableLines

    /** Merged and sorted: book time already transcribed. */
    val covered: List<ReadAlongSpan> get() = mutableCovered

    constructor(language: ReadAlongLanguage) : this(language, emptyList(), emptyList())

    /**
     * Merges a run into the transcript. An incoming line replaces any stored line starting within
     * [DUPLICATE_TOLERANCE_MS] of it, so overlapping runs re-transcribing the same audio do not duplicate
     * lines. Spans that touch or overlap are merged.
     *
     * Only the stored lines near the chunk's are touched (found by binary search) and the two sorted runs
     * merged there, so the rest is never filtered or re-sorted.
     */
    fun add(newLines: List<ReadAlongLine>, covering: ReadAlongSpan) {
        mergeCovered(covering)
        if (newLines.isEmpty()) return
        val incoming = newLines.sortedBy { it.startMs }
        val first = incoming.first()
        val last = incoming.last()
        // Stored lines outside this window can't be within the tolerance of an incoming one.
        val low = firstIndex { it.startMs >= first.startMs - DUPLICATE_TOLERANCE_MS }
        val high = firstIndex { it.startMs > last.startMs + DUPLICATE_TOLERANCE_MS }
        val merged = ArrayList<ReadAlongLine>(high - low + incoming.size)
        var next = 0
        for (index in low until high) {
            val existing = mutableLines[index]
            while (next < incoming.size && incoming[next].startMs <= existing.startMs) {
                merged.add(incoming[next])
                next++
            }
            // The incoming lines nearest to it are the last one placed and the next one.
            val nearest = listOfNotNull(incoming.getOrNull(next - 1), incoming.getOrNull(next))
            val isReplaced = nearest.any { withinTolerance(it, existing) }
            if (!isReplaced) merged.add(existing)
        }
        if (next < incoming.size) merged.addAll(incoming.subList(next, incoming.size))
        repeat(high - low) { mutableLines.removeAt(low) }
        mutableLines.addAll(low, merged)
    }

    fun isCovered(timeMs: Long): Boolean = mutableCovered.any { it.startMs <= timeMs && timeMs < it.endMs }

    /**
     * What the pane shows at [timeMs]. Inside a line, or a short pause after one, that line is current.
     * Before the first line or after a longer pause the next line is shown coming, unless it is further
     * away than [MUSIC_BEFORE_LINE_MS] (or there is none), which is music.
     */
    fun displayAt(timeMs: Long): ReadAlongDisplay {
        val position = positionAt(timeMs)
        if (position != null && timeMs < mutableLines[position.line].endMs + HOLD_AFTER_LINE_MS) {
            return ReadAlongDisplay.Line(position)
        }
        val next = firstIndex { it.startMs > timeMs }
        val isMusic = next >= mutableLines.size || mutableLines[next].startMs - timeMs > MUSIC_BEFORE_LINE_MS
        return if (isMusic) ReadAlongDisplay.Music else ReadAlongDisplay.Upcoming(next)
    }

    /**
     * The line being spoken is the last one that has started; the gap after it holds that line rather than
     * blanking the text. [ReadAlongPosition.word] is null between words.
     */
    fun positionAt(timeMs: Long): ReadAlongPosition? {
        val index = lastIndex { it.startMs <= timeMs }
        if (index < 0) return null
        val word = mutableLines[index].words.indexOfFirst { it.startMs <= timeMs && timeMs < it.endMs }
        return ReadAlongPosition(index, word.takeIf { it >= 0 })
    }

    /** The index of the first line for which [isAtOrPast] holds (it must hold for every line after it too). */
    private fun firstIndex(isAtOrPast: (ReadAlongLine) -> Boolean): Int {
        var low = 0
        var high = mutableLines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (isAtOrPast(mutableLines[middle])) high = middle else low = middle + 1
        }
        return low
    }

    private fun lastIndex(isAtOrPast: (ReadAlongLine) -> Boolean): Int {
        var low = 0
        var high = mutableLines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (isAtOrPast(mutableLines[middle])) low = middle + 1 else high = middle
        }
        return low - 1
    }

    private fun mergeCovered(span: ReadAlongSpan) {
        if (span.endMs <= span.startMs) return
        val merged = ArrayList<ReadAlongSpan>((mutableCovered + span).sortedBy { it.startMs })
        val result = ArrayList<ReadAlongSpan>(merged.size)
        for (next in merged) {
            val last = result.lastOrNull()
            if (last != null && next.startMs <= last.endMs) {
                result[result.lastIndex] = last.copy(endMs = maxOf(last.endMs, next.endMs))
            } else {
                result.add(next)
            }
        }
        mutableCovered.clear()
        mutableCovered.addAll(result)
    }

    private fun withinTolerance(incoming: ReadAlongLine, existing: ReadAlongLine): Boolean =
        kotlin.math.abs(incoming.startMs - existing.startMs) <= DUPLICATE_TOLERANCE_MS

    companion object {
        /** How close two line starts must be to count as the same line seen by two runs. */
        const val DUPLICATE_TOLERANCE_MS = 50L

        /** A pause this long after a line ends is a break, not a breath: the line stops being current. */
        const val HOLD_AFTER_LINE_MS = 5_000L

        /** Silence or music until the next line further away than this shows the music mark rather than the text coming. */
        const val MUSIC_BEFORE_LINE_MS = 30_000L

        /** A transcript read back from storage. */
        fun of(language: ReadAlongLanguage, lines: List<ReadAlongLine>, covered: List<ReadAlongSpan>): ReadAlongTranscript =
            ReadAlongTranscript(language, lines.sortedBy { it.startMs }, covered)
    }
}
