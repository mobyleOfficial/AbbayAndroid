package com.repository

import com.subtitles.TrackSubtitles

interface SubtitleRepository {
    /** The `.srt` or `.lrc` file with the same base name as the book's playable track [trackIndex], or null. */
    suspend fun subtitles(bookId: String, trackIndex: Int): TrackSubtitles?

    /** The charset for the book's subtitle files that are not UTF-8 or UTF-16 with BOM; null goes back to detection. */
    suspend fun setCharset(bookId: String, charset: String?)
}
