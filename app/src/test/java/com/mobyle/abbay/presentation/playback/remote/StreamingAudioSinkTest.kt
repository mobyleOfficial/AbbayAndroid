package com.mobyle.abbay.presentation.playback.remote

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.audio.AudioSink
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StreamingAudioSinkTest {
    private val stream = ProcessedAudioStream(capacityBytes = 64 * 1024)
    private val route = AudioRoute().apply { stream = this@StreamingAudioSinkTest.stream }
    private val generations = mutableListOf<Int>()
    private val sink = StreamingAudioSink(effectProcessors = emptyList(), route = route)

    init {
        stream.listener = object : ProcessedAudioStream.Listener {
            override fun onPlay() = Unit

            override fun onPause() = Unit

            override fun onNewGeneration(generation: Int) {
                generations += generation
            }
        }
    }

    @Test
    fun theDecodedPcmReachesTheStreamUnchangedWithoutEffects() {
        sink.configure(pcmFormat(), 0, null)
        val pcm = tone(frames = 1_000)

        assertTrue(sink.handleBuffer(ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN), START_US, 1))

        assertEquals(listOf(1), generations)
        val received = stream.open(stream.generation)!!.readNBytes(pcm.size)
        assertArrayEquals(pcm, received)
    }

    @Test
    fun aFullStreamPacesTheDecoder() {
        sink.configure(pcmFormat(), 0, null)
        val buffer = ByteBuffer.wrap(tone(frames = 30_000)) // 120 KB, the stream holds 64 KB

        assertFalse(sink.handleBuffer(buffer, START_US, 1))
        assertTrue(buffer.hasRemaining())
        stream.open(stream.generation)!!.readNBytes(64 * 1024)
        assertTrue(sink.handleBuffer(buffer, START_US, 1))
    }

    @Test
    fun thePositionIsWhereTheReceiverIs() {
        sink.configure(pcmFormat(), 0, null)
        sink.handleBuffer(ByteBuffer.wrap(tone(frames = SAMPLE_RATE / 10)), START_US, 1)
        assertEquals(AudioSink.CURRENT_POSITION_NOT_SET, sink.getCurrentPositionUs(false))

        stream.updateReceiver(stream.generation, positionMs = 50, isPlaying = false)

        assertEquals(START_US + 50_000, sink.getCurrentPositionUs(false))
        assertTrue(sink.hasPendingData())
    }

    @Test
    fun speedIsAppliedAndTheMediaPositionScalesWithIt() {
        sink.setPlaybackParameters(PlaybackParameters(2f))
        sink.configure(pcmFormat(), 0, null)
        repeat(4) { sink.handleBuffer(ByteBuffer.wrap(tone(frames = SAMPLE_RATE / 10)), START_US + it * 100_000L, 1) }

        // 400 ms of book at 2× is about 200 ms of output (Sonic keeps a little in its buffer).
        val outputMs = stream.bufferedBytes / (CHANNELS * 2) * 1_000 / SAMPLE_RATE
        assertTrue("output $outputMs ms", outputMs in 150..210)
        stream.updateReceiver(stream.generation, positionMs = 50, isPlaying = false)
        // 50 ms heard at 2× is 100 ms of book; the checkpoint grid and Sonic's latency allow a little slack.
        val positionUs = sink.getCurrentPositionUs(false)
        assertTrue("position $positionUs", positionUs - START_US in 80_000L..130_000L)
    }

    @Test
    fun aFlushStartsANewGenerationThatTheOldReaderDoesNotSee() {
        sink.configure(pcmFormat(), 0, null)
        sink.handleBuffer(ByteBuffer.wrap(tone(frames = 100)), START_US, 1)
        val oldReader = stream.open(stream.generation)!!

        sink.flush()

        assertEquals(listOf(1, 2), generations)
        assertEquals(-1, oldReader.read(ByteArray(16), 0, 16))
        assertEquals(0, stream.bufferedBytes)
    }

    private fun pcmFormat(): Format = Format.Builder()
        .setSampleMimeType(MimeTypes.AUDIO_RAW)
        .setPcmEncoding(C.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE)
        .setChannelCount(CHANNELS)
        .build()

    private fun tone(frames: Int): ByteArray {
        val buffer = ByteBuffer.allocate(frames * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { frame ->
            val sample = (Math.sin(2 * Math.PI * 440 * frame / SAMPLE_RATE) * 8_000).toInt().toShort()
            repeat(CHANNELS) { buffer.putShort(sample) }
        }
        return buffer.array()
    }

    private companion object {
        const val SAMPLE_RATE = 8_000
        const val CHANNELS = 2
        const val START_US = 10_000_000L
    }
}
