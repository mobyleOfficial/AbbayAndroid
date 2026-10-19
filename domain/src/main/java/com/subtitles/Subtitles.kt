package com.subtitles

/** One line of a `.srt` or `.lrc` file, in track time. */
data class SubtitleLine(val startMs: Long, val text: String)

/**
 * The text of one track. [needsCharset] is true when the file is neither UTF-8 nor UTF-16 with a byte-order mark and
 * no charset was chosen for the book: the user picks one, and [lines] is empty until then.
 */
data class TrackSubtitles(val fileName: String, val lines: List<SubtitleLine>, val charset: String?, val needsCharset: Boolean)

object SubtitleTimeline {
    /** Index of the line playing at [positionMs]: the last one that started; -1 before the first. */
    fun indexAt(lines: List<SubtitleLine>, positionMs: Long): Int {
        var low = 0
        var high = lines.lastIndex
        var found = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].startMs <= positionMs) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return found
    }
}
