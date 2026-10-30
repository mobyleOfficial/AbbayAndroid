package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class ObserveLibriVoxDownloads @Inject constructor(private val repository: LibriVoxRepository) {
    operator fun invoke() = repository.observeDownloads()
}
