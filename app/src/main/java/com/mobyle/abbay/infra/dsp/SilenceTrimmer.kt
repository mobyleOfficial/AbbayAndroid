package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Silence trimming. The input is cut into 10 ms windows; a window is silent when
 * its RMS over all channels is below the threshold. A run of silent windows longer than `minimumSilence` is a pause:
 * only `keptSilence` of it is output (half from its start, half from its end), joined by a 5 ms equal-power
 * crossfade. Windows inside a protected range (near chapter and track boundaries) are never dropped: they end the
 * current run and are output as they are, so only silence outside those ranges is shortened.
 *
 * Streaming: [process] takes interleaved float frames and writes what can be output now; a pause is held back until
 * it ends. Only the start, the most recent end and the length of a long pause are kept, so memory does not grow with
 * its length. All buffers are allocated in the constructor.
 */
@Suppress("TooManyFunctions") // Small steps of one streaming algorithm.
class SilenceTrimmer(
    private val channelCount: Int,
    sampleRate: Int,
    private val settings: Settings,
    /** True when input frames [startFrame, endFrame) touch a range that must not be shortened. */
    private val isProtected: (startFrame: Long, endFrame: Long) -> Boolean = { _, _ -> false },
) {
    data class Settings(val thresholdDb: Float, val minimumSilenceMs: Int, val keptSilenceMs: Int)

    private val windowFrames = sampleRate * WINDOW_MS / 1_000
    private val minimumFrames = sampleRate * settings.minimumSilenceMs / 1_000
    private val keptHalfFrames = sampleRate * settings.keptSilenceMs / 2 / 1_000
    private val crossfadeFrames = sampleRate * CROSSFADE_MS / 1_000
    private val tailFrames = keptHalfFrames + crossfadeFrames
    private val thresholdPower = Decibels.toGain(settings.thresholdDb).let { it * it }

    private val window = FloatArray(windowFrames * channelCount)
    private var windowFill = 0

    /** Silent run: its first frames (up to minimumSilence, later only the kept head) and a ring of its last frames. */
    private val head = FloatArray(maxOf(minimumFrames, keptHalfFrames) * channelCount + windowFrames * channelCount)
    private var headFrames = 0
    private val tail = FloatArray(tailFrames * channelCount)
    private var tailStart = 0
    private var tailCount = 0
    private var runFrames = 0L

    /** Input frames seen so far (since the last [reset]) and frames dropped. */
    private var inputFrames = 0L
    var savedFrames = 0L
        private set

    private lateinit var output: FloatArray
    private var outputFrames = 0

    /**
     * Processes [frameCount] frames of [input] and writes the frames ready for output to [destination] from frame
     * [destinationOffset], returning how many. [destination] must have room for [maximumOutputFrames] after the offset.
     */
    fun process(input: FloatArray, frameCount: Int, destination: FloatArray, destinationOffset: Int = 0): Int {
        output = destination
        outputFrames = destinationOffset
        var frame = 0
        while (frame < frameCount) {
            val copy = minOf(windowFrames - windowFill, frameCount - frame)
            input.copyInto(window, windowFill * channelCount, frame * channelCount, (frame + copy) * channelCount)
            windowFill += copy
            frame += copy
            if (windowFill == windowFrames) {
                handleWindow(windowFrames)
                windowFill = 0
            }
        }
        return outputFrames - destinationOffset
    }

    /** End of stream: everything held back is output unchanged. */
    fun drain(destination: FloatArray, destinationOffset: Int = 0): Int {
        output = destination
        outputFrames = destinationOffset
        endRun()
        emit(window, 0, windowFill)
        inputFrames += windowFill
        windowFill = 0
        return outputFrames - destinationOffset
    }

    /** Index of the next input frame in the stream's frame count (track time × sample rate). */
    val nextInputFrame: Long get() = inputFrames + windowFill

    /** The stream jumped (a new track joined without a flush): the next input frame is [nextFrame]. */
    fun rebase(nextFrame: Long) {
        inputFrames = nextFrame - windowFill
    }

    /** Upper bound of frames one [process] call can output for [frameCount] input frames. */
    fun maximumOutputFrames(frameCount: Int): Int = frameCount + windowFrames + head.size / channelCount + tailFrames

    fun reset(startFrame: Long = 0L) {
        windowFill = 0
        headFrames = 0
        tailCount = 0
        tailStart = 0
        runFrames = 0
        inputFrames = startFrame
    }

    private fun handleWindow(frames: Int) {
        val windowStart = inputFrames
        inputFrames += frames
        if (isSilent(frames) && !isProtected(windowStart, windowStart + frames)) {
            addToRun(frames)
        } else {
            endRun()
            emit(window, 0, frames)
        }
    }

    private fun isSilent(frames: Int): Boolean {
        var power = 0.0
        val samples = frames * channelCount
        for (index in 0 until samples) power += window[index] * window[index]
        return power / samples < thresholdPower
    }

    private fun addToRun(frames: Int) {
        runFrames += frames
        // The head keeps the whole run until it is long enough to be a pause, then only its first half of kept silence.
        val headCapacity = head.size / channelCount
        val toHead = minOf(frames, headCapacity - headFrames)
        if (toHead > 0) {
            window.copyInto(head, headFrames * channelCount, 0, toHead * channelCount)
            headFrames += toHead
        }
        for (frame in 0 until frames) pushTail(frame)
    }

    private fun pushTail(windowFrame: Int) {
        val slot = (tailStart + tailCount) % tailFrames
        window.copyInto(tail, slot * channelCount, windowFrame * channelCount, (windowFrame + 1) * channelCount)
        if (tailCount < tailFrames) tailCount++ else tailStart = (tailStart + 1) % tailFrames
    }

    /** The run ended (speech, a protected range, or the end of the stream): output it whole, or shortened if a pause. */
    private fun endRun() {
        if (runFrames == 0L) return
        if (runFrames >= minimumFrames) {
            emitHeadJoinedToTail()
            savedFrames += runFrames - keptHalfFrames * 2L
        } else {
            emit(head, 0, runFrames.toInt())
        }
        runFrames = 0
        headFrames = 0
        tailCount = 0
        tailStart = 0
    }

    /** First keptHalf frames of the run, crossfaded over 5 ms into its last keptHalf frames. */
    private fun emitHeadJoinedToTail() {
        emit(head, 0, keptHalfFrames - crossfadeFrames)
        for (index in 0 until crossfadeFrames) {
            val position = (index + 0.5) / crossfadeFrames
            val fadeOut = cos(position * PI / 2).toFloat()
            val fadeIn = sin(position * PI / 2).toFloat()
            val headFrame = keptHalfFrames - crossfadeFrames + index
            val tailFrame = (tailStart + index) % tailFrames
            for (channel in 0 until channelCount) {
                val mixed = head[headFrame * channelCount + channel] * fadeOut + tail[tailFrame * channelCount + channel] * fadeIn
                output[outputFrames * channelCount + channel] = mixed
            }
            outputFrames++
        }
        emitTail(crossfadeFrames, tailCount)
    }

    private fun emitTail(fromFrame: Int, toFrame: Int) {
        for (index in fromFrame until toFrame) {
            val slot = (tailStart + index) % tailFrames
            tail.copyInto(output, outputFrames * channelCount, slot * channelCount, (slot + 1) * channelCount)
            outputFrames++
        }
    }

    private fun emit(source: FloatArray, fromFrame: Int, frames: Int) {
        if (frames <= 0) return
        source.copyInto(output, outputFrames * channelCount, fromFrame * channelCount, (fromFrame + frames) * channelCount)
        outputFrames += frames
    }

    companion object {
        private const val WINDOW_MS = 10
        private const val CROSSFADE_MS = 5

        val GENTLE = Settings(thresholdDb = -50f, minimumSilenceMs = 400, keptSilenceMs = 300)
        val MODERATE = Settings(thresholdDb = -45f, minimumSilenceMs = 300, keptSilenceMs = 200)
        val STRONG = Settings(thresholdDb = -42f, minimumSilenceMs = 200, keptSilenceMs = 100)

        /** RMS of a buffer, for tests and meters. */
        fun rms(samples: FloatArray, count: Int = samples.size): Float {
            var sum = 0.0
            for (index in 0 until count) sum += samples[index] * samples[index]
            return sqrt(sum / count).toFloat()
        }
    }
}
