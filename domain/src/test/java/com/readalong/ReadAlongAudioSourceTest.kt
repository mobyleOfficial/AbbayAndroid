package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongAudioSourceTest {
    private fun segment(fileUri: String?, startMs: Long, durationMs: Long) =
        ReadAlongAudioSource.Segment(fileUri = fileUri, bookStartMs = startMs, durationMs = durationMs)

    private fun source(vararg segments: ReadAlongAudioSource.Segment) = ReadAlongAudioSource(segments.toList())

    @Test
    fun `a run reads the file holding its start, then every file after it`() {
        val source = source(
            segment("file:///one.mp3", 0, 10_000),
            segment("file:///two.mp3", 10_000, 10_000),
        )

        val readings = source.readingsFrom(12_000)

        assertEquals(1, readings.size)
        assertEquals("file:///two.mp3", readings.single().fileUri)
        assertEquals(2_000L, readings.single().offsetMs)
        assertEquals(12_000L, readings.single().bookStartMs)
        assertEquals(20_000L, readings.single().bookEndMs)
    }

    @Test
    fun `a start before the first file reads it from its beginning`() {
        val source = source(segment("file:///one.mp3", 5_000, 10_000))

        val readings = source.readingsFrom(0)

        assertEquals(0L, readings.single().offsetMs)
        assertEquals(5_000L, readings.single().bookStartMs)
    }

    @Test
    fun `a file that is not on the device ends the run there`() {
        val source = source(
            segment("file:///one.mp3", 0, 10_000),
            segment(null, 10_000, 10_000),
            segment("file:///three.mp3", 20_000, 10_000),
        )

        assertEquals(listOf("file:///one.mp3"), source.readingsFrom(0).map { it.fileUri })
    }

    @Test
    fun `a start past a file's end skips it`() {
        val source = source(segment("file:///one.mp3", 0, 10_000), segment("file:///two.mp3", 10_000, 10_000))

        val readings = source.readingsFrom(10_000)

        assertEquals(listOf("file:///two.mp3"), readings.map { it.fileUri })
    }

    @Test
    fun `a time maps to the file that holds it`() {
        val source = source(segment("file:///one.mp3", 0, 10_000), segment("file:///two.mp3", 10_000, 10_000))

        assertEquals("file:///two.mp3", source.segmentAt(15_000)?.fileUri)
        assertEquals("file:///two.mp3", source.segmentAt(99_000)?.fileUri)
        assertTrue(source.isOnDevice(15_000))
    }

    @Test
    fun `a stream that was not downloaded is not on the device`() {
        val source = source(segment(null, 0, 10_000))

        assertFalse(source.isOnDevice(5_000))
        assertTrue(source.readingsFrom(5_000).isEmpty())
    }

    @Test
    fun `an empty source reads nothing`() {
        assertTrue(ReadAlongAudioSource(emptyList()).readingsFrom(0).isEmpty())
    }
}
