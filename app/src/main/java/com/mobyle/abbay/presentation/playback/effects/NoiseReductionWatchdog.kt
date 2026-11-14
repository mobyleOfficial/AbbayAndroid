package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener

@OptIn(UnstableApi::class)
class NoiseReductionWatchdog(
    private val state: EffectsState,
    private val onTooSlow: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : AnalyticsListener {
    private val underrunTimes = ArrayDeque<Long>()

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long,
    ) {
        if (!state.isNoiseReductionRunning) {
            underrunTimes.clear()
            return
        }
        val now = clock()
        underrunTimes.addLast(now)
        while (now - underrunTimes.first() > WINDOW_MS) underrunTimes.removeFirst()
        if (underrunTimes.size >= UNDERRUNS) {
            underrunTimes.clear()
            onTooSlow()
        }
    }

    private companion object {
        const val UNDERRUNS = 3
        const val WINDOW_MS = 10_000L
    }
}
