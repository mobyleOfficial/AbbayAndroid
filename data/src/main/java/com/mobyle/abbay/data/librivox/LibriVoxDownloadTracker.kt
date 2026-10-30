package com.mobyle.abbay.data.librivox

import com.model.ChapterDownload
import com.model.ChapterDownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibriVoxDownloadTracker @Inject constructor() {
    private val chapters = MutableStateFlow<Map<Long, Map<Long, ChapterDownload>>>(emptyMap())
    val state: StateFlow<Map<Long, Map<Long, ChapterDownload>>> = chapters.asStateFlow()

    fun downloading(bookId: Long, trackId: Long, fraction: Float) = set(
        bookId,
        trackId,
        ChapterDownload(ChapterDownloadStatus.DOWNLOADING, fraction)
    )

    fun failed(bookId: Long, trackId: Long) = set(bookId, trackId, ChapterDownload(ChapterDownloadStatus.FAILED))

    fun finished(bookId: Long, trackId: Long) = chapters.update { all ->
        val remaining = all[bookId].orEmpty() - trackId
        if (remaining.isEmpty()) all - bookId else all + (bookId to remaining)
    }

    fun clear(bookId: Long) = chapters.update { it - bookId }

    fun clearAll() {
        chapters.value = emptyMap()
    }

    private fun set(bookId: Long, trackId: Long, download: ChapterDownload) = chapters.update { all ->
        all + (bookId to (all[bookId].orEmpty() + (trackId to download)))
    }
}
