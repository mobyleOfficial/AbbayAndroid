package com.mobyle.abbay.data.librivox

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object LibriVoxJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun searchDocs(body: String): List<ArchiveSearchDoc> {
        val docs = objectOf(body)?.objectAt("response")?.get("docs") as? JsonArray ?: return emptyList()
        return docs.mapNotNull { element ->
            val doc = element as? JsonObject ?: return@mapNotNull null
            ArchiveSearchDoc(
                identifier = doc.lossyString("identifier"),
                title = doc.lossyString("title"),
                creators = doc.stringList("creator"),
                languages = doc.stringList("language"),
                mediatype = doc.lossyString("mediatype"),
            )
        }
    }

    fun archiveItem(identifier: String, body: String): ArchiveItem? {
        val root = objectOf(body) ?: return null
        val metadata = root.objectAt("metadata") ?: return null
        val files = (root["files"] as? JsonArray).orEmpty().mapNotNull { element ->
            val file = element as? JsonObject ?: return@mapNotNull null
            ArchiveFile(
                name = file.lossyString("name"),
                format = file.lossyString("format"),
                size = file.lossyString("size"),
                length = file.lossyString("length"),
                title = file.lossyString("title"),
                track = file.lossyString("track"),
            )
        }
        return ArchiveItem(
            identifier = identifier,
            title = metadata.lossyString("title"),
            creators = metadata.stringList("creator"),
            languages = metadata.stringList("language"),
            description = metadata.stringList("description").firstOrNull(),
            mediatype = metadata.lossyString("mediatype"),
            runtime = metadata.lossyString("runtime"),
            files = files,
        )
    }

    fun feedBooks(body: String): List<LibriVoxFeedBook> {
        val books = objectOf(body)?.get("books") as? JsonArray ?: return emptyList()
        return books.mapNotNull { element ->
            val book = element as? JsonObject ?: return@mapNotNull null
            LibriVoxFeedBook(
                id = book.lossyString("id"),
                urlIArchive = book.lossyString("url_iarchive"),
                description = book.lossyString("description"),
                sections = (book["sections"] as? JsonArray).orEmpty().mapNotNull { sectionElement ->
                    val section = sectionElement as? JsonObject ?: return@mapNotNull null
                    LibriVoxFeedSection(
                        title = section.lossyString("title"),
                        listenUrl = section.lossyString("listen_url"),
                        playtime = section.lossyString("playtime"),
                    )
                },
            )
        }
    }

    private fun objectOf(body: String): JsonObject? = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()

    private fun JsonObject.objectAt(key: String): JsonObject? = get(key) as? JsonObject

    private fun JsonObject.lossyString(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.stringList(key: String): List<String> {
        val values = when (val element: JsonElement? = get(key)) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(element.contentOrNull)
            else -> emptyList()
        }
        return values.map(String::trim).filter(String::isNotEmpty)
    }
}
