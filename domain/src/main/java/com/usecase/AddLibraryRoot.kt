package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

/** Adds a root and starts a full scan of it. */
class AddLibraryRoot @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(treeUri: String) = repository.addRoot(treeUri).also { repository.requestFullScan() }
}
