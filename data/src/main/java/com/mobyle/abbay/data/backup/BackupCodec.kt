package com.mobyle.abbay.data.backup

import com.mobyle.abbay.data.sidecar.SidecarBook
import com.mobyle.abbay.data.sidecar.SidecarCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

/**
 * The backup file: every book in the sidecar shape, plus where it is (relative
 * path, so tracks are found again on another device) and its history. Pure Kotlin, tested on the JVM.
 */
object BackupCodec {
    const val FORMAT = "abbay-backup"
    const val VERSION = 1

    private val json = Json { prettyPrint = true }

    data class Entry(val book: SidecarBook, val relativePath: String, val title: String, val history: List<HistoryItem>)

    data class HistoryItem(val type: String, val at: Long, val positionMs: Long, val extraMs: Long?)

    fun write(entries: List<Entry>, exportedAt: Long): String = json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "format" to JsonPrimitive(FORMAT),
                "version" to JsonPrimitive(VERSION),
                "exportedAt" to JsonPrimitive(Instant.ofEpochMilli(exportedAt).toString()),
                "books" to JsonArray(entries.map(::encode)),
            ),
        ),
    )

    /** The entries of a backup file, or null when [text] is not one. */
    fun read(text: String): List<Entry>? {
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return null
        if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) return null
        return (root["books"] as? JsonArray).orEmpty().mapNotNull { element ->
            val bookObject = element as? JsonObject ?: return@mapNotNull null
            val book = SidecarCodec.decode(bookObject) ?: return@mapNotNull null
            Entry(
                book = book,
                relativePath = (bookObject["relativePath"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                title = (bookObject["title"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                history = (bookObject["history"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::decodeHistory) },
            )
        }
    }

    private fun encode(entry: Entry): JsonObject {
        val fields = LinkedHashMap(SidecarCodec.encode(entry.book, previous = null))
        fields["relativePath"] = JsonPrimitive(entry.relativePath)
        fields["title"] = JsonPrimitive(entry.title)
        fields["history"] = JsonArray(
            entry.history.map { item ->
                JsonObject(
                    mapOf(
                        "type" to JsonPrimitive(item.type),
                        "at" to JsonPrimitive(item.at),
                        "positionMs" to JsonPrimitive(item.positionMs),
                        "extraMs" to (item.extraMs?.let(::JsonPrimitive) ?: JsonNull),
                    ),
                )
            },
        )
        return JsonObject(fields)
    }

    private fun decodeHistory(item: JsonObject): HistoryItem? {
        val type = (item["type"] as? JsonPrimitive)?.contentOrNull ?: return null
        val at = item["at"]?.jsonPrimitive?.longOrNull ?: return null
        val position = item["positionMs"]?.jsonPrimitive?.longOrNull ?: return null
        return HistoryItem(type, at, position, (item["extraMs"] as? JsonPrimitive)?.longOrNull)
    }
}
