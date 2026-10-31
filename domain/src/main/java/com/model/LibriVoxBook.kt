package com.model

/**
 * A book from an online catalog, before it is in the library: LibriVox, or an Audiobookshelf server. Each section is
 * one audio file; [chapters] are the book's own chapters when the catalog knows them (a single-file book, say).
 */
data class LibriVoxBook(
    val id: String,
    val title: String,
    val authors: String,
    val creator: String?,
    val coverUrl: String,
    val description: String = "",
    val language: String = "",
    val durationMs: Long = 0L,
    val sections: List<LibriVoxSection> = emptyList(),
    val chapters: List<CatalogChapter> = emptyList(),
) {
    val totalBytes: Long get() = sections.sumOf { it.sizeBytes }

    companion object {
        private const val COVER_ENDPOINT = "https://archive.org/services/img/"

        fun coverUrlOf(identifier: String) = COVER_ENDPOINT + identifier
    }
}

data class LibriVoxSection(
    val title: String,
    val durationMs: Long,
    val listenUrl: String,
    val sizeBytes: Long,
    /** The file's own name, when its url does not end with it. */
    val fileName: String = "",
)

data class CatalogChapter(val title: String, val startMs: Long, val endMs: Long)

data class LibriVoxDetails(val book: LibriVoxBook, val isAwaitingTitles: Boolean)
