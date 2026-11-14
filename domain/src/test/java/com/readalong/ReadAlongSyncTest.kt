package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadAlongSyncTest {
    private val line = ReadAlongLine(10_000, 12_000, listOf(ReadAlongWord("hello", 10_000, 12_000)))

    @Test
    fun `a positive offset shows the text ahead and seeks back by as much`() {
        assertEquals(11_500L, ReadAlongSync.textTime(audioMs = 10_000, offsetMs = 1_500))
        assertEquals(8_500L, ReadAlongSync.audioTime(textMs = 10_000, offsetMs = 1_500))
    }

    @Test
    fun `times never go before the start of the book`() {
        assertEquals(0L, ReadAlongSync.textTime(audioMs = 500, offsetMs = -2_000))
        assertEquals(0L, ReadAlongSync.audioTime(textMs = 500, offsetMs = 2_000))
    }

    @Test
    fun `nudging moves by whole steps and stops at the limit`() {
        assertEquals(1_000L, ReadAlongSync.nudged(0, 2))
        assertEquals(-500L, ReadAlongSync.nudged(0, -1))
        assertEquals(ReadAlongSync.LIMIT_MS, ReadAlongSync.nudged(ReadAlongSync.LIMIT_MS, 1))
        assertEquals(-ReadAlongSync.LIMIT_MS, ReadAlongSync.nudged(-ReadAlongSync.LIMIT_MS, -1))
    }

    @Test
    fun `aligning to a line makes it start at the audio's position`() {
        val offset = ReadAlongSync.aligning(line, audioMs = 7_000)

        assertEquals(3_000L, offset)
        assertEquals(line.startMs, ReadAlongSync.textTime(7_000, offset))
        assertEquals(-ReadAlongSync.LIMIT_MS, ReadAlongSync.aligning(line, audioMs = 600_000))
    }
}
