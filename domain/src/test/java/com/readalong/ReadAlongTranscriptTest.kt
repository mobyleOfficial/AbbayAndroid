package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongTranscriptTest {
    private fun word(text: String, startMs: Long, endMs: Long) = ReadAlongWord(text, startMs, endMs)

    private fun line(startMs: Long, endMs: Long, vararg words: ReadAlongWord) = ReadAlongLine(startMs, endMs, words.toList())

    private fun transcript(vararg lines: ReadAlongLine) = ReadAlongTranscript(ReadAlongLanguage.ENGLISH).apply {
        add(lines.toList(), ReadAlongSpan(0, 10_000))
    }

    @Test
    fun `a line's text is its words joined`() {
        val line = line(0, 900, word("the", 0, 300), word("rabbit", 300, 900))

        assertEquals("the rabbit", line.text)
    }

    @Test
    fun `lines are kept in order whatever order the runs arrive in`() {
        val transcript = ReadAlongTranscript(ReadAlongLanguage.ENGLISH)

        transcript.add(listOf(line(5_000, 6_000), line(9_000, 10_000)), ReadAlongSpan(5_000, 10_000))
        transcript.add(listOf(line(1_000, 2_000)), ReadAlongSpan(0, 3_000))

        assertEquals(listOf(1_000L, 5_000L, 9_000L), transcript.lines.map { it.startMs })
    }

    @Test
    fun `a run re-transcribing the same audio does not duplicate a line`() {
        val transcript = ReadAlongTranscript(ReadAlongLanguage.ENGLISH)
        transcript.add(listOf(line(1_000, 2_000)), ReadAlongSpan(0, 3_000))

        transcript.add(listOf(line(1_020, 2_100)), ReadAlongSpan(1_000, 4_000))

        assertEquals(1, transcript.lines.size)
        assertEquals(1_020L, transcript.lines.single().startMs)
    }

    @Test
    fun `a line further than the tolerance from a stored one is kept beside it`() {
        val transcript = ReadAlongTranscript(ReadAlongLanguage.ENGLISH)
        transcript.add(listOf(line(1_000, 2_000)), ReadAlongSpan(0, 3_000))

        transcript.add(listOf(line(1_200, 2_100)), ReadAlongSpan(1_000, 4_000))

        assertEquals(listOf(1_000L, 1_200L), transcript.lines.map { it.startMs })
    }

    @Test
    fun `covered spans that touch are merged`() {
        val transcript = ReadAlongTranscript(ReadAlongLanguage.ENGLISH)

        transcript.add(emptyList(), ReadAlongSpan(0, 10_000))
        transcript.add(emptyList(), ReadAlongSpan(10_000, 20_000))
        transcript.add(emptyList(), ReadAlongSpan(30_000, 35_000))

        assertEquals(listOf(ReadAlongSpan(0, 20_000), ReadAlongSpan(30_000, 35_000)), transcript.covered)
    }

    @Test
    fun `an empty span covers nothing`() {
        val transcript = ReadAlongTranscript(ReadAlongLanguage.ENGLISH)

        transcript.add(emptyList(), ReadAlongSpan(5_000, 5_000))

        assertTrue(transcript.covered.isEmpty())
    }

    @Test
    fun `a half-open span covers its start and not its end`() {
        val transcript = transcript()

        assertTrue(transcript.isCovered(0))
        assertTrue(transcript.isCovered(9_999))
        assertFalse(transcript.isCovered(10_000))
    }

    @Test
    fun `the line being read is the last one that started, with the word being spoken`() {
        val transcript = transcript(
            line(0, 1_000, word("the", 0, 400), word("rabbit", 400, 1_000)),
            line(1_000, 2_000, word("hole", 1_000, 2_000)),
        )

        val position = transcript.positionAt(600)!!

        assertEquals(0, position.line)
        assertEquals(1, position.word)
    }

    @Test
    fun `between words nothing is highlighted`() {
        val transcript = transcript(line(0, 1_000, word("the", 0, 400), word("rabbit", 700, 1_000)))

        assertNull(transcript.positionAt(500)!!.word)
    }

    @Test
    fun `before the first line there is no position`() {
        val transcript = transcript(line(1_000, 2_000))

        assertNull(transcript.positionAt(0))
    }

    @Test
    fun `a line stays current through a pause shorter than the hold`() {
        val transcript = transcript(line(0, 1_000))

        assertEquals(ReadAlongDisplay.Line(ReadAlongPosition(0, null)), transcript.displayAt(5_999))
    }

    @Test
    fun `a longer pause shows the next line coming, dimmed`() {
        val transcript = transcript(line(0, 1_000), line(12_000, 13_000))

        assertEquals(ReadAlongDisplay.Upcoming(1), transcript.displayAt(6_000))
    }

    @Test
    fun `a stretch with no speech for longer than the music threshold shows the music mark`() {
        val transcript = transcript(line(0, 1_000), line(60_000, 61_000))

        assertEquals(ReadAlongDisplay.Music, transcript.displayAt(6_000))
    }

    @Test
    fun `after the last line with nothing following there is music`() {
        val transcript = transcript(line(0, 1_000))

        assertEquals(ReadAlongDisplay.Music, transcript.displayAt(6_000))
    }

    @Test
    fun `a line read from storage is sorted by its start`() {
        val loaded = ReadAlongTranscript.of(
            language = ReadAlongLanguage.PORTUGUESE,
            lines = listOf(line(5_000, 6_000), line(1_000, 2_000)),
            covered = listOf(ReadAlongSpan(0, 6_000)),
        )

        assertEquals(listOf(1_000L, 5_000L), loaded.lines.map { it.startMs })
        assertEquals(ReadAlongLanguage.PORTUGUESE, loaded.language)
    }
}
