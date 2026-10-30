package com.usecase

import com.effects.AudioEffects
import com.repository.AudioEffectsRepository
import javax.inject.Inject

class UpdateAudioEffects @Inject constructor(private val repository: AudioEffectsRepository) {
    suspend operator fun invoke(bookId: String, effects: AudioEffects) = repository.updateEffects(
        bookId,
        effects.copy(
            equalizerGains = List(AudioEffects.BAND_COUNT) { band ->
                effects.equalizerGains.getOrElse(band) { 0 }.coerceIn(-AudioEffects.MAXIMUM_BAND_DB, AudioEffects.MAXIMUM_BAND_DB)
            },
            balance = effects.balance.coerceIn(-1f, 1f),
            pitchSemitones = effects.pitchSemitones.coerceIn(-AudioEffects.MAXIMUM_PITCH_SEMITONES, AudioEffects.MAXIMUM_PITCH_SEMITONES),
        ),
    )
}
