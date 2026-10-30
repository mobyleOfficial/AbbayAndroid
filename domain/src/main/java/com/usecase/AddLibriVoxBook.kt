package com.usecase

import com.model.LibriVoxBook
import com.repository.LibriVoxRepository
import javax.inject.Inject

class AddLibriVoxBook @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke(book: LibriVoxBook, allowsCellular: Boolean) = repository.add(book, allowsCellular)
}
