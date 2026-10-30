package com.usecase

import com.repository.QueueRepository
import javax.inject.Inject

class RemoveFromQueue @Inject constructor(private val repository: QueueRepository) {
    suspend operator fun invoke(bookId: String) = repository.remove(bookId)
}
