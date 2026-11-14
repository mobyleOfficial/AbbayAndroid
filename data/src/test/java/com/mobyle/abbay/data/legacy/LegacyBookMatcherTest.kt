package com.mobyle.abbay.data.legacy

import com.mobyle.abbay.data.legacy.LegacyBookMatcher.Candidate
import com.mobyle.abbay.data.legacy.LegacyBookMatcher.LegacyBook
import com.mobyle.abbay.data.legacy.LegacyBookMatcher.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyBookMatcherTest {
    private val candidates = listOf(
        Candidate(1, "Stories of the Sea", listOf("01.mp3", "02.mp3", "03.mp3")),
        Candidate(2, "Other Book", listOf("01.mp3", "02.mp3")),
        Candidate(3, "The Long Road Home", listOf("long_road_home.m4a")),
    )

    @Test
    fun `a single-file legacy book matches the book containing its file`() {
        assertEquals(Match(3, 0), LegacyBookMatcher.match(LegacyBook("Whatever", listOf("long_road_home.m4a"), 0), candidates))
    }

    @Test
    fun `a multi-file legacy book keeps its current track`() {
        val legacy = LegacyBook("Stories of the Sea", listOf("01.mp3", "02.mp3", "03.mp3"), currentTrackIndex = 2)

        assertEquals(Match(1, 2), LegacyBookMatcher.match(legacy, candidates))
    }

    @Test
    fun `shared file names are resolved by overlap, then by title`() {
        assertEquals(1L, LegacyBookMatcher.match(LegacyBook("x", listOf("01.mp3", "03.mp3"), 0), candidates)?.bookId)
        assertEquals(2L, LegacyBookMatcher.match(LegacyBook("Other Book", listOf("01.mp3", "02.mp3"), 1), candidates)?.bookId)
    }

    @Test
    fun `track index follows the new natural order`() {
        val reordered = listOf(Candidate(9, "Book", listOf("Chapter 1.mp3", "Chapter 2.mp3", "Chapter 10.mp3")))
        val legacy = LegacyBook("Book", listOf("Chapter 1.mp3", "Chapter 10.mp3", "Chapter 2.mp3"), currentTrackIndex = 1)

        assertEquals(Match(9, 2), LegacyBookMatcher.match(legacy, reordered))
    }

    @Test
    fun `no match when the file is not in the library`() {
        assertNull(LegacyBookMatcher.match(LegacyBook("Gone", listOf("missing.mp3"), 0), candidates))
        assertNull(LegacyBookMatcher.match(LegacyBook("Empty", emptyList(), 0), candidates))
    }

    @Test
    fun `version 1 track list encoding is decoded`() {
        val encoded = """["{\"id\":\"46\",\"fileName\":\"ch1.m4a\",\"progress\":0}","{\"id\":\"47\",\"fileName\":\"ch2.m4a\"}"]"""

        assertEquals(listOf("ch1.m4a", "ch2.m4a"), LegacyBookMatcher.decodeTrackFileNames(encoded))
        assertEquals(emptyList<String>(), LegacyBookMatcher.decodeTrackFileNames("not json"))
    }
}
