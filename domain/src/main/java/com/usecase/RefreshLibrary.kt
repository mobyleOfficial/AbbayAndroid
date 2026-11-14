package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class RefreshLibrary @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke() {
        repository.validateRoots()
        repository.quickScan()
    }
}
