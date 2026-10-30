package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.effects.AudioEffects
import com.effects.BoostLevel
import com.mobyle.abbay.infra.dsp.ChannelMixer
import com.mobyle.abbay.infra.dsp.GraphicEqualizer
import com.mobyle.abbay.infra.dsp.LoudnessCompressor
import com.mobyle.abbay.infra.dsp.PeakLimiter

@OptIn(UnstableApi::class)
class EqualizerProcessor(private val state: EffectsState) : FloatPcmProcessor() {
    private var equalizer: GraphicEqualizer? = null
    private var appliedGains: List<Int>? = null

    override fun onFormat() {
        equalizer = GraphicEqualizer(sampleRate, channelCount)
        appliedGains = null
        applySettings(immediately = true)
    }

    override fun isBypassed(): Boolean {
        applySettings(immediately = false)
        return equalizer?.isFlat != false
    }

    override fun process(samples: FloatArray, frameCount: Int) {
        equalizer?.process(samples, frameCount)
    }

    override fun onFlush() {
        equalizer?.reset()
    }

    private fun applySettings(immediately: Boolean) {
        val gains = state.current.equalizerGains
        if (gains == appliedGains) return
        appliedGains = gains
        equalizer?.setGains(FloatArray(GraphicEqualizer.BAND_COUNT) { gains.getOrElse(it) { 0 }.toFloat() }, immediately)
    }
}

@OptIn(UnstableApi::class)
class ChannelProcessor(private val state: EffectsState) : FloatPcmProcessor() {
    private var mixer: ChannelMixer? = null
    private var applied: Pair<Boolean, Float>? = null

    override fun onFormat() {
        mixer = ChannelMixer(channelCount, sampleRate)
        applied = null
        applySettings(immediately = true)
    }

    override fun isBypassed(): Boolean {
        applySettings(immediately = false)
        return channelCount < 2 || mixer?.isIdentity != false
    }

    override fun process(samples: FloatArray, frameCount: Int) {
        mixer?.process(samples, frameCount)
    }

    private fun applySettings(immediately: Boolean) {
        val effects = state.current
        val settings = effects.mono to effects.balance
        if (settings == applied) return
        applied = settings
        mixer?.set(effects.mono, effects.balance, immediately)
    }
}

@OptIn(UnstableApi::class)
class LoudnessProcessor(private val state: EffectsState) : FloatPcmProcessor() {
    private var compressor: LoudnessCompressor? = null
    private var limiter: PeakLimiter? = null
    private var appliedBoost: BoostLevel? = null
    private var direct = FloatArray(0)
    private var isLimiting = false

    private var fadeFramesLeft = 0
    private var fadeFrames = 0

    override fun onFormat() {
        compressor = LoudnessCompressor(channelCount, sampleRate)
        limiter = PeakLimiter(channelCount, sampleRate)
        appliedBoost = null
        fadeFrames = sampleRate * FADE_MS / MILLISECONDS_PER_SECOND
        isLimiting = false
        fadeFramesLeft = 0
        applySettings(immediately = true)
    }

    override fun isBypassed(): Boolean {
        applySettings(immediately = false)
        val wanted = needsLimiter(state.current) || compressor?.isOff == false
        if (wanted && !isLimiting) {
            limiter?.reset()
            isLimiting = true
            fadeFramesLeft = fadeFrames
        } else if (!wanted && isLimiting && fadeFramesLeft <= 0) {
            isLimiting = false
            fadeFramesLeft = -fadeFrames
        }
        return !isLimiting && fadeFramesLeft == 0
    }

    override fun process(samples: FloatArray, frameCount: Int) {
        val sampleCount = frameCount * channelCount
        if (direct.size < sampleCount) direct = FloatArray(sampleCount)
        samples.copyInto(direct, 0, 0, sampleCount)
        compressor?.takeUnless { it.isOff }?.process(samples, frameCount)
        limiter?.process(samples, frameCount)
        if (fadeFramesLeft != 0) crossfade(samples, frameCount)
    }

    private fun crossfade(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            val limitedWeight = when {
                fadeFramesLeft > 0 -> 1f - (fadeFramesLeft--).toFloat() / fadeFrames
                fadeFramesLeft < 0 -> -(fadeFramesLeft++).toFloat() / fadeFrames
                isLimiting -> 1f
                else -> 0f
            }
            for (channel in 0 until channelCount) {
                val index = frame * channelCount + channel
                samples[index] = samples[index] * limitedWeight + direct[index] * (1f - limitedWeight)
            }
        }
    }

    override fun onFlush() {
        compressor?.reset()
        limiter?.reset()
    }

    private fun applySettings(immediately: Boolean) {
        val boost = state.current.boost
        if (boost == appliedBoost) return
        appliedBoost = boost
        compressor?.set(EffectsState.boostSettingsOf(boost), immediately)
    }

    private fun needsLimiter(effects: AudioEffects): Boolean = effects.boost != BoostLevel.OFF || effects.equalizerGains.any { it > 0 }

    private companion object {
        const val FADE_MS = 10
        const val MILLISECONDS_PER_SECOND = 1_000
    }
}
