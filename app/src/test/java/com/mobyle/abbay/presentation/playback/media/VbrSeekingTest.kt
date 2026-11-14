package com.mobyle.abbay.presentation.playback.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mp3.Mp3Extractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

@OptIn(UnstableApi::class)
class VbrSeekingTest {
    private val fixture = File(requireNotNull(javaClass.classLoader?.getResource("vbr_no_xing.mp3")).toURI())

    private class Capture : ExtractorOutput, TrackOutput {
        var seekMap: SeekMap? = null
        val frameTimesByOffset = sortedMapOf<Long, Long>()
        var input: ExtractorInput? = null
        private var pendingFrameOffset: Long? = null
        private val scratch = ByteArray(8_192)

        override fun track(id: Int, type: Int): TrackOutput = this

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
        }

        override fun format(format: Format) = Unit

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
            if (pendingFrameOffset == null) pendingFrameOffset = this.input?.position
            return input.read(scratch, 0, minOf(length, scratch.size))
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) = data.skipBytes(length)

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) {
            pendingFrameOffset?.let { frameTimesByOffset[it] = timeUs }
            pendingFrameOffset = null
        }
    }

    private fun readAll(flags: Int): Capture {
        val capture = Capture()
        val extractor: Extractor = Mp3Extractor(flags)
        fixture.inputStream().buffered().use { stream ->
            val reader = DataReader { buffer, offset, length ->
                val read = stream.read(buffer, offset, length)
                if (read < 0) C.RESULT_END_OF_INPUT else read
            }
            val input = DefaultExtractorInput(reader, 0, fixture.length())
            capture.input = input
            extractor.init(capture)
            val positionHolder = PositionHolder()
            while (extractor.read(input, positionHolder) != Extractor.RESULT_END_OF_INPUT) Unit
        }
        return capture
    }

    private fun Capture.trueTimeAt(byteOffset: Long): Long =
        frameTimesByOffset.headMap(byteOffset + 1).let { if (it.isEmpty()) 0L else it[it.lastKey()]!! }

    @Test
    fun `index seeking lands within half a second of the target`() {
        val capture = readAll(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val seekMap = requireNotNull(capture.seekMap)

        assertTrue(seekMap.isSeekable)
        assertEquals(60.0, seekMap.durationUs / 1_000_000.0, 0.5)
        listOf(5L, 17L, 30L, 44L, 58L).map { it * C.MICROS_PER_SECOND }.forEach { targetUs ->
            val point = seekMap.getSeekPoints(targetUs).first
            val landedUs = capture.trueTimeAt(point.position)
            assertTrue("target ${targetUs / 1000} ms landed at ${landedUs / 1000} ms", abs(landedUs - targetUs) <= 500_000L)
        }
    }

    @Test
    fun `a constant-bitrate estimate is off by more than half a second on this file`() {
        val truth = readAll(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val estimate = readAll(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
        val seekMap = requireNotNull(estimate.seekMap)

        val worstErrorUs = listOf(17L, 30L, 44L).map { it * C.MICROS_PER_SECOND }.maxOf { targetUs ->
            val point = seekMap.getSeekPoints(targetUs).first
            abs(truth.trueTimeAt(point.position) - targetUs)
        }
        assertTrue("worst error ${worstErrorUs / 1000} ms", worstErrorUs > 500_000L)
    }
}
