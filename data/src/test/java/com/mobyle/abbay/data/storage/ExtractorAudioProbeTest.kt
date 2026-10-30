package com.mobyle.abbay.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ExtractorAudioProbeTest {
    private fun fixture(name: String) = File(requireNotNull(javaClass.classLoader?.getResource(name)).toURI())

    private fun probe(file: File) = RandomAccessFile(file, "r").use { ExtractorAudioProbe.probe(it.channel) }

    @Test
    fun `an M4B with the moov box after the audio data has audio, its duration and its tags`() {
        val result = requireNotNull(probe(fixture("moov_at_end.m4b")))

        assertTrue(result.hasAudio)
        assertEquals(3_000.0, result.durationMs.toDouble(), 100.0)
        assertEquals("Probe Album", result.tags["TALB"])
        assertEquals("Probe Artist", result.tags["TPE1"])
    }

    @Test
    fun `an MP3 is read too`() {
        val result = requireNotNull(probe(fixture("vbr_with_xing.mp3")))

        assertTrue(result.hasAudio)
    }

    @Test
    fun `a file that is not audio is not recognized`() {
        val notAudio = File.createTempFile("not_audio", ".m4b").apply {
            writeText("plain text renamed to look like an audiobook")
            deleteOnExit()
        }

        assertNull(probe(notAudio))
    }
}
