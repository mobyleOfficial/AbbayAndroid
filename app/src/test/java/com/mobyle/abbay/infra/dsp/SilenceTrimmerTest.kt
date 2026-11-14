package com.mobyle.abbay.infra.dsp

import org.junit.Assert.assertEquals
import org.junit.Test

class SilenceTrimmerTest {
    private val rate = Signals.RATE

    @Test
    fun moderateShortensEachTwoSecondSilenceToTwoHundredMilliseconds() {
        val input = Signals.bursts(burstSeconds = 1.0, silenceSeconds = 2.0, count = 4, level = 0.3f)
        val trimmer = SilenceTrimmer(1, rate, SilenceTrimmer.MODERATE)

        val output = runInChunks(trimmer, input)

        val silences = silentRuns(output)
        assertEquals(3, silences.size)
        silences.forEach { frames -> assertEquals(0.200, frames.toDouble() / rate, 0.010) }
        assertEquals(input.size - trimmer.savedFrames, output.size.toLong())
        assertEquals(3 * (2.0 - 0.2), trimmer.savedFrames.toDouble() / rate, 0.001)
        assertEquals(nonSilentSamples(input), nonSilentSamples(output))
    }

    @Test
    fun silenceNearAChapterBoundaryIsNotShortened() {
        val input = Signals.bursts(burstSeconds = 1.0, silenceSeconds = 0.8, count = 2, level = 0.3f)
        val boundary = (1.8 * rate).toLong()
        val oneSecond = rate.toLong()
        val trimmer = SilenceTrimmer(1, rate, SilenceTrimmer.STRONG) { start, end ->
            end > boundary - oneSecond &&
                start < boundary + oneSecond
        }

        val output = runInChunks(trimmer, input)

        assertEquals(input.size, output.size)
        assertEquals(0L, trimmer.savedFrames)
    }

    @Test
    fun onlyTheSilenceOutsideTheProtectedSecondIsShortened() {
        val input = Signals.bursts(burstSeconds = 1.0, silenceSeconds = 3.0, count = 2, level = 0.3f)
        val boundary = (4.0 * rate).toLong()
        val trimmer = SilenceTrimmer(1, rate, SilenceTrimmer.MODERATE) { start, end -> end > boundary - rate && start < boundary + rate }

        val output = runInChunks(trimmer, input)

        assertEquals(2.0 - 0.2, trimmer.savedFrames.toDouble() / rate, 0.011)
        assertEquals(1.2, silentRuns(output).single().toDouble() / rate, 0.011)
    }

    @Test
    fun shortPausesAreKept() {
        val input = Signals.bursts(burstSeconds = 0.5, silenceSeconds = 0.25, count = 3, level = 0.3f)
        val trimmer = SilenceTrimmer(1, rate, SilenceTrimmer.MODERATE)
        assertEquals(input.size, runInChunks(trimmer, input).size)
    }

    private fun runInChunks(trimmer: SilenceTrimmer, input: FloatArray): FloatArray {
        val output = ArrayList<Float>()
        var position = 0
        var chunk = 997
        while (position < input.size) {
            val frames = minOf(chunk, input.size - position)
            val destination = FloatArray(trimmer.maximumOutputFrames(frames))
            val written = trimmer.process(input.copyOfRange(position, position + frames), frames, destination)
            for (index in 0 until written) output += destination[index]
            position += frames
            chunk = if (chunk == 997) 4_096 else 997
        }
        val destination = FloatArray(trimmer.maximumOutputFrames(0))
        val written = trimmer.drain(destination)
        for (index in 0 until written) output += destination[index]
        return output.toFloatArray()
    }

    private fun silentRuns(samples: FloatArray): List<Int> {
        val runs = mutableListOf<Int>()
        var run = 0
        var seenSound = false
        samples.forEach { sample ->
            if (sample == 0f) {
                run++
            } else {
                if (seenSound && run > MINIMUM_RUN) runs += run
                run = 0
                seenSound = true
            }
        }
        return runs
    }

    private fun nonSilentSamples(samples: FloatArray): List<Float> = samples.filter { it != 0f }

    private companion object {
        const val MINIMUM_RUN = 100
    }
}
