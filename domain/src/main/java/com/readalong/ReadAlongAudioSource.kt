package com.readalong

/**
 * Where a book's audio is on the device, as files and the book time each one starts at.
 *
 * The transcriber reads files, not the player's timeline, so it needs this to start from any book time. A
 * book made of many files is one segment per file; a single file with chapter markers inside it is one
 * segment covering the whole book.
 *
 * Read-along only ever reads what is already here: a missing file is never fetched.
 */
data class ReadAlongAudioSource(
    val segments: List<Segment>,
    /** True for a book whose audio is a stream until it is downloaded (LibriVox, Audiobookshelf). */
    val isRemote: Boolean = false,
) {
    data class Segment(
        /** Null when the file is not on the device (a LibriVox chapter that was not downloaded). */
        val fileUri: String?,
        /** The book time where this file's 0 is. */
        val bookStartMs: Long,
        val durationMs: Long,
    )

    /** One file to read for a run: the segment, and how far into its file to start. */
    data class Reading(val segment: Segment, val fileUri: String, val offsetMs: Long) {
        /** Book time of the first sample read: the engine's 0 for this file. */
        val bookStartMs: Long get() = segment.bookStartMs + offsetMs
        val bookEndMs: Long get() = segment.bookStartMs + segment.durationMs
    }

    /**
     * The files a run starting at [startMs] reads, in order: the one holding it from that point, then each
     * following one from its start, stopping before the first that isn't on the device.
     */
    fun readingsFrom(startMs: Long): List<Reading> {
        if (segments.isEmpty()) return emptyList()
        val first = segments.indexOfLast { it.bookStartMs <= startMs }.takeIf { it >= 0 } ?: 0
        val result = ArrayList<Reading>(segments.size - first)
        var index = first
        while (index < segments.size) {
            val segment = segments[index]
            val fileUri = segment.fileUri ?: break
            val offsetMs = if (index == first) (startMs - segment.bookStartMs).coerceAtLeast(0L) else 0L
            if (offsetMs < segment.durationMs) result.add(Reading(segment, fileUri, offsetMs))
            index++
        }
        return result
    }

    /** The segment that holds [bookTimeMs]: the last one starting at or before it, clamped to the ends. */
    fun segmentAt(bookTimeMs: Long): Segment? =
        segments.lastOrNull { it.bookStartMs <= bookTimeMs } ?: segments.firstOrNull()

    fun isOnDevice(bookTimeMs: Long): Boolean = segmentAt(bookTimeMs)?.fileUri != null
}

/** What a transcription run hands over as it goes: the lines it finished, and the book time it has covered. */
data class ReadAlongChunk(val lines: List<ReadAlongLine>, val covered: ReadAlongSpan)
