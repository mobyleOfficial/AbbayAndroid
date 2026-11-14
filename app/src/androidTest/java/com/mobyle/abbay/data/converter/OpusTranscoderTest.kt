package com.mobyle.abbay.data.converter

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mobyle.abbay.infra.dsp.ResamplerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OpusTranscoderTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun convertsAnMp3WithTagsAndCover() {
        val transcoder = OpusTranscoder(targetContext, ResamplerFactory())
        assertNotNull("this device has no Opus encoder", transcoder.encoderName())
        val source = File(targetContext.cacheDir, "source.mp3")
        testContext.assets.open("vbr_no_xing.mp3").use { input -> source.outputStream().use(input::copyTo) }
        val target = File(targetContext.cacheDir, "converted.opus")
        val cover = ByteArray(5_000) { it.toByte() }
        var lastProgress = 0f

        val result = target.outputStream().use { output ->
            transcoder.transcode(
                sourceUri = Uri.fromFile(source).toString(),
                output = output,
                bitrateKbps = 32,
                forceMono = true,
                tags = OpusTags(listOf("TITLE" to "Capítulo 1", "ALBUM" to "Livro"), OpusTags.Cover("image/jpeg", cover)),
                onProgress = { lastProgress = it },
                isCancelled = { false },
            )
        }

        val extractor = MediaExtractor().apply { setDataSource(target.path) }
        val format = extractor.getTrackFormat(0)
        extractor.release()
        assertEquals(MediaFormat.MIMETYPE_AUDIO_OPUS, format.getString(MediaFormat.KEY_MIME))
        assertEquals(1, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        val durationDifferenceMs = kotlin.math.abs(format.getLong(MediaFormat.KEY_DURATION) - result.decodedDurationUs) / 1_000
        assertTrue("durations differ by $durationDifferenceMs ms", durationDifferenceMs <= 500)
        assertEquals(60.0, result.decodedDurationUs / 1e6, 0.5)
        assertTrue(lastProgress > 0f)
        assertTrue("smaller than the source", target.length() < source.length())

        val retriever = MediaMetadataRetriever().apply { setDataSource(target.path) }
        assertEquals("Capítulo 1", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
        assertEquals("Livro", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
        retriever.release()
    }
}
