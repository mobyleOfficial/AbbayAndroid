package com.usecase

import com.repository.ListeningRepository
import javax.inject.Inject

class ObserveBookmarks @Inject constructor(private val repository: ListeningRepository) {
    operator fun invoke(bookId: String) = repository.observeBookmarks(bookId)
}
