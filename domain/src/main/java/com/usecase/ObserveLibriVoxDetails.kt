package com.usecase

import com.model.LibriVoxBook
import com.repository.LibriVoxRepository
import javax.inject.Inject

class ObserveLibriVoxDetails @Inject constructor(private val repository: LibriVoxRepository) {
    operator fun invoke(book: LibriVoxBook) = repository.details(book)
}
