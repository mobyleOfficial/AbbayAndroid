package com.readalong

/**
 * Where a book's audio is on the device: its files, and the book time each one starts at. The transcriber reads files,
 * not the player's timeline, so it needs this to start from any book time; the text needs it to say the audio must be
 * on the device first. A file streamed from a server (LibriVox, Audiobookshelf) that isn't downloaded is a segment with
 * no [Segment.uri]: read-along only ever reads what is already here and never fetches audio.
 */
data class ReadAlongAudioSource(val segments: List<Segment>, val isDownloadable: Boolean) {
    data class Segment(
        /** A `content:` or `file:` uri; null when the file isn't on the device. */
        val uri: String?,
        /** Book time of the file's 0. */
        val bookStartMs: Long,
        val durationMs: Long,
    ) {
        val bookEndMs: Long get() = bookStartMs + durationMs
    }

    /** One file to read for a run: the segment, and how far into its file to start. */
    data class Reading(val segment: Segment, val offsetMs: Long) {
        /** Book time of the first sample read: the engine's 0 for this file. */
        val bookStartMs: Long get() = segment.bookStartMs + offsetMs
        val bookEndMs: Long get() = segment.bookEndMs
    }

    /** The segment holding [bookTimeMs]: the last one starting at or before it, clamped to the first and last. */
    fun segmentAt(bookTimeMs: Long): Segment? = segments.lastOrNull { it.bookStartMs <= bookTimeMs } ?: segments.firstOrNull()

    fun isOnDevice(bookTimeMs: Long): Boolean = segmentAt(bookTimeMs)?.uri != null

    /**
     * The files a run starting at [startMs] reads, in order: the one holding it from that point, then each following one
     * from its start, stopping before the first that isn't on the device. Empty past the end or when its own file is missing.
     */
    fun readings(startMs: Long): List<Reading> {
        if (segments.isEmpty()) return emptyList()
        val first = segments.indexOfLast { it.bookStartMs <= startMs }.coerceAtLeast(0)
        val result = ArrayList<Reading>()
        for (index in first until segments.size) {
            val segment = segments[index]
            if (segment.uri == null) break
            val offset = if (index == first) (startMs - segment.bookStartMs).coerceAtLeast(0L) else 0L
            if (offset < segment.durationMs) result += Reading(segment, offset)
        }
        return result
    }
}
