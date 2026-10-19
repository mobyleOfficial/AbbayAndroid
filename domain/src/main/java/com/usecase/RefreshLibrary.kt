package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

/** Validates folder permissions, then runs a quick scan (app start and library screen). */
class RefreshLibrary @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke() {
        repository.validateRoots()
        repository.quickScan()
    }
}
