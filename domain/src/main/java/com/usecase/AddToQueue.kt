package com.usecase

import com.repository.QueueRepository
import javax.inject.Inject

class AddToQueue @Inject constructor(private val repository: QueueRepository) {
    suspend operator fun invoke(bookId: String, playNext: Boolean) = repository.add(bookId, playNext)
}
