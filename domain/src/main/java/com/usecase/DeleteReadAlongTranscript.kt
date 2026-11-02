package com.usecase

import com.repository.ReadAlongRepository
import javax.inject.Inject

/** The book left the library: its text goes with it. */
class DeleteReadAlongTranscript @Inject constructor(private val repository: ReadAlongRepository) {
    suspend operator fun invoke(bookId: String) = repository.delete(bookId)
}
