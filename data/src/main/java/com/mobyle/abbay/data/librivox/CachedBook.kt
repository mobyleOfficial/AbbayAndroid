package com.mobyle.abbay.data.librivox

import com.model.LibriVoxBook
import com.model.LibriVoxSection
import kotlinx.serialization.Serializable

@Serializable
data class CachedBook(
    val id: String,
    val title: String,
    val authors: String,
    val creator: String? = null,
    val coverUrl: String,
    val description: String = "",
    val language: String = "",
    val durationMs: Long = 0L,
    val sections: List<CachedSection> = emptyList(),
) {
    fun toDomain() = LibriVoxBook(
        id = id,
        title = title,
        authors = authors,
        creator = creator,
        coverUrl = coverUrl,
        description = description,
        language = language,
        durationMs = durationMs,
        sections = sections.map { LibriVoxSection(it.title, it.durationMs, it.listenUrl, it.sizeBytes) },
    )

    companion object {
        fun of(book: LibriVoxBook) = CachedBook(
            id = book.id,
            title = book.title,
            authors = book.authors,
            creator = book.creator,
            coverUrl = book.coverUrl,
            description = book.description,
            language = book.language,
            durationMs = book.durationMs,
            sections = book.sections.map { CachedSection(it.title, it.durationMs, it.listenUrl, it.sizeBytes) },
        )
    }
}

@Serializable
data class CachedSection(val title: String, val durationMs: Long, val listenUrl: String, val sizeBytes: Long)

@Serializable
data class CachedMapping(val librivoxId: String?)
