package com.mobyle.abbay.presentation.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.effects.AudioEffects
import com.mobyle.abbay.presentation.playback.effects.EffectsState
import com.mobyle.abbay.presentation.playback.media.AudiobookExoPlayer
import com.mobyle.abbay.presentation.playback.remote.AudioRoute
import com.mobyle.abbay.presentation.playback.remote.ProcessedAudioStream
import com.mobyle.abbay.presentation.playback.remote.StreamRestart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class ProcessedCastStreamTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val effects = EffectsState()
    private val route = AudioRoute()
    private val stream = ProcessedAudioStream()
    private lateinit var player: ExoPlayer

    @After
    fun release() {
        stream.close()
        onMain { player.release() }
    }

    @Test
    fun theProcessedAudioReachesTheReceiverWithEffectsSpeedAndPosition() {
        val file = File(context.cacheDir, "vbr_no_xing.mp3").apply {
            instrumentation.context.assets.open("vbr_no_xing.mp3").use { input -> outputStream().use(input::copyTo) }
        }
        onMain {
            player = AudiobookExoPlayer.build(context, effects, route)
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.prepare()
            player.play()
        }
        waitMs(1_000)

        val flatStartMs = onMain { player.currentPosition }
        val flat = startGeneration()
        val flatAudio = receive(flat, durationMs = 2_000)
        assertTrue("audio arrives", flatAudio.isNotEmpty())
        val flatRms = rms(flatAudio)
        assertTrue("not silence: $flatRms", flatRms > SILENCE_RMS)
        val heardMs = flatAudio.size / bytesPerMs()
        val positionMs = onMain { player.currentPosition }
        assertTrue(
            "position $positionMs follows what was heard ($heardMs ms from $flatStartMs)",
            positionMs - flatStartMs in heardMs - 400L..heardMs + 1_500L,
        )

        val sameRms = rms(receive(startGeneration(atMs = PASSAGE_MS), durationMs = 1_500))
        effects.current = AudioEffects(equalizerGains = List(AudioEffects.BAND_COUNT) { AudioEffects.MAXIMUM_BAND_DB })
        val boostedRms = rms(receive(startGeneration(atMs = PASSAGE_MS), durationMs = 1_500))
        assertTrue("equalizer applied: $sameRms → $boostedRms", boostedRms > sameRms * 1.3)

        effects.current = AudioEffects()
        onMain { player.playbackParameters = PlaybackParameters(2f) }
        val fast = startGeneration()
        val before = onMain { player.currentPosition }
        val fastAudio = receive(fast, durationMs = 1_500)
        val advancedMs = onMain { player.currentPosition } - before
        val fastHeardMs = fastAudio.size / bytesPerMs()
        assertTrue("2× speed: $fastHeardMs ms heard moved $advancedMs ms", advancedMs in fastHeardMs * 3 / 2..fastHeardMs * 5 / 2 + 1_000)

        route.stream = null
        onMain { player.seekTo(player.currentMediaItemIndex, StreamRestart.positionAfter(player.currentPosition, player.duration)) }
        waitMs(500)
        assertEquals(true, onMain { player.isPlaying })
    }

    private fun startGeneration(atMs: Long? = null): Int {
        val previous = stream.generation
        route.stream = stream
        onMain { player.seekTo(player.currentMediaItemIndex, atMs ?: StreamRestart.positionAfter(player.currentPosition, player.duration)) }
        val deadline = System.currentTimeMillis() + WAIT_MS
        while (stream.generation == previous && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertTrue("a new generation began", stream.generation != previous)
        return stream.generation
    }

    private fun receive(generation: Int, durationMs: Long): ByteArray {
        val input: InputStream = stream.open(generation) ?: return ByteArray(0)
        val received = java.io.ByteArrayOutputStream()
        val started = System.currentTimeMillis()
        val chunk = ByteArray(bytesPerMs() * POLL_MS.toInt())
        while (System.currentTimeMillis() - started < durationMs) {
            val playedMs = received.size() / bytesPerMs()
            stream.updateReceiver(generation, playedMs.toLong(), isPlaying = true)
            val read = input.read(chunk, 0, chunk.size)
            if (read < 0) break
            received.write(chunk, 0, read)
            Thread.sleep(POLL_MS)
        }
        return received.toByteArray()
    }

    private fun bytesPerMs(): Int = (stream.sampleRateHz * stream.channelCount * 2 / 1_000).coerceAtLeast(1)

    private fun rms(pcm: ByteArray): Double {
        val samples = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        var sum = 0.0
        val count = samples.remaining()
        repeat(count) {
            val sample = samples.get().toDouble()
            sum += sample * sample
        }
        return if (count == 0) 0.0 else sqrt(sum / count)
    }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun waitMs(milliseconds: Long) = Thread.sleep(milliseconds)

    private companion object {
        const val SILENCE_RMS = 50.0
        const val PASSAGE_MS = 20_000L
        const val WAIT_MS = 5_000L
        const val POLL_MS = 20L
    }
}
