package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("TooManyFunctions")
class Resampler(
    private val inputRate: Int,
    private val outputRate: Int,
    private val channels: Int,
    passbandFraction: Double = PASSBAND_FRACTION,
) {
    private val stopEdge = 0.5 * min(1.0, outputRate.toDouble() / inputRate)
    private val passEdge = stopEdge * passbandFraction
    private val cutoff = (stopEdge + passEdge) / 2

    val halfWidth = ceil((ATTENUATION_DB - KAISER_CONSTANT) / (KAISER_SLOPE * 2 * PI * (stopEdge - passEdge)) / 2).toInt()
    private val table = buildTable()
    private val taps = 2 * halfWidth

    private val divisor = greatestCommonDivisor(inputRate, outputRate)
    private val phaseCount = outputRate / divisor
    private val scratchFilter = FloatArray(taps)
    private val phaseFilters: FloatArray? = if (phaseCount <= MAXIMUM_EXACT_PHASES) buildPhaseFilters() else null

    private var history = FloatArray(0)
    private var historyFrames = 0

    private var inputIndex = 0L
    private var fraction = 0L
    private var consumedFrames = 0L

    val isPassthrough: Boolean get() = inputRate == outputRate

    init {
        history = FloatArray(halfWidth * channels)
        reset()
    }

    var output = FloatArray(0)
        private set

    fun process(input: FloatArray, offset: Int, frames: Int): Int {
        append(input, offset, frames)
        return produce(lastUsableFrame = consumedFrames + historyFrames - halfWidth - 1)
    }

    fun process(input: FloatArray): FloatArray {
        if (isPassthrough) return input.copyOf()
        val frames = process(input, 0, input.size / channels)
        return output.copyOf(frames * channels)
    }

    fun flush(): FloatArray = if (isPassthrough) FloatArray(0) else process(FloatArray(halfWidth * channels))

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

    private fun dot(filter: FloatArray, start: Int, firstFrame: Int, channel: Int): Float {
        var sum = 0f
        var historyIndex = firstFrame * channels + channel
        for (tap in 0 until taps) {
            sum += history[historyIndex] * filter[start + tap]
            historyIndex += channels
        }
        return sum
    }

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

        private const val BETA = 0.1102 * (ATTENUATION_DB - 8.7)
        private const val KAISER_CONSTANT = 8.0
        private const val KAISER_SLOPE = 2.285
        private const val BESSEL_EPSILON = 1e-12
    }
}
