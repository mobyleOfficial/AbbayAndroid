package com.mobyle.abbay.data.chapters

import com.mobyle.abbay.data.chapters.BookChapterBuilder.BuiltChapter
import com.mobyle.abbay.data.chapters.BookChapterBuilder.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class BookChapterBuilderTest {
    private val tracks = listOf(Track("01.mp3", 0, 60_000, "Opening"), Track("02.mp3", 60_000, 40_000, null))

    @Test
    fun `cue sheet chapters are offset by each file's start`() {
        val cue = mapOf("01.MP3" to listOf(ParsedChapter("A", 0), ParsedChapter("B", 30_000)), "02.mp3" to listOf(ParsedChapter("C", 0)))

        val chapters = BookChapterBuilder.build(tracks, cue) { error("embedded must not be read when a cue sheet applies") }

        assertEquals(
            listOf(
                BuiltChapter("A", 0, 30_000, "CUE"),
                BuiltChapter("B", 30_000, 60_000, "CUE"),
                BuiltChapter("C", 60_000, 100_000, "CUE"),
            ),
            chapters,
        )
    }

    @Test
    fun `embedded chapters per file and one chapter for files without any`() {
        val chapters = BookChapterBuilder.build(tracks, cueChapters = null) { track ->
            if (track.fileName == "02.mp3") listOf(ParsedChapter("Part 2", 500), ParsedChapter("Part 3", 20_000)) else emptyList()
        }

        assertEquals(
            listOf(
                BuiltChapter("Opening", 0, 60_000, "FILE"),
                BuiltChapter("Part 2", 60_000, 80_000, "EMBEDDED"),
                BuiltChapter("Part 3", 80_000, 100_000, "EMBEDDED"),
            ),
            chapters,
        )
    }

    @Test
    fun `fallback titles use the title tag, otherwise the file name, and chapters past the end are ignored`() {
        val chapters = BookChapterBuilder.build(tracks, cueChapters = mapOf("other.mp3" to listOf(ParsedChapter("X", 0)))) { track ->
            if (track.fileName == "01.mp3") listOf(ParsedChapter("Beyond", 90_000)) else emptyList()
        }

        assertEquals(listOf("Opening", "02"), chapters.map { it.title })
        assertEquals(listOf("FILE", "FILE"), chapters.map { it.source })
    }
}
