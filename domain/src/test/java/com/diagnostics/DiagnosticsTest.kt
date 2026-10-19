package com.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
    @Test
    fun ringBufferKeepsTheLastLines() {
        val buffer = LogRingBuffer(capacity = 3)
        (1..5).forEach { buffer.add(LogLine(it.toLong(), "line $it")) }
        assertEquals(listOf("line 3", "line 4", "line 5"), buffer.snapshot().map { it.message })
    }

    @Test
    fun fileNamesAreLeftOutUnlessAskedFor() {
        val report = DiagnosticsReport(
            androidVersion = "11",
            device = "motorola moto g(9) plus",
            appVersion = "1.0",
            decoders = mapOf("audio/mpeg" to listOf("c2.android.mp3.decoder"), "audio/x-ms-wma" to emptyList()),
            opusEncoder = null,
            outputSampleRate = 48_000,
            outputFramesPerBuffer = 192,
            log = listOf(LogLine(0, "Track skipped:", "Secret Title 01.mp3"), LogLine(1, "Playing")),
        )
        val hidden = DiagnosticsText.of(report, includeFileNames = false) { "t$it" }
        assertFalse("Secret Title" in hidden)
        assertTrue("t0 Track skipped: <file>" in hidden)
        assertTrue("audio/x-ms-wma: none" in hidden)
        assertTrue("Opus encoder: none" in hidden)
        assertTrue("Secret Title 01.mp3" in DiagnosticsText.of(report, includeFileNames = true) { "t$it" })
    }
}
