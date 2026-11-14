package com.model

enum class ChapterDownloadStatus {
    DOWNLOADED,
    DOWNLOADING,
    WAITING,
    FAILED,
}

data class ChapterDownload(val status: ChapterDownloadStatus, val fraction: Float = 0f)

enum class LibriVoxDownloadMode {
    ACTIVE,
    PAUSED,
    OFF,
}

data class LibriVoxDownload(
    val chapters: List<ChapterDownload>,
    val fraction: Float,
    val mode: LibriVoxDownloadMode = LibriVoxDownloadMode.ACTIVE,
) {
    val isComplete: Boolean get() = chapters.all { it.status == ChapterDownloadStatus.DOWNLOADED }

    val isInProgress: Boolean get() = !isComplete && mode != LibriVoxDownloadMode.OFF

    val hasDownloadedChapters: Boolean get() = chapters.any { it.status == ChapterDownloadStatus.DOWNLOADED }

    val hasFailures: Boolean get() = chapters.any { it.status == ChapterDownloadStatus.FAILED }

    companion object {
        fun of(
            chapters: List<ChapterDownload>,
            expectedBytes: List<Long>,
            mode: LibriVoxDownloadMode = LibriVoxDownloadMode.ACTIVE,
        ): LibriVoxDownload {
            if (chapters.isEmpty()) return LibriVoxDownload(chapters, 0f, mode)
            val done = chapters.map { chapter ->
                when (chapter.status) {
                    ChapterDownloadStatus.DOWNLOADED -> 1f
                    ChapterDownloadStatus.DOWNLOADING -> chapter.fraction.coerceIn(0f, 1f)
                    ChapterDownloadStatus.WAITING, ChapterDownloadStatus.FAILED -> 0f
                }
            }
            val sizes = chapters.indices.map { index -> expectedBytes.getOrNull(index)?.coerceAtLeast(0L) ?: 0L }
            val totalBytes = sizes.sum()
            val fraction = if (totalBytes > 0L) {
                done.zip(sizes).sumOf { (part, size) -> part.toDouble() * size }.div(totalBytes).toFloat()
            } else {
                done.sum() / chapters.size
            }
            return LibriVoxDownload(chapters, fraction, mode)
        }
    }
}

data class NetworkStatus(val isOnline: Boolean, val isMetered: Boolean)
