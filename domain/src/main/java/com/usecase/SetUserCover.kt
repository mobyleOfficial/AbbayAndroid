package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class SetUserCover @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, imageUri: String) = repository.setUserCover(bookId, imageUri)
}
