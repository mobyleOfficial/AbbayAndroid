package com.repository

import com.subtitles.TrackSubtitles

interface SubtitleRepository {
    suspend fun subtitles(bookId: String, trackIndex: Int): TrackSubtitles?

    suspend fun setCharset(bookId: String, charset: String?)
}
