package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class ObserveEqualizerPresets @Inject constructor(private val repository: AudioEffectsRepository) {
    operator fun invoke() = repository.observePresets()
}
