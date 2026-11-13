package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongTranscriptTest {
    private fun line(startMs: Long, vararg words: String, wordMs: Long = 300): ReadAlongLine {
        val parsed = words.mapIndexed { index, text -> ReadAlongWord(text, startMs + index * wordMs, startMs + (index + 1) * wordMs - 50) }
        return ReadAlongLine(parsed.first().startMs, parsed.last().endMs, parsed)
    }

    @Test
    fun `runs merge in any order and stay sorted`() {
        val transcript = ReadAlongTranscript("en-US")
            .adding(listOf(line(10_000, "later")), ReadAlongSpan(10_000, 20_000))
            .adding(listOf(line(1_000, "earlier")), ReadAlongSpan(0, 10_000))

        assertEquals(listOf("earlier", "later"), transcript.lines.map { it.text })
        assertEquals(listOf(ReadAlongSpan(0, 20_000)), transcript.covered)
    }

    @Test
    fun `re-transcribing the same audio replaces lines instead of duplicating them`() {
        val transcript = ReadAlongTranscript("en-US")
            .adding(listOf(line(1_000, "old", "text"), line(5_000, "kept")), ReadAlongSpan(0, 8_000))
            .adding(listOf(line(1_030, "new", "text")), ReadAlongSpan(0, 4_000))

        assertEquals(listOf("new text", "kept"), transcript.lines.map { it.text })
    }

    @Test
    fun `separate spans stay apart and touching ones merge`() {
        val transcript = ReadAlongTranscript("en-US")
            .adding(emptyList(), ReadAlongSpan(0, 1_000))
            .adding(emptyList(), ReadAlongSpan(5_000, 6_000))
            .adding(emptyList(), ReadAlongSpan(1_000, 2_000))

        assertEquals(listOf(ReadAlongSpan(0, 2_000), ReadAlongSpan(5_000, 6_000)), transcript.covered)
        assertTrue(transcript.isCovered(1_999))
        assertFalse(transcript.isCovered(2_000))
    }

    @Test
    fun `an empty span changes nothing`() {
        val transcript = ReadAlongTranscript("en-US").adding(emptyList(), ReadAlongSpan(3_000, 3_000))

        assertTrue(transcript.covered.isEmpty())
    }

    @Test
    fun `the spoken word is found inside the current line, none between words`() {
        val transcript = ReadAlongTranscript("en-US").adding(listOf(line(1_000, "one", "two", "three")), ReadAlongSpan(0, 5_000))

        assertEquals(ReadAlongPosition(0, 1), transcript.position(1_400))
        assertEquals(ReadAlongPosition(0, null), transcript.position(1_570))
        assertNull(transcript.position(999))
    }

    @Test
    fun `a short pause holds the line, a longer one shows the next line coming`() {
        val transcript = ReadAlongTranscript("en-US")
            .adding(listOf(line(1_000, "first"), line(12_000, "second")), ReadAlongSpan(0, 20_000))

        assertEquals(ReadAlongDisplay.Line(ReadAlongPosition(0, null)), transcript.display(3_000))
        assertEquals(ReadAlongDisplay.Upcoming(1), transcript.display(8_000))
        assertEquals(ReadAlongDisplay.Upcoming(0), transcript.display(0))
    }

    @Test
    fun `a long stretch without speech is music`() {
        val transcript = ReadAlongTranscript("en-US").adding(listOf(line(60_000, "late")), ReadAlongSpan(0, 70_000))

        assertEquals(ReadAlongDisplay.Music, transcript.display(10_000))
        assertEquals(ReadAlongDisplay.Upcoming(0), transcript.display(40_000))
        assertEquals(ReadAlongDisplay.Music, transcript.display(69_000))
    }

    @Test
    fun `shifting and clipping keep words in book time`() {
        val shifted = line(0, "a", "b", "c").shifted(10_000)
        val clipped = shifted.clipped(10_400)

        assertEquals(10_000L, shifted.startMs)
        assertEquals(listOf("a", "b"), clipped?.words?.map { it.text })
        assertEquals(10_400L, clipped?.endMs)
        assertNull(shifted.clipped(10_000))
    }
}
