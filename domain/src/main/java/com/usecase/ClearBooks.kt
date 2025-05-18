package com.usecase

import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class ClearBooks @Inject constructor(
    private val repository: BooksRepository
) {
    suspend operator fun invoke() {
        repository.clearBooks()
    }
}