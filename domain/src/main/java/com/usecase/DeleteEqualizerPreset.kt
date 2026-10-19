package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class DeleteEqualizerPreset @Inject constructor(private val repository: AudioEffectsRepository) {
    suspend operator fun invoke(presetId: Long) = repository.deletePreset(presetId)
}
