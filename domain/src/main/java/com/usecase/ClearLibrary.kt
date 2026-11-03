package com.usecase

import com.repository.LibraryRepository
import com.repository.ReadAlongRepository
import javax.inject.Inject

class ClearLibrary @Inject constructor(private val repository: LibraryRepository, private val readAlong: ReadAlongRepository) {
    suspend operator fun invoke() {
        repository.clearLibrary()
        readAlong.deleteAll()
        readAlong.forgetBooks()
    }
}
