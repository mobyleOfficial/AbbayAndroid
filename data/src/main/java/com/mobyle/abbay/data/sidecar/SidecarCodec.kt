package com.mobyle.abbay.data.sidecar

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.format.DateTimeParseException

@Suppress("TooManyFunctions")
object SidecarCodec {
    const val FORMAT = "abbay-book"
    const val VERSION = 1

    private val json = Json { prettyPrint = true }

    fun read(text: String): Read? {
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return null
        if (root.string("format") != FORMAT) return null
        return decode(root)?.let { Read(it, root) }
    }

    fun decode(root: JsonObject): SidecarBook? {
        val id = root.string("id")?.takeIf(String::isNotBlank) ?: return null
        return SidecarBook(
            id = id,
            updatedAt = root.instant("updatedAt") ?: 0L,
            position = root.objectAt("position")?.let(::decodePosition),
            state = root.string("state") ?: "NEW",
            lastPlayedAt = root.instant("lastPlayedAt"),
            lastPausedAt = root.instant("lastPausedAt"),
            audio = decodeAudio(root.objectAt("audio")),
            repeatMode = root.objectAt("repeat")?.string("mode") ?: "OFF",
            loopAMs = root.objectAt("repeat")?.long("loopAMs"),
            loopBMs = root.objectAt("repeat")?.long("loopBMs"),
            skipIntroSeconds = root.objectAt("skip")?.int("introSeconds") ?: 0,
            skipOutroSeconds = root.objectAt("skip")?.int("outroSeconds") ?: 0,
            sortByTrackTag = root.boolean("sortByTrackTag") ?: false,
            bookmarks = (root["bookmarks"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::decodeBookmark) },
            characters = (root["characters"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::decodeCharacter) },
            overrides = root.objectAt("overrides").let { overrides ->
                SidecarBook.Overrides(
                    title = overrides?.string("title"),
                    author = overrides?.string("author"),
                    narrator = overrides?.string("narrator"),
                    description = overrides?.string("description"),
                )
            },
        )
    }

    fun write(book: SidecarBook, previous: JsonObject?): String = json.encodeToString(JsonObject.serializer(), encode(book, previous))

    fun encode(book: SidecarBook, previous: JsonObject?): JsonObject {
        val fields = LinkedHashMap(previous.orEmpty())
        fields["format"] = JsonPrimitive(FORMAT)
        fields["version"] = JsonPrimitive(VERSION)
        fields["id"] = JsonPrimitive(book.id)
        fields["updatedAt"] = instant(book.updatedAt)
        fields["position"] = book.position?.let { encodePosition(it, previous?.objectAt("position")) } ?: JsonNull
        fields["state"] = JsonPrimitive(book.state)
        fields["lastPlayedAt"] = book.lastPlayedAt?.let(::instant) ?: JsonNull
        fields["lastPausedAt"] = book.lastPausedAt?.let(::instant) ?: JsonNull
        fields["audio"] = encodeAudio(book.audio, previous?.objectAt("audio"))
        fields["repeat"] = merge(
            previous?.objectAt("repeat"),
            "mode" to JsonPrimitive(book.repeatMode),
            "loopAMs" to (book.loopAMs?.let(::JsonPrimitive) ?: JsonNull),
            "loopBMs" to (book.loopBMs?.let(::JsonPrimitive) ?: JsonNull),
        )
        fields["skip"] = merge(
            previous?.objectAt("skip"),
            "introSeconds" to JsonPrimitive(book.skipIntroSeconds),
            "outroSeconds" to JsonPrimitive(book.skipOutroSeconds),
        )
        fields["sortByTrackTag"] = JsonPrimitive(book.sortByTrackTag)
        fields["bookmarks"] = encodeBookmarks(book.bookmarks, previous?.get("bookmarks") as? JsonArray)
        fields["characters"] = encodeCharacters(book.characters, previous?.get("characters") as? JsonArray)
        fields["overrides"] = merge(
            previous?.objectAt("overrides"),
            "title" to text(book.overrides.title),
            "author" to text(book.overrides.author),
            "narrator" to text(book.overrides.narrator),
            "description" to text(book.overrides.description),
        )
        return JsonObject(fields)
    }

    private fun decodePosition(position: JsonObject): SidecarBook.Position? {
        val fileName = position.string("trackFileName") ?: return null
        return SidecarBook.Position(fileName, position.long("positionInTrackMs") ?: 0L, position.long("bookPositionMs") ?: 0L)
    }

    private fun encodePosition(position: SidecarBook.Position, previous: JsonObject?) = merge(
        previous,
        "trackFileName" to JsonPrimitive(position.trackFileName),
        "positionInTrackMs" to JsonPrimitive(position.positionInTrackMs),
        "bookPositionMs" to JsonPrimitive(position.bookPositionMs),
    )

