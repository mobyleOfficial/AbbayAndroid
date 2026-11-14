package com.effects

enum class SilenceTrimLevel { OFF, GENTLE, MODERATE, STRONG }

@Suppress("MagicNumber")
enum class NoiseReductionLevel(val wet: Float) {
    OFF(0f),
    LOW(0.5f),
    MEDIUM(0.75f),
    HIGH(1f)
}

enum class BoostLevel { OFF, LOW, MEDIUM, HIGH }

data class AudioEffects(
    val silenceTrim: SilenceTrimLevel = SilenceTrimLevel.OFF,
    val boost: BoostLevel = BoostLevel.OFF,
    val equalizerGains: List<Int> = FLAT,
    val mono: Boolean = false,
    val balance: Float = 0f,
    val pitchSemitones: Float = 0f,
    val silenceSavedMs: Long = 0L,
    val noiseReduction: NoiseReductionLevel = NoiseReductionLevel.OFF,
) {
    val isEqualizerFlat: Boolean get() = equalizerGains.all { it == 0 }

    companion object {
        const val BAND_COUNT = 10
        const val MAXIMUM_BAND_DB = 12
        const val MAXIMUM_PITCH_SEMITONES = 6f
        const val PITCH_STEP = 0.5f
        val FLAT: List<Int> = List(BAND_COUNT) { 0 }

        fun pitchFactor(semitones: Float): Float = Math.pow(2.0, semitones / SEMITONES_PER_OCTAVE).toFloat()

        private const val SEMITONES_PER_OCTAVE = 12.0
    }
}
