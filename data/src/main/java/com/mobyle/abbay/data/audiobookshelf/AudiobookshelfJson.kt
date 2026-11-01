package com.mobyle.abbay.data.audiobookshelf

import com.mobyle.abbay.data.librivox.CatalogText
import com.model.AudiobookshelfLibrary
import com.model.CatalogChapter
import com.model.LibriVoxBook
import com.model.LibriVoxSection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Reads Audiobookshelf's REST answers, leniently: fields moved between server versions. */
object AudiobookshelfJson {
    data class Tokens(val accessToken: String, val refreshToken: String?)

    /** A refreshable session when the server returns one (its access token is short-lived), else the long-lived token. */
    fun tokens(body: String): Tokens? {
        val user = objectOf(body)?.objectAt("user") ?: return null
        val refresh = user.string("refreshToken")
        val access = user.string("accessToken")
        if (access != null && refresh != null) return Tokens(access, refresh)
        return (user.string("token") ?: access)?.let { Tokens(it, null) }
    }

    fun isServerStatus(body: String): Boolean {
        val root = objectOf(body) ?: return false
        return root.containsKey("isInit") || root.containsKey("serverVersion") || root.containsKey("authMethods")
    }

    fun isPing(body: String): Boolean = (objectOf(body)?.get("success") as? JsonPrimitive)?.booleanOrNull == true

    fun libraries(body: String): List<AudiobookshelfLibrary> {
        val element = parseJson(body)
        val array = (element as? JsonObject)?.get("libraries") as? JsonArray ?: element as? JsonArray ?: return emptyList()
        return array.mapNotNull { it as? JsonObject }
            .filter { (it.string("mediaType") ?: "book") == "book" }
            .mapNotNull { library ->
                val id = library.string("id") ?: return@mapNotNull null
                AudiobookshelfLibrary(id, library.string("name") ?: id)
            }
    }

    fun pageItems(body: String, coverUrlOf: (String) -> String): List<LibriVoxBook> {
        val results = objectOf(body)?.get("results") as? JsonArray ?: return emptyList()
        return results.mapNotNull { (it as? JsonObject)?.let { item -> lightBook(item, coverUrlOf) } }
    }

    fun searchItems(body: String, coverUrlOf: (String) -> String): List<LibriVoxBook> {
        val matches = objectOf(body)?.get("book") as? JsonArray ?: return emptyList()
        return matches.mapNotNull { match ->
            (match as? JsonObject)?.objectAt("libraryItem")?.let { lightBook(it, coverUrlOf) }
        }.distinctBy { it.id }
    }

    fun book(body: String, serverUrl: String, coverUrlOf: (String) -> String): LibriVoxBook? {
        val item = objectOf(body) ?: return null
        val light = lightBook(item, coverUrlOf) ?: return null
        val media = item.objectAt("media") ?: return null
        val sections = sections(light.id, media, serverUrl)
        if (sections.isEmpty()) return null
        val chapters = (media["chapters"] as? JsonArray).orEmpty().mapNotNull { element ->
            val chapter = element as? JsonObject ?: return@mapNotNull null
            val start = chapter.millis("start") ?: return@mapNotNull null
            val end = chapter.millis("end") ?: return@mapNotNull null
            CatalogChapter(chapter.string("title").orEmpty(), start, end).takeIf { end > start }
        }.sortedBy { it.startMs }
        val sum = sections.sumOf { it.durationMs }
        return light.copy(
            description = CatalogText.stripHtml(media.objectAt("metadata")?.string("description").orEmpty()),
            durationMs = if (sum > 0) sum else light.durationMs,
            sections = sections,
            chapters = chapters,
        )
    }

    private fun sections(itemId: String, media: JsonObject, serverUrl: String): List<LibriVoxSection> {
        val files = (media["audioFiles"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            .filter { it.boolean("exclude") != true && it.boolean("invalid") != true }
            .sortedBy { it.long("index") ?: Long.MAX_VALUE }
        return files.mapNotNull { file ->
            val ino = file.string("ino") ?: return@mapNotNull null
            val fileMetadata = file.objectAt("metadata")
            val fileName = fileMetadata?.string("filename") ?: ino
            LibriVoxSection(
                title = file.objectAt("metaTags")?.string("tagTitle") ?: fileName.substringBeforeLast('.'),
                durationMs = file.millis("duration") ?: 0L,
                listenUrl = AudiobookshelfUrls.fileUrl(serverUrl, itemId, ino),
                sizeBytes = fileMetadata?.long("size") ?: 0L,
                fileName = fileName,
            )
        }
    }

    internal fun lightBook(item: JsonObject, coverUrlOf: (String) -> String): LibriVoxBook? {
        val media = item.objectAt("media")?.takeIf { isPlayableBook(item, it) } ?: return null
        val metadata = media.objectAt("metadata") ?: return null
        val id = item.string("id") ?: return null
        val title = metadata.string("title") ?: return null
        return LibriVoxBook(
            id = id,
            title = title,
            authors = metadata.string("authorName") ?: names(metadata["authors"]),
            creator = null,
            coverUrl = coverUrlOf(id),
            language = metadata.string("language").orEmpty(),
            durationMs = media.millis("duration") ?: 0L,
            series = AudiobookshelfSeriesJson.seriesOf(item, metadata),
        )
    }

    /** A book (not a podcast) that is on the server's disk and has audio, not only an ebook. */
    private fun isPlayableBook(item: JsonObject, media: JsonObject): Boolean {
        val audioFiles = media.long("numAudioFiles") ?: (media["audioFiles"] as? JsonArray)?.size?.toLong()
        return (item.string("mediaType") ?: "book") == "book" &&
            item.boolean("isMissing") != true &&
            item.boolean("isInvalid") != true &&
            audioFiles != 0L
    }
}
