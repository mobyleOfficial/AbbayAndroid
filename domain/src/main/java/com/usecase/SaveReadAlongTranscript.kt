package com.usecase

import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository
import javax.inject.Inject

class SaveReadAlongTranscript @Inject constructor(private val repository: ReadAlongRepository) {
    suspend operator fun invoke(bookId: String, transcript: ReadAlongTranscript) = repository.save(bookId, transcript)
}
