package com.mobyle.abbay.data.librivox

import com.library.NaturalOrderComparator
import com.model.LibriVoxBook
import com.model.LibriVoxSection

object ArchiveCatalog {
    const val CHAPTER_FORMAT = "64Kbps MP3"
    const val SEARCH_ENDPOINT = "https://archive.org/advancedsearch.php"
    const val METADATA_ENDPOINT = "https://archive.org/metadata/"
    const val DOWNLOAD_ENDPOINT = "https://archive.org/download/"
    const val FEED_ENDPOINT = "https://librivox.org/api/feed/audiobooks/"

    private const val MILLIS_PER_SECOND = 1_000.0
    private const val SECONDS_PER_UNIT = 60.0
    private val naturalOrder = NaturalOrderComparator()

    fun coverUrl(identifier: String) = LibriVoxBook.coverUrlOf(identifier)

    fun millis(length: String?): Long? {
        val text = length?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (':' !in text) return text.toDoubleOrNull()?.takeIf { it >= 0 }?.let { (it * MILLIS_PER_SECOND).toLong() }
        var total = 0.0
        for (part in text.split(':')) {
            val value = part.toDoubleOrNull()?.takeIf { it >= 0 } ?: return null
            total = total * SECONDS_PER_UNIT + value
        }
        return (total * MILLIS_PER_SECOND).toLong()
    }

    fun trackNumber(track: String?): Int? = track?.substringBefore('/')?.trim()?.toIntOrNull()

    fun chapterFiles(files: List<ArchiveFile>): List<ArchiveFile> = files
        .filter { it.format == CHAPTER_FORMAT && !it.name.isNullOrEmpty() }
        .sortedWith(
            compareBy<ArchiveFile> { trackNumber(it.track) ?: Int.MAX_VALUE }
                .thenBy(naturalOrder) { it.name.orEmpty() },
        )

    fun fallbackSection(file: ArchiveFile, identifier: String): LibriVoxSection? {
        val name = file.name?.takeIf(String::isNotEmpty) ?: return null
        return LibriVoxSection(
            title = file.title?.trim()?.takeIf(String::isNotEmpty) ?: name.substringBeforeLast('.'),
            durationMs = millis(file.length) ?: 0L,
            listenUrl = "$DOWNLOAD_ENDPOINT$identifier/$name",
            sizeBytes = file.size?.toLongOrNull() ?: 0L,
        )
    }

    fun lightBook(doc: ArchiveSearchDoc): LibriVoxBook? {
        val identifier = doc.identifier?.takeIf(String::isNotEmpty) ?: return null
        val title = doc.title?.takeIf(String::isNotEmpty) ?: return null
        if (doc.mediatype != null && doc.mediatype != "audio") return null
        return LibriVoxBook(
            id = identifier,
            title = CatalogText.cleanTitle(title, doc.creators.firstOrNull()),
            authors = doc.creators.joinToString(", "),
            creator = doc.creators.firstOrNull(),
            coverUrl = coverUrl(identifier),
            language = doc.languages.firstOrNull().orEmpty(),
        )
    }

    fun book(item: ArchiveItem, match: LibriVoxFeedBook?): LibriVoxBook? {
        val sizes = item.files.mapNotNull { file -> file.name?.let { it to (file.size?.toLongOrNull() ?: 0L) } }.toMap()
        val lengths = item.files.mapNotNull { file -> file.name?.let { name -> millis(file.length)?.let { name to it } } }.toMap()
        val matchedSections = match?.let { sections(it.sections, lengths, sizes) }
        val sections = matchedSections
            ?: chapterFiles(item.files).mapNotNull { fallbackSection(it, item.identifier) }
        if (sections.isEmpty()) return null
        val summary = match?.description?.takeIf { matchedSections != null && CatalogText.stripHtml(it).isNotEmpty() } ?: item.description
        val sum = sections.sumOf { it.durationMs }
        return LibriVoxBook(
            id = item.identifier,
            title = CatalogText.cleanTitle(item.title ?: item.identifier, item.creator),
            authors = item.creators.joinToString(", "),
            creator = item.creator,
            coverUrl = coverUrl(item.identifier),
            description = CatalogText.stripHtml(summary.orEmpty()),
            language = item.languages.firstOrNull().orEmpty(),
            durationMs = if (sum > 0) sum else millis(item.runtime) ?: 0L,
            sections = sections,
        )
    }

    fun sections(raw: List<LibriVoxFeedSection>, lengths: Map<String, Long>, sizes: Map<String, Long>): List<LibriVoxSection>? {
        if (raw.isEmpty()) return null
        return raw.mapIndexed { index, section ->
            val url = section.listenUrl?.takeIf(String::isNotEmpty) ?: return null
            val fileName = url.substringAfterLast('/')
            val playtime = section.playtime?.toDoubleOrNull()?.takeIf { it > 0 }?.let { (it * MILLIS_PER_SECOND).toLong() }
            LibriVoxSection(
                title = section.title?.takeIf(String::isNotEmpty) ?: "${index + 1}",
                durationMs = playtime ?: lengths[fileName] ?: 0L,
                listenUrl = url,
                sizeBytes = sizes[fileName] ?: 0L,
            )
        }
    }
}
