package com.subtitles

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTimelineTest {
    private val lines = listOf(SubtitleLine(1_000, "a"), SubtitleLine(2_000, "b"), SubtitleLine(5_000, "c"))

    @Test
    fun findsTheLineThatStartedLast() {
        assertEquals(-1, SubtitleTimeline.indexAt(lines, 999))
        assertEquals(0, SubtitleTimeline.indexAt(lines, 1_000))
        assertEquals(1, SubtitleTimeline.indexAt(lines, 4_999))
        assertEquals(2, SubtitleTimeline.indexAt(lines, 60_000))
        assertEquals(-1, SubtitleTimeline.indexAt(emptyList(), 0))
    }
}
