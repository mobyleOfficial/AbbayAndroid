package com.model

/** Everything the book details screen shows. */
data class BookDetails(
    val id: String,
    val title: String,
    val author: String?,
    val narrator: String?,
    val description: String?,
    val coverUri: String?,
    val durationMs: Long,
    val fileCount: Int,
    /** Folder path inside the library root, or the file name of a single picked file. */
    val location: String,
    val chapters: List<Chapter>,
    val unplayableFiles: List<UnplayableFile>,
    val sortByTrackNumber: Boolean,
    /** The values the user typed, null when the field shows the scanned value. */
    val overrides: BookOverrides,
)

data class BookOverrides(
    val title: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val description: String? = null,
)

/** A file that cannot be played; [isUnsupportedFormat] marks formats Abbay never plays, such as WMA. */
data class UnplayableFile(val fileName: String, val isUnsupportedFormat: Boolean)

/** An image that can become the book's cover: a picture in the folder or the one embedded in the audio. */
data class CoverCandidate(val uri: String, val name: String, val isEmbedded: Boolean)
