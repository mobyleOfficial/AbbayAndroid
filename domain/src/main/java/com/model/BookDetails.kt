package com.model

data class BookDetails(
    val id: String,
    val title: String,
    val author: String?,
    val narrator: String?,
    val description: String?,
    val coverUri: String?,
    val durationMs: Long,
    val fileCount: Int,
    val location: String,
    val chapters: List<Chapter>,
    val unplayableFiles: List<UnplayableFile>,
    val sortByTrackNumber: Boolean,
    val overrides: BookOverrides,
)

data class BookOverrides(
    val title: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val description: String? = null,
)

data class UnplayableFile(val fileName: String, val isUnsupportedFormat: Boolean)

data class CoverCandidate(val uri: String, val name: String, val isEmbedded: Boolean)
