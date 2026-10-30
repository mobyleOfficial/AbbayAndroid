package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Signals {
    const val RATE = 48_000

    fun sine(frequencyHz: Double, seconds: Double, amplitude: Float, channels: Int = 1, rate: Int = RATE): FloatArray {
        val frames = (seconds * rate).toInt()
        return FloatArray(frames * channels) { index ->
            val frame = index / channels
            (amplitude * sin(2 * PI * frequencyHz * frame / rate)).toFloat()
        }
    }

    fun bursts(burstSeconds: Double, silenceSeconds: Double, count: Int, level: Float, rate: Int = RATE, seed: Int = 3): FloatArray {
        val random = Random(seed)
        val burstFrames = (burstSeconds * rate).toInt()
        val silenceFrames = (silenceSeconds * rate).toInt()
        val output = FloatArray(count * burstFrames + (count - 1) * silenceFrames)
        var position = 0
        repeat(count) { burst ->
            for (frame in 0 until burstFrames) {
                val envelope = 0.6f + 0.4f * sin(2 * PI * 4.0 * frame / rate).toFloat()
                output[position + frame] = level * envelope * (random.nextFloat() * 2f - 1f)
            }
            position += burstFrames
            if (burst < count - 1) position += silenceFrames
        }
        return output
    }

    fun rms(samples: FloatArray, from: Int = 0, to: Int = samples.size, channels: Int = 1, channel: Int = 0): Double {
        var sum = 0.0
        var count = 0
        var index = from - from % channels + channel
        while (index < to) {
            sum += samples[index] * samples[index]
            count++
            index += channels
        }
        return sqrt(sum / count)
    }

    fun peak(samples: FloatArray): Float = samples.maxOf { abs(it) }

    fun decibels(ratio: Double): Double = 20 * kotlin.math.log10(ratio)

    fun maximumStep(samples: FloatArray, channels: Int = 1, channel: Int = 0): Float {
        var maximum = 0f
        var index = channel + channels
        while (index < samples.size) {
            maximum = maxOf(maximum, abs(samples[index] - samples[index - channels]))
            index += channels
        }
        return maximum
    }
}
