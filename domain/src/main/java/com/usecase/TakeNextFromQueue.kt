package com.usecase

import com.model.Book
import com.repository.LibraryRepository
import com.repository.QueueRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TakeNextFromQueue @Inject constructor(
    private val queueRepository: QueueRepository,
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(): Book? {
        val books = libraryRepository.observeBooks().first().associateBy { it.id }
        while (true) {
            val bookId = queueRepository.takeNext() ?: return null
            books[bookId]?.takeUnless { it.hasError }?.let { return it }
        }
    }
}
