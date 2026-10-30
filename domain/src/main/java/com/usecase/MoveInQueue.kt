package com.usecase

import com.repository.QueueRepository
import javax.inject.Inject

class MoveInQueue @Inject constructor(private val repository: QueueRepository) {
    suspend operator fun invoke(bookId: String, index: Int) = repository.move(bookId, index)
}
