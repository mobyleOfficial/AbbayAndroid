package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class RememberOpenedDocument @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, documentUri: String) = repository.rememberOpenedDocument(bookId, documentUri)
}
