package com.mobyle.abbay.data.effects

import com.effects.AudioEffects
import com.effects.BoostLevel
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.data.database.entity.ListeningStateEntity

/** Effect columns of `listening_state` ↔ [AudioEffects]. The equalizer is a JSON array of 10 integers, null when flat. */
internal object EffectsMapper {
    fun toEffects(state: ListeningStateEntity): AudioEffects = AudioEffects(
        silenceTrim = SilenceTrimLevel.entries.firstOrNull { it.name == state.silenceTrimLevel } ?: SilenceTrimLevel.OFF,
        boost = BoostLevel.entries.firstOrNull { it.name == state.boostLevel } ?: BoostLevel.OFF,
        equalizerGains = gainsOf(state.equalizerJson),
        mono = state.mono,
        balance = state.balance,
        pitchSemitones = state.pitchSemitones,
        silenceSavedMs = state.silenceSavedMs,
        noiseReduction = NoiseReductionLevel.entries.firstOrNull { it.name == state.noiseReductionLevel } ?: NoiseReductionLevel.OFF,
    )

    fun withEffects(state: ListeningStateEntity, effects: AudioEffects): ListeningStateEntity = state.copy(
        silenceTrimLevel = effects.silenceTrim.name,
        boostLevel = effects.boost.name,
        equalizerJson = jsonOf(effects.equalizerGains),
        mono = effects.mono,
        balance = effects.balance,
        pitchSemitones = effects.pitchSemitones,
        noiseReductionLevel = effects.noiseReduction.name,
    )

    fun jsonOf(gains: List<Int>): String? = gains.takeUnless { list -> list.all { it == 0 } }?.joinToString(",", "[", "]")

    fun gainsOf(json: String?): List<Int> {
        val values = json?.trim()?.removePrefix("[")?.removeSuffix("]")?.split(',')?.mapNotNull { it.trim().toDoubleOrNull()?.toInt() }
        return List(AudioEffects.BAND_COUNT) { band -> values?.getOrNull(band) ?: 0 }
    }
}
