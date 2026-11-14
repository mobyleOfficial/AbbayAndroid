package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class SearchLibriVox @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke(query: String) = repository.search(query)
}
