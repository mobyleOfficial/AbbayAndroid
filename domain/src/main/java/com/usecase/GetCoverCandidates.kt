package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class GetCoverCandidates @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String) = repository.coverCandidates(bookId)
}