    private fun decodeAudio(audio: JsonObject?): SidecarBook.Audio {
        val defaults = SidecarBook.Audio()
        return SidecarBook.Audio(
            speed = audio?.float("speed") ?: defaults.speed,
            pitchSemitones = audio?.float("pitchSemitones") ?: defaults.pitchSemitones,
            silenceTrim = audio?.string("silenceTrim") ?: defaults.silenceTrim,
            boost = audio?.string("boost") ?: defaults.boost,
            mono = audio?.boolean("mono") ?: defaults.mono,
            balance = audio?.float("balance") ?: defaults.balance,
            noiseReduction = audio?.string("noiseReduction") ?: defaults.noiseReduction,
            equalizerJson = (audio?.get("equalizer") as? JsonArray)?.toString(),
        )
    }

    private fun encodeAudio(audio: SidecarBook.Audio, previous: JsonObject?) = merge(
        previous,
        "speed" to JsonPrimitive(audio.speed),
        "pitchSemitones" to JsonPrimitive(audio.pitchSemitones),
        "silenceTrim" to JsonPrimitive(audio.silenceTrim),
        "boost" to JsonPrimitive(audio.boost),
        "mono" to JsonPrimitive(audio.mono),
        "balance" to JsonPrimitive(audio.balance),
        "noiseReduction" to JsonPrimitive(audio.noiseReduction),
        "equalizer" to (audio.equalizerJson?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } ?: JsonNull),
    )

    private fun decodeBookmark(bookmark: JsonObject): SidecarBook.Bookmark? {
        val position = bookmark.long("bookPositionMs") ?: return null
        return SidecarBook.Bookmark(
            position,
            bookmark.string("chapterTitle").orEmpty(),
            bookmark.string("note").orEmpty(),
            bookmark.instant("createdAt") ?: 0L
        )
    }

    private fun encodeBookmarks(bookmarks: List<SidecarBook.Bookmark>, previous: JsonArray?): JsonArray {
        val previousByKey = previous.orEmpty().mapNotNull { it as? JsonObject }
            .associateBy { it.long("bookPositionMs") to it.instant("createdAt") }
        return JsonArray(
            bookmarks.map { bookmark ->
                merge(
                    previousByKey[bookmark.bookPositionMs to bookmark.createdAt],
                    "bookPositionMs" to JsonPrimitive(bookmark.bookPositionMs),
                    "chapterTitle" to JsonPrimitive(bookmark.chapterTitle),
                    "note" to JsonPrimitive(bookmark.note),
                    "createdAt" to instant(bookmark.createdAt),
                )
            },
        )
    }

    private fun decodeCharacter(character: JsonObject): SidecarBook.Character? {
        val name = character.string("name")?.takeIf(String::isNotBlank) ?: return null
        return SidecarBook.Character(name, character.string("description").orEmpty(), character.long("firstPositionMs"))
    }

    private fun encodeCharacters(characters: List<SidecarBook.Character>, previous: JsonArray?): JsonArray {
        val previousByName = previous.orEmpty().mapNotNull { it as? JsonObject }.associateBy { it.string("name") }
        return JsonArray(
            characters.map { character ->
                merge(
                    previousByName[character.name],
                    "name" to JsonPrimitive(character.name),
                    "description" to JsonPrimitive(character.description),
                    "firstPositionMs" to (character.firstPositionMs?.let(::JsonPrimitive) ?: JsonNull),
                )
            },
        )
    }

    private fun merge(previous: JsonObject?, vararg fields: Pair<String, JsonElement>): JsonObject =
        JsonObject(LinkedHashMap(previous.orEmpty()).apply { fields.forEach { (key, value) -> put(key, value) } })

    private fun instant(epochMs: Long) = JsonPrimitive(Instant.ofEpochMilli(epochMs).toString())

    private fun text(value: String?): JsonElement = value?.let(::JsonPrimitive) ?: JsonNull

    private fun JsonObject.objectAt(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

    private fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull

    private fun JsonObject.long(key: String): Long? = primitive(key)?.longOrNull

    private fun JsonObject.int(key: String): Int? = primitive(key)?.intOrNull

    private fun JsonObject.float(key: String): Float? = primitive(key)?.floatOrNull

    private fun JsonObject.boolean(key: String): Boolean? = primitive(key)?.booleanOrNull

    private fun JsonObject.instant(key: String): Long? {
        val value = string(key) ?: return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (invalid: DateTimeParseException) {
            value.toLongOrNull()
        }
    }

    data class Read(val book: SidecarBook, val raw: JsonObject)
}
