package com.usecase

import com.model.Book
import com.repository.LibraryRepository
import com.repository.QueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** The queued books in order; books no longer in the library are left out. */
class ObserveQueue @Inject constructor(private val queueRepository: QueueRepository, private val libraryRepository: LibraryRepository) {
    operator fun invoke(): Flow<List<Book>> = combine(queueRepository.observeQueue(), libraryRepository.observeBooks()) { ids, books ->
        val byId = books.associateBy { it.id }
        ids.mapNotNull(byId::get)
    }
}
