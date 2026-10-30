package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Streaming windowed-sinc resampler for interleaved float audio.
 * A Kaiser-windowed low-pass (≥ 90 dB stopband design, 80 dB required) is tabulated at [PHASES] points per input
 * sample and linearly interpolated, so any pair of rates works. The cutoff sits below the lower of the two Nyquist
 * frequencies. The filter is zero-phase (output frame n is input time n·in/out); it needs [halfWidth] input frames of
 * look-ahead before it can produce, and [flush] returns the tail.
 */
@Suppress("TooManyFunctions") // Streaming API, filter design and the kernel lookup.
class Resampler(
    private val inputRate: Int,
    private val outputRate: Int,
    private val channels: Int,
    /** Where the passband ends, as a fraction of the lower Nyquist frequency; lower is cheaper (fewer taps). */
    passbandFraction: Double = PASSBAND_FRACTION,
) {
    private val stopEdge = 0.5 * min(1.0, outputRate.toDouble() / inputRate)
    private val passEdge = stopEdge * passbandFraction
    private val cutoff = (stopEdge + passEdge) / 2

    /** Taps on each side of the centre, in input samples. */
    val halfWidth = ceil((ATTENUATION_DB - KAISER_CONSTANT) / (KAISER_SLOPE * 2 * PI * (stopEdge - passEdge)) / 2).toInt()
    private val table = buildTable()
    private val taps = 2 * halfWidth

    /**
     * For rate pairs with few distinct output phases (44.1 ↔ 48 kHz has 160), the filter of every phase is computed
     * once; other pairs interpolate the table per output sample.
     */
    private val divisor = greatestCommonDivisor(inputRate, outputRate)
    private val phaseCount = outputRate / divisor
    private val scratchFilter = FloatArray(taps)
    private val phaseFilters: FloatArray? = if (phaseCount <= MAXIMUM_EXACT_PHASES) buildPhaseFilters() else null

    /** Input history per channel: [halfWidth] samples before the current one and the pending ones. */
    private var history = FloatArray(0)
    private var historyFrames = 0

    /** Output position as input index + [fraction] / [outputRate]; exact integer arithmetic, no drift. */
    private var inputIndex = 0L
    private var fraction = 0L
    private var consumedFrames = 0L

    val isPassthrough: Boolean get() = inputRate == outputRate

    init {
        // Leading zeros stand for the samples before the start.
        history = FloatArray(halfWidth * channels)
        reset()
    }

    /**
     * The output of the last [process] call, interleaved; valid until the next call. Grows only when a call produces
     * more than ever before, so steady playback does not allocate.
     */
    var output = FloatArray(0)
        private set

    /** Resamples [frames] interleaved frames of [input] from [offset] (in frames); returns the output frame count. */
    fun process(input: FloatArray, offset: Int, frames: Int): Int {
        append(input, offset, frames)
        return produce(lastUsableFrame = consumedFrames + historyFrames - halfWidth - 1)
    }

    /** Convenience for whole arrays (tests, the converter): resamples [input] and returns a copy of the output. */
    fun process(input: FloatArray): FloatArray {
        if (isPassthrough) return input.copyOf()
        val frames = process(input, 0, input.size / channels)
        return output.copyOf(frames * channels)
    }

    /** The remaining output once the input has ended (the filter's tail). */
    fun flush(): FloatArray = if (isPassthrough) FloatArray(0) else process(FloatArray(halfWidth * channels))

    /** Forgets the stream (a seek): the next input starts a new one. */
    fun reset() {
        history.fill(0f)
        historyFrames = halfWidth
        consumedFrames = -halfWidth.toLong()
        inputIndex = 0L
        fraction = 0L
    }

    private fun append(input: FloatArray, offset: Int, frames: Int) {
        val needed = (historyFrames + frames) * channels
        if (history.size < needed) history = history.copyOf(maxOf(needed, history.size * 2))
        input.copyInto(history, historyFrames * channels, offset * channels, (offset + frames) * channels)
        historyFrames += frames
    }

    private fun produce(lastUsableFrame: Long): Int {
        val maximumFrames = ((lastUsableFrame - inputIndex + 1).coerceAtLeast(0L) * outputRate / inputRate + 2).toInt()
        if (output.size < maximumFrames * channels) output = FloatArray(maximumFrames * channels)
        var frames = 0
        while (inputIndex <= lastUsableFrame) {
            val filters = phaseFilters
            val filterStart: Int
            val filter: FloatArray
            if (filters != null) {
                filter = filters
                filterStart = (fraction / divisor).toInt() * taps
            } else {
                fillFilter(fraction.toDouble() / outputRate)
                filter = scratchFilter
                filterStart = 0
            }
            val firstFrame = (inputIndex - consumedFrames).toInt() - halfWidth + 1
            for (channel in 0 until channels) {
                output[frames * channels + channel] = dot(filter, filterStart, firstFrame, channel)
            }
            frames++
            fraction += inputRate
            inputIndex += fraction / outputRate
            fraction %= outputRate
        }
        discardBefore(inputIndex - halfWidth)
        return frames
    }

    /**
     * Σ x[firstFrame + j] · filter[start + j]. The history always holds the frames needed: [halfWidth] zeros or past
     * frames before the current one, and the look-ahead that [process] waited for.
     */
    private fun dot(filter: FloatArray, start: Int, firstFrame: Int, channel: Int): Float {
        var sum = 0f
        var historyIndex = firstFrame * channels + channel
        for (tap in 0 until taps) {
            sum += history[historyIndex] * filter[start + tap]
            historyIndex += channels
        }
        return sum
    }

    /** Taps for the output at [offset] (0…1) input samples after a frame: h(k − offset) for k in (−halfWidth, halfWidth]. */
    private fun fillFilter(offset: Double) {
        for (tap in 0 until taps) scratchFilter[tap] = kernel(tap - halfWidth + 1 - offset).toFloat()
    }

    private fun buildPhaseFilters(): FloatArray {
        val filters = FloatArray(phaseCount * taps)
        for (phase in 0 until phaseCount) {
            fillFilter(phase.toDouble() * divisor / outputRate)
            scratchFilter.copyInto(filters, phase * taps)
        }
        return filters
    }

    private fun greatestCommonDivisor(first: Int, second: Int): Int = if (second ==
        0
    ) {
        first
    } else {
        greatestCommonDivisor(second, first % second)
    }

    private fun kernel(position: Double): Double {
        val scaled = kotlin.math.abs(position) * PHASES
        val low = scaled.toInt()
        if (low >= table.size - 1) return 0.0
        val weight = scaled - low
        return table[low] * (1 - weight) + table[low + 1] * weight
    }

    private fun discardBefore(frame: Long) {
        val drop = (frame - consumedFrames).toInt().coerceIn(0, historyFrames)
        if (drop == 0) return
        history.copyInto(history, 0, drop * channels, historyFrames * channels)
        historyFrames -= drop
        consumedFrames += drop
    }

    /** h(x) = 2·fc·sinc(2·fc·x)·kaiser(x / halfWidth) for x = j / PHASES, j ≥ 0 (the kernel is symmetric). */
    private fun buildTable(): DoubleArray = DoubleArray(halfWidth * PHASES + 2) { step ->
        val x = step.toDouble() / PHASES
        if (x >= halfWidth) {
            0.0
        } else {
            val argument = 2 * cutoff * x
            val sinc = if (argument == 0.0) 1.0 else sin(PI * argument) / (PI * argument)
            val ratio = x / halfWidth
            2 * cutoff * sinc * besselI0(BETA * sqrt(1 - ratio * ratio)) / besselI0(BETA)
        }
    }

    private fun besselI0(value: Double): Double {
        var sum = 1.0
        var term = 1.0
        var k = 1
        while (term > sum * BESSEL_EPSILON) {
            val half = value / (2 * k)
            term *= half * half
            sum += term
            k++
        }
        return sum
    }

    companion object {
        const val PHASES = 1024
        private const val MAXIMUM_EXACT_PHASES = 2_048
        private const val ATTENUATION_DB = 90.0
        private const val PASSBAND_FRACTION = 0.9

        /** Kaiser's design formulas: beta = 0.1102 (A − 8.7); taps = (A − 8) / (2.285 Δω). */
        private const val BETA = 0.1102 * (ATTENUATION_DB - 8.7)
        private const val KAISER_CONSTANT = 8.0
        private const val KAISER_SLOPE = 2.285
        private const val BESSEL_EPSILON = 1e-12
    }
}
