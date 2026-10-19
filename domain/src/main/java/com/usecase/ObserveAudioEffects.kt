package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class ObserveAudioEffects @Inject constructor(private val repository: AudioEffectsRepository) {
    operator fun invoke(bookId: String) = repository.observeEffects(bookId)
}
