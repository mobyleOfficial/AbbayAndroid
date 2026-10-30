package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class SaveEqualizerPreset @Inject constructor(private val repository: AudioEffectsRepository) {
    suspend operator fun invoke(name: String, gains: List<Int>) = repository.savePreset(name.trim(), gains)
}
