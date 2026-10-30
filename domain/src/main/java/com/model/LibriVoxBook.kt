package com.model

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
) {
    val totalBytes: Long get() = sections.sumOf { it.sizeBytes }

    companion object {
        private const val COVER_ENDPOINT = "https://archive.org/services/img/"

        fun coverUrlOf(identifier: String) = COVER_ENDPOINT + identifier
    }
}

data class LibriVoxSection(val title: String, val durationMs: Long, val listenUrl: String, val sizeBytes: Long)

data class LibriVoxDetails(val book: LibriVoxBook, val isAwaitingTitles: Boolean)
