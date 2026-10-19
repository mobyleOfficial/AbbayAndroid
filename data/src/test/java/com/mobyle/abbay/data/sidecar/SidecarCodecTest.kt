package com.mobyle.abbay.data.sidecar

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SidecarCodecTest {
    @Test
    fun readsTheExampleOfTheSpec() {
        val book = SidecarCodec.read(SPEC_EXAMPLE)!!.book

        assertEquals("6f1c2e7a-0d3b-4a39-9a77-3d8f8a1f2b10", book.id)
        assertEquals(Instant.parse("2026-10-16T21:04:11Z").toEpochMilli(), book.updatedAt)
        assertEquals(SidecarBook.Position("Chapter 07.mp3", 734_120L, 21_734_120L), book.position)
        assertEquals("IN_PROGRESS", book.state)
        assertEquals(1.4f, book.audio.speed)
        assertEquals("MODERATE", book.audio.silenceTrim)
        assertEquals("[0,0,-2,0,0,0,3,3,0,0]", book.audio.equalizerJson)
        assertEquals(
            listOf(SidecarBook.Bookmark(1_200_345L, "", "Great quote", Instant.parse("2026-10-10T08:00:00Z").toEpochMilli())),
            book.bookmarks,
        )
        assertEquals(listOf(SidecarBook.Character("Mira", "Cartographer", 300_000L)), book.characters)
        assertEquals(SidecarBook.Overrides(), book.overrides)
    }

    @Test
    fun roundTripKeepsEveryKnownField() {
        val book = SidecarBook(
            id = "id-1",
            updatedAt = 1_700_000_000_000L,
            position = SidecarBook.Position("02.mp3", 5_000L, 65_000L),
            state = "FINISHED",
            lastPlayedAt = 1_700_000_000_000L,
            lastPausedAt = 1_699_999_000_000L,
            audio = SidecarBook.Audio(speed = 1.25f, mono = true, balance = -0.5f, equalizerJson = "[1,2]"),
            repeatMode = "CHAPTER",
            loopAMs = 1L,
            loopBMs = 2L,
            skipIntroSeconds = 10,
            skipOutroSeconds = 20,
            sortByTrackTag = true,
            bookmarks = listOf(SidecarBook.Bookmark(10L, "Opening", "note", 1_600_000_000_000L)),
            characters = listOf(SidecarBook.Character("Mira", "Cartographer", 300_000L), SidecarBook.Character("Tom", "", null)),
            overrides = SidecarBook.Overrides(title = "Title", narrator = "Narrator"),
        )

        assertEquals(book, SidecarCodec.read(SidecarCodec.write(book, previous = null))!!.book)
    }

    @Test
    fun unknownFieldsSurviveARewrite() {
        val read = SidecarCodec.read(
            SPEC_EXAMPLE.replace("\"state\"", "\"futureTopLevel\": {\"a\": 1}, \"state\"")
                .replace("\"note\": \"Great quote\"", "\"note\": \"Great quote\", \"color\": \"red\"")
                .replace("\"mono\": false", "\"mono\": false, \"futureAudio\": 7")
                .replace("\"name\": \"Mira\",", "\"name\": \"Mira\", \"portrait\": \"m.png\","),
        )!!
        val rewritten = Json.parseToJsonElement(SidecarCodec.write(read.book.copy(state = "FINISHED"), read.raw)).jsonObject

        assertEquals("{\"a\":1}", rewritten["futureTopLevel"].toString())
        assertEquals("7", (rewritten["audio"] as JsonObject)["futureAudio"].toString())
        assertEquals(
            "red",
            (rewritten["bookmarks"]!! as kotlinx.serialization.json.JsonArray)[0].jsonObject["color"]!!.jsonPrimitive.content
        )
        assertEquals("FINISHED", rewritten["state"]!!.jsonPrimitive.content)
        assertEquals(
            "[{\"name\":\"Mira\",\"portrait\":\"m.png\",\"description\":\"Cartographer\",\"firstPositionMs\":300000}]",
            rewritten["characters"].toString()
        )
    }

    @Test
    fun otherFilesAreNotSidecars() {
        assertNull(SidecarCodec.read("not json"))
        assertNull(SidecarCodec.read("{\"format\": \"something-else\", \"id\": \"x\"}"))
        assertNull(SidecarCodec.read("{\"format\": \"abbay-book\"}"))
    }

    private companion object {
        val SPEC_EXAMPLE = """
            {
              "format": "abbay-book",
              "version": 1,
              "id": "6f1c2e7a-0d3b-4a39-9a77-3d8f8a1f2b10",
              "updatedAt": "2026-10-16T21:04:11Z",
              "position": { "trackFileName": "Chapter 07.mp3", "positionInTrackMs": 734120, "bookPositionMs": 21734120 },
              "state": "IN_PROGRESS",
              "lastPlayedAt": "2026-10-16T21:04:11Z",
              "audio": { "speed": 1.4, "pitchSemitones": 0, "silenceTrim": "MODERATE", "boost": "OFF",
                         "mono": false, "balance": 0.0, "noiseReduction": "OFF",
                         "equalizer": [0,0,-2,0,0,0,3,3,0,0] },
              "repeat": { "mode": "OFF" },
              "skip": { "introSeconds": 0, "outroSeconds": 0 },
              "bookmarks": [ { "bookPositionMs": 1200345, "note": "Great quote", "createdAt": "2026-10-10T08:00:00Z" } ],
              "characters": [ { "name": "Mira", "description": "Cartographer", "firstPositionMs": 300000 } ],
              "overrides": { "title": null, "author": null, "narrator": null, "description": null }
            }
        """.trimIndent()
    }
}
