package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.effects.NoiseReductionLevel
import com.mobyle.abbay.infra.dsp.FrameDenoiser
import com.mobyle.abbay.infra.dsp.NoiseReducer
import com.mobyle.abbay.infra.dsp.nativelib.RnnNoise

/**
 * RNNoise in the chain, first, before silence trimming. The reducer delays the
 * audio by about 20 ms ([NoiseReducer.latencyFrames]); output frames equal input frames, so the media position stays
 * right. Turning it on or off crossfades over 20 ms between the direct and the reduced signal; while it is off the
 * reducer does not run and the input passes bit for bit.
 */
@OptIn(UnstableApi::class)
class NoiseReductionProcessor(private val state: EffectsState) : FloatPcmProcessor() {
    private var reducer: NoiseReducer? = null
    private var direct = FloatArray(0)
    private var isReducing = false

    /** Frames left in the current crossfade; positive while fading in, negative while fading out. */
    private var fadeFramesLeft = 0
    private var fadeFrames = 0

    override fun onFormat() {
        reducer?.close()
        reducer = null
        isReducing = false
        fadeFramesLeft = 0
        fadeFrames = sampleRate * FADE_MS / MILLISECONDS_PER_SECOND
    }

    override fun isBypassed(): Boolean {
        val level = state.current.noiseReduction.takeIf { RnnNoise.isAvailable } ?: NoiseReductionLevel.OFF
        val wanted = level != NoiseReductionLevel.OFF
        if (wanted && !isReducing) {
            val current = reducer ?: NoiseReducer(sampleRate, channelCount, ::RnnNoiseDenoiser).also { reducer = it }
            current.reset()
            current.setWet(level.wet, immediately = true)
            isReducing = true
            fadeFramesLeft = fadeFrames
        } else if (wanted) {
            reducer?.setWet(level.wet)
        } else if (isReducing && fadeFramesLeft <= 0) {
            isReducing = false
            fadeFramesLeft = -fadeFrames
        }
        state.isNoiseReductionRunning = isReducing || fadeFramesLeft != 0
        return !state.isNoiseReductionRunning
    }

    override fun process(samples: FloatArray, frameCount: Int) {
        val current = reducer ?: return
        val sampleCount = frameCount * channelCount
        if (direct.size < sampleCount) direct = FloatArray(sampleCount)
        samples.copyInto(direct, 0, 0, sampleCount)
        current.process(samples, frameCount)
        if (fadeFramesLeft != 0) crossfade(samples, frameCount)
    }

    /** Per frame, weight 1 is the reduced signal, 0 the direct one. */
    private fun crossfade(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            val reducedWeight = when {
                fadeFramesLeft > 0 -> 1f - (fadeFramesLeft--).toFloat() / fadeFrames
                fadeFramesLeft < 0 -> -(fadeFramesLeft++).toFloat() / fadeFrames
                isReducing -> 1f
                else -> 0f
            }
            for (channel in 0 until channelCount) {
                val index = frame * channelCount + channel
                samples[index] = samples[index] * reducedWeight + direct[index] * (1f - reducedWeight)
            }
        }
    }

    override fun onFlush() {
        reducer?.reset()
    }

    override fun onReset() {
        reducer?.close()
        reducer = null
        isReducing = false
        state.isNoiseReductionRunning = false
    }

    /** One RNNoise state per channel. */
    private class RnnNoiseDenoiser : FrameDenoiser {
        private val rnnNoise = RnnNoise()
        override val frameSize: Int = RnnNoise.FRAME_SIZE

        override fun process(input: FloatArray, output: FloatArray) {
            rnnNoise.process(input, output)
        }

        override fun close() = rnnNoise.close()
    }

    private companion object {
        const val FADE_MS = 20
        const val MILLISECONDS_PER_SECOND = 1_000
    }
}
