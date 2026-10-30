package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class RequestFullScan @Inject constructor(private val repository: LibraryRepository) {
    operator fun invoke() = repository.requestFullScan()
}
