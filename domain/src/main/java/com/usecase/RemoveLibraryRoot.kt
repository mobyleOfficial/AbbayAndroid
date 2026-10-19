package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class RemoveLibraryRoot @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(rootId: Long) = repository.removeRoot(rootId)
}
