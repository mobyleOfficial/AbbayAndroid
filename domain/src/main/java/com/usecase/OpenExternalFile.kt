package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class OpenExternalFile @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(documentUri: String) = repository.describeExternalFile(documentUri)
}
