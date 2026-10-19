package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class AddSingleFileBook @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(documentUri: String) = repository.addSingleFile(documentUri)
}
