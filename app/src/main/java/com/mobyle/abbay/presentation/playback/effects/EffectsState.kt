package com.mobyle.abbay.presentation.playback.effects

import com.effects.AudioEffects
import com.effects.BoostLevel
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.infra.dsp.LoudnessCompressor
import com.mobyle.abbay.infra.dsp.SilenceTrimmer
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class EffectsState {
    private val effects = AtomicReference(AudioEffects())
    private val boundaries = AtomicReference(TrackBoundaries.NONE)

    private val bufferTrackTimeUs = AtomicLong(0L)
    private val savedSilenceUs = AtomicLong(0L)

    @Volatile
    var isNoiseReductionRunning = false

    var current: AudioEffects
        get() = effects.get()
        set(value) = effects.set(value)

    var trackBoundaries: TrackBoundaries
        get() = boundaries.get()
        set(value) = boundaries.set(value)

    var currentBufferTrackTimeUs: Long
        get() = bufferTrackTimeUs.get()
        set(value) = bufferTrackTimeUs.set(value)

    fun addSavedSilence(microseconds: Long) {
        savedSilenceUs.addAndGet(microseconds)
    }

    fun takeSavedSilenceMs(): Long {
        val microseconds = savedSilenceUs.get()
        val whole = microseconds / MICROSECONDS_PER_MILLISECOND
        savedSilenceUs.addAndGet(-whole * MICROSECONDS_PER_MILLISECOND)
        return whole
    }

    data class TrackBoundaries(val durationUs: Long, val chapterEdgesUs: List<Long>) {
        fun isProtected(startUs: Long, endUs: Long): Boolean {
            if (startUs < PROTECTION_US) return true
            if (durationUs > 0 && endUs > durationUs - PROTECTION_US) return true
            return chapterEdgesUs.any { edge -> endUs > edge - PROTECTION_US && startUs < edge + PROTECTION_US }
        }

        companion object {
            val NONE = TrackBoundaries(0L, emptyList())

            const val PROTECTION_US = 1_000_000L
        }
    }

    companion object {
        private const val MICROSECONDS_PER_MILLISECOND = 1_000L

        fun trimSettingsOf(level: SilenceTrimLevel): SilenceTrimmer.Settings? = when (level) {
            SilenceTrimLevel.OFF -> null
            SilenceTrimLevel.GENTLE -> SilenceTrimmer.GENTLE
            SilenceTrimLevel.MODERATE -> SilenceTrimmer.MODERATE
            SilenceTrimLevel.STRONG -> SilenceTrimmer.STRONG
        }

        fun boostSettingsOf(level: BoostLevel): LoudnessCompressor.Settings = when (level) {
            BoostLevel.OFF -> LoudnessCompressor.Settings.OFF
            BoostLevel.LOW -> LoudnessCompressor.Settings(thresholdDb = -24f, ratio = 2f, makeUpDb = 4f)
            BoostLevel.MEDIUM -> LoudnessCompressor.Settings(thresholdDb = -28f, ratio = 3f, makeUpDb = 8f)
            BoostLevel.HIGH -> LoudnessCompressor.Settings(thresholdDb = -32f, ratio = 4f, makeUpDb = 12f)
        }
    }
}
