package com.usecase

import com.repository.ReadAlongRepository
import javax.inject.Inject

class GetReadAlongAudioSource @Inject constructor(private val repository: ReadAlongRepository) {
    suspend operator fun invoke(bookId: String) = repository.audioSource(bookId)
}
