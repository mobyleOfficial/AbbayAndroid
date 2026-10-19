package com.mobyle.abbay.infra.dsp

/** A speech denoiser for mono 48 kHz frames of [frameSize] samples, delayed by one frame (RNNoise). */
interface FrameDenoiser : AutoCloseable {
    val frameSize: Int

    fun process(input: FloatArray, output: FloatArray)
}

/**
 * Noise reduction around a [FrameDenoiser]: each channel is resampled to 48 kHz
 * when needed, denoised in frames, resampled back, and mixed with the original signal ([wet] 0.5 / 0.75 / 1.0), which
 * is delayed by the same amount. The output has exactly as many frames as the input, [latencyFrames] later: the
 * denoised stream starts with that many zeros, so its delay never depends on how the audio is cut into buffers.
 * Stereo whose channels are identical (common in audiobooks) is denoised once and copied to both channels.
 */
class NoiseReducer(
    private val sampleRate: Int,
    private val channels: Int,
    denoiserFactory: () -> FrameDenoiser,
) : AutoCloseable {
    private val lanes = List(channels) { Lane(denoiserFactory()) }
    private val wetRamp = LinearRamp(1f, LinearRamp.framesFor(sampleRate))
    private var channelBuffer = FloatArray(0)

    /** Times a denoised sample was not ready (0 when [latencyFrames] is right); a zero is played instead. */
    var underflowCount = 0L
        private set

    /** Frames by which the output follows the input. */
    val latencyFrames: Int = lanes.first().latencyFrames

    fun setWet(wet: Float, immediately: Boolean = false) = if (immediately) wetRamp.snapTo(wet) else wetRamp.moveTo(wet)

    /** Processes [frameCount] interleaved frames of [samples] in place. */
    fun process(samples: FloatArray, frameCount: Int) {
        if (channelBuffer.size < frameCount) channelBuffer = FloatArray(frameCount)
        if (isDualMono && !channelsAreEqual(samples, frameCount)) {
            // The channels differ from now on: the others start their own lanes, a short dip while they fill.
            isDualMono = false
            lanes.drop(1).forEach(Lane::reset)
        }
        val activeLanes = if (isDualMono) 1 else channels
        for (channel in 0 until activeLanes) {
            for (frame in 0 until frameCount) channelBuffer[frame] = samples[frame * channels + channel]
            lanes[channel].feed(channelBuffer, frameCount)
        }
        for (frame in 0 until frameCount) {
            val wet = wetRamp.next()
            if (isDualMono) {
                val value = lanes[0].nextWet() * wet + lanes[0].nextDry() * (1 - wet)
                for (channel in 0 until channels) samples[frame * channels + channel] = value
            } else {
                lanes.forEachIndexed { channel, lane ->
                    samples[frame * channels + channel] = lane.nextWet() * wet + lane.nextDry() * (1 - wet)
                }
            }
        }
    }

    /** True while every channel has carried the same samples since the stream started. */
    private var isDualMono = channels > 1

    private fun channelsAreEqual(samples: FloatArray, frameCount: Int): Boolean {
        for (frame in 0 until frameCount) {
            val first = samples[frame * channels]
            for (channel in 1 until channels) if (samples[frame * channels + channel] != first) return false
        }
        return true
    }

    /** A seek: the next audio is a new stream. */
    fun reset() {
        lanes.forEach(Lane::reset)
        isDualMono = channels > 1
    }

    override fun close() = lanes.forEach { it.denoiser.close() }

    /** One channel: input → (48 kHz) → frames → denoiser → (back) → queue of denoised samples; plus the dry delay. */
    private inner class Lane(val denoiser: FrameDenoiser) {
        private val frameSize = denoiser.frameSize
        private val up = Resampler(sampleRate, DENOISER_RATE, 1, SPEECH_PASSBAND).takeUnless { sampleRate == DENOISER_RATE }
        private val down = Resampler(DENOISER_RATE, sampleRate, 1, SPEECH_PASSBAND).takeUnless { sampleRate == DENOISER_RATE }
        private val frame = FloatArray(frameSize)
        private val denoised = FloatArray(frameSize)
        private var frameFill = 0

        /** Zeros queued before the first denoised sample: covers frame filling and the resamplers' look-ahead. */
        private val prefill = ceilDiv(frameSize * sampleRate, DENOISER_RATE) +
            (up?.halfWidth ?: 0) + ceilDiv((down?.halfWidth ?: 0) * sampleRate, DENOISER_RATE) + if (up == null) 0 else 1

        /** The denoiser's own one-frame delay, in this lane's rate. */
        private val denoiserDelay = Math.round(frameSize.toDouble() * sampleRate / DENOISER_RATE).toInt()

        val latencyFrames = prefill + denoiserDelay

        private val wetQueue = FloatQueue(latencyFrames * 4 + frameSize * 2)
        private val dryQueue = FloatQueue(latencyFrames * 4 + frameSize * 2)

        init {
            reset()
        }

        fun reset() {
            up?.reset()
            down?.reset()
            frameFill = 0
            wetQueue.clear()
            dryQueue.clear()
            repeat(prefill) { wetQueue.add(0f) }
            repeat(latencyFrames) { dryQueue.add(0f) }
        }

        fun feed(input: FloatArray, count: Int) {
            for (index in 0 until count) dryQueue.add(input[index])
            if (up == null) {
                collect(input, count)
            } else {
                val produced = up.process(input, 0, count)
                collect(up.output, produced)
            }
        }

        fun nextWet(): Float = if (wetQueue.isEmpty) 0f.also { underflowCount++ } else wetQueue.remove()

        fun nextDry(): Float = if (dryQueue.isEmpty) 0f else dryQueue.remove()

        private fun collect(samples48k: FloatArray, count: Int) {
            var index = 0
            while (index < count) {
                val take = minOf(frameSize - frameFill, count - index)
                samples48k.copyInto(frame, frameFill, index, index + take)
                frameFill += take
                index += take
                if (frameFill == frameSize) {
                    denoiser.process(frame, denoised)
                    frameFill = 0
                    emit()
                }
            }
        }

        private fun emit() {
            if (down == null) {
                for (sample in denoised) wetQueue.add(sample)
            } else {
                val produced = down.process(denoised, 0, frameSize)
                for (index in 0 until produced) wetQueue.add(down.output[index])
            }
        }
    }

    private fun ceilDiv(numerator: Int, denominator: Int) = (numerator + denominator - 1) / denominator

    companion object {
        const val DENOISER_RATE = 48_000

        /** Passband to 80 % of the lower Nyquist (17.6 kHz at 44.1 kHz): plenty for speech, half the taps of 90 %. */
        const val SPEECH_PASSBAND = 0.8
    }
}

/** A growable ring buffer of floats; no allocation once it has its working size. */
internal class FloatQueue(initialCapacity: Int) {
    private var items = FloatArray(initialCapacity.coerceAtLeast(1))
    private var head = 0
    private var size = 0

    val isEmpty: Boolean get() = size == 0

    fun add(value: Float) {
        if (size == items.size) grow()
        items[(head + size) % items.size] = value
        size++
    }

    fun remove(): Float {
        val value = items[head]
        head = (head + 1) % items.size
        size--
        return value
    }

    fun clear() {
        head = 0
        size = 0
    }

    private fun grow() {
        val larger = FloatArray(items.size * 2)
        for (index in 0 until size) larger[index] = items[(head + index) % items.size]
        items = larger
        head = 0
    }
}
