package com.mobyle.abbay.presentation.playback

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mobyle.abbay.presentation.playback.media.AudiobookExoPlayer
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Phase 3 acceptance: a speed change applies within 100 ms. The player's position follows the
 * audio actually leaving the AudioTrack, so the moment its slope changes is the moment the new speed is heard.
 */
@RunWith(AndroidJUnit4::class)
class SpeedChangeLatencyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @Test
    fun speedChangesAreHeardWithinOneHundredMilliseconds() {
        // The emulator's virtual audio device corrects its timestamps ("device stall"), so its positions are not a
        // measure of real output; the criterion is checked on real hardware.
        assumeFalse("needs real audio hardware", Build.HARDWARE in EMULATOR_HARDWARE)
        // Both directions, plus the ends of the 0.5–3.5 range.
        val latencies = SPEED_CHANGES.map { (from, to) -> measureOnce(from, to) }
        Log.i(TAG, "speed change latencies (ms): $latencies")
        assertTrue("latencies $latencies", latencies.max() <= LIMIT_MS)
    }

    /** Plays at [from], switches to [to], returns ms until the position advances at the new rate. */
    private fun measureOnce(from: Float, to: Float): Long {
        val samples = mutableListOf<Pair<Long, Long>>()
        var switchedAt = 0L
        val done = CountDownLatch(1)
        lateinit var player: ExoPlayer

        mainHandler.post {
            player = AudiobookExoPlayer.build(context, com.mobyle.abbay.presentation.playback.effects.EffectsState())
            player.setMediaItem(MediaItem.fromUri("asset:///vbr_no_xing.mp3"))
            player.setPlaybackSpeed(from)
            player.prepare()
            player.play()
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) return
                    // Let output settle, then switch and sample every 5 ms.
                    mainHandler.postDelayed({
                        switchedAt = SystemClock.elapsedRealtime()
                        player.setPlaybackSpeed(to)
                        sample(player, samples, done)
                    }, SETTLE_MS)
                }
            })
        }
        assertTrue(done.await(TIMEOUT_S, TimeUnit.SECONDS))
        mainHandler.post { player.release() }
        val rates = samples.windowed(RATE_WINDOW, RATE_WINDOW).map { window ->
            val (startTime, startPosition) = window.first()
            val (endTime, endPosition) = window.last()
            "${startTime - switchedAt}:${"%.2f".format((endPosition - startPosition).toFloat() / (endTime - startTime))}"
        }
        Log.i(TAG, "$from -> $to rates: $rates")
        return switchTime(samples, switchedAt, from, to)
    }

    private fun sample(player: ExoPlayer, samples: MutableList<Pair<Long, Long>>, done: CountDownLatch) {
        samples += SystemClock.elapsedRealtime() to player.currentPosition
        if (samples.size <
            SAMPLE_COUNT
        ) {
            mainHandler.postDelayed({ sample(player, samples, done) }, SAMPLE_INTERVAL_MS)
        } else {
            done.countDown()
        }
    }

    /**
     * First moment the position rate, over 50 ms windows, crosses halfway from [from] to [to] and stays there for two
     * windows. ExoPlayer later smooths its position estimate, so the start of the change is what tracks the audio.
     */
    private fun switchTime(samples: List<Pair<Long, Long>>, switchedAt: Long, from: Float, to: Float): Long {
        val midpoint = (from + to) / 2
        val rates = samples.windowed(RATE_WINDOW_SAMPLES).map { window ->
            val (startTime, startPosition) = window.first()
            val (endTime, endPosition) = window.last()
            endTime to (endPosition - startPosition).toFloat() / (endTime - startTime)
        }
        fun crossed(rate: Float) = if (to > from) rate > midpoint else rate < midpoint
        val index = rates.indices.first { start ->
            crossed(rates[start].second) &&
                rates.getOrNull(start + RATE_WINDOW_SAMPLES)?.second?.let(::crossed) != false
        }
        return rates[index].first - switchedAt
    }

    private companion object {
        const val TAG = "SpeedLatency"
        val EMULATOR_HARDWARE = setOf("ranchu", "goldfish")
        val SPEED_CHANGES = listOf(1.0f to 2.0f, 2.0f to 1.0f, 1.0f to 2.0f, 2.0f to 1.0f, 0.5f to 3.5f, 3.5f to 0.5f)
        const val LIMIT_MS = 100L
        const val SETTLE_MS = 1_500L
        const val SAMPLE_INTERVAL_MS = 5L
        const val SAMPLE_COUNT = 300
        const val RATE_WINDOW_SAMPLES = 10
        const val RATE_WINDOW = 20
        const val TIMEOUT_S = 30L
    }
}
