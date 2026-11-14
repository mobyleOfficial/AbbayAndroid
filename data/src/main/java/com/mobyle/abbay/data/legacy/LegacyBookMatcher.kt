package com.mobyle.abbay.data.legacy

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LegacyBookMatcher {
    data class Candidate(val bookId: Long, val title: String, val trackFileNames: List<String>)

    data class LegacyBook(val name: String, val trackFileNames: List<String>, val currentTrackIndex: Int)

    data class Match(val bookId: Long, val trackIndex: Int)

    fun match(legacyBook: LegacyBook, candidates: List<Candidate>): Match? {
        val currentFileName = legacyBook.trackFileNames.getOrNull(legacyBook.currentTrackIndex)
            ?: legacyBook.trackFileNames.firstOrNull()
            ?: return null
        val legacyFiles = legacyBook.trackFileNames.toSet()

        val best = candidates
            .filter { currentFileName in it.trackFileNames }
            .maxWithOrNull(
                compareBy<Candidate> { candidate -> candidate.trackFileNames.count { it in legacyFiles } }
                    .thenBy { candidate -> if (candidate.title == legacyBook.name) 1 else 0 }
                    .thenBy { candidate -> -candidate.trackFileNames.size },
            ) ?: return null

        return Match(bookId = best.bookId, trackIndex = best.trackFileNames.indexOf(currentFileName))
    }

    @Suppress("SwallowedException")
    fun decodeTrackFileNames(encodedTrackList: String): List<String> = try {
        Json.decodeFromString<List<String>>(encodedTrackList).mapNotNull { encodedTrack ->
            Json.parseToJsonElement(encodedTrack).jsonObject["fileName"]?.jsonPrimitive?.content
        }
    } catch (malformed: IllegalArgumentException) {
        emptyList()
    }
}
