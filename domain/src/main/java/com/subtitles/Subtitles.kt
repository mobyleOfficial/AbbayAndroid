package com.subtitles

data class SubtitleLine(val startMs: Long, val text: String)

data class TrackSubtitles(val fileName: String, val lines: List<SubtitleLine>, val charset: String?, val needsCharset: Boolean)

object SubtitleTimeline {
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
