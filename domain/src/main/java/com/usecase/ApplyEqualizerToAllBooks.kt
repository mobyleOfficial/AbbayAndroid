package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class ApplyEqualizerToAllBooks @Inject constructor(private val repository: AudioEffectsRepository) {
    suspend operator fun invoke(gains: List<Int>) = repository.applyEqualizerToAllBooks(gains)
}
