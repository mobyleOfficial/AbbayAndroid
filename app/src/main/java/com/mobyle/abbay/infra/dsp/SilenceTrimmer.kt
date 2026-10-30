package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("TooManyFunctions")
class SilenceTrimmer(
    private val channelCount: Int,
    sampleRate: Int,
    private val settings: Settings,
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

    private val head = FloatArray(maxOf(minimumFrames, keptHalfFrames) * channelCount + windowFrames * channelCount)
    private var headFrames = 0
    private val tail = FloatArray(tailFrames * channelCount)
    private var tailStart = 0
    private var tailCount = 0
    private var runFrames = 0L

    private var inputFrames = 0L
    var savedFrames = 0L
        private set

    private lateinit var output: FloatArray
    private var outputFrames = 0

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

    fun drain(destination: FloatArray, destinationOffset: Int = 0): Int {
        output = destination
        outputFrames = destinationOffset
        endRun()
        emit(window, 0, windowFill)
        inputFrames += windowFill
        windowFill = 0
        return outputFrames - destinationOffset
    }

    val nextInputFrame: Long get() = inputFrames + windowFill

    fun rebase(nextFrame: Long) {
        inputFrames = nextFrame - windowFill
    }

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

        fun rms(samples: FloatArray, count: Int = samples.size): Float {
            var sum = 0.0
            for (index in 0 until count) sum += samples[index] * samples[index]
            return sqrt(sum / count).toFloat()
        }
    }
}
