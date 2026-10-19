package com.mobyle.abbay.infra.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Look-ahead peak limiter: 3 ms look-ahead, −1 dBFS ceiling, channels linked.
 * The gain for a delayed sample is the moving average of the sliding minimum of the required gains over the
 * look-ahead window, so it has reached the needed reduction when the peak arrives and never overshoots the ceiling;
 * recovery takes [RELEASE_MS]. Below the ceiling it is a pure delay. Interleaved float samples, in place.
 */
class PeakLimiter(private val channelCount: Int, sampleRate: Int, ceilingDb: Float = CEILING_DB) {
    private val ceiling = Decibels.toGain(ceilingDb)
    private val lookAhead = max(1, sampleRate * LOOK_AHEAD_MS / 1_000)
    private val delay = FloatArray(lookAhead * channelCount)
    private var delayIndex = 0

    /** Monotonic deque (frame index, required gain) for the minimum over the last lookAhead + 1 frames. */
    private val queueFrames = LongArray(lookAhead + 1)
    private val queueGains = FloatArray(lookAhead + 1)
    private var queueHead = 0
    private var queueSize = 0
    private var frameIndex = 0L

    /** Moving average of the sliding minimum. */
    private val minimumHistory = FloatArray(lookAhead)
    private var historyIndex = 0
    private var historySum = lookAhead.toDouble()
    private var releasedGain = 1f
    private val releaseCoefficient = exp(-1.0 / (RELEASE_MS / 1_000.0 * sampleRate)).toFloat()

    /** Frames of delay added to the signal. */
    val latencyFrames: Int get() = lookAhead

    init {
        reset()
    }

    fun reset() {
        delay.fill(0f)
        delayIndex = 0
        queueHead = 0
        queueSize = 0
        frameIndex = 0L
        minimumHistory.fill(1f)
        historyIndex = 0
        historySum = lookAhead.toDouble()
        releasedGain = 1f
    }

    fun process(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            val base = frame * channelCount
            var peak = 0f
            for (channel in 0 until channelCount) peak = max(peak, abs(samples[base + channel]))
            val minimum = pushRequired(if (peak > ceiling) ceiling / peak else 1f)

            historySum += minimum - minimumHistory[historyIndex]
            minimumHistory[historyIndex] = minimum
            historyIndex = (historyIndex + 1) % lookAhead
            val averaged = (historySum / lookAhead).toFloat()
            releasedGain = minOf(averaged, releaseCoefficient * releasedGain + (1 - releaseCoefficient))

            val delayBase = delayIndex * channelCount
            for (channel in 0 until channelCount) {
                val delayed = delay[delayBase + channel]
                delay[delayBase + channel] = samples[base + channel]
                samples[base + channel] = delayed * releasedGain
            }
            delayIndex = (delayIndex + 1) % lookAhead
        }
    }

    /** Adds this frame's required gain and returns the minimum over the last lookAhead + 1 frames. */
    private fun pushRequired(gain: Float): Float {
        val capacity = queueFrames.size
        val oldestAllowed = frameIndex - lookAhead
        while (queueSize > 0 && queueFrames[queueHead] < oldestAllowed) {
            queueHead = (queueHead + 1) % capacity
            queueSize--
        }
        while (queueSize > 0 && queueGains[(queueHead + queueSize - 1) % capacity] >= gain) queueSize--
        val slot = (queueHead + queueSize) % capacity
        queueFrames[slot] = frameIndex
        queueGains[slot] = gain
        queueSize++
        frameIndex++
        return queueGains[queueHead]
    }

    companion object {
        const val CEILING_DB = -1f
        private const val LOOK_AHEAD_MS = 3
        private const val RELEASE_MS = 50.0
    }
}
