package com.usecase

import com.repository.AudioEffectsRepository
import javax.inject.Inject

class AddSilenceSaved @Inject constructor(private val repository: AudioEffectsRepository) {
    suspend operator fun invoke(bookId: String, milliseconds: Long) {
        if (milliseconds > 0L) repository.addSilenceSaved(bookId, milliseconds)
    }
}
