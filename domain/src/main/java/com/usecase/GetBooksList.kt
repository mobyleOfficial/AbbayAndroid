package com.usecase

import com.repository.BooksRepository
import javax.inject.Inject

class GetBooksList @Inject constructor(private val repository: BooksRepository) {
    suspend operator fun invoke() = repository.getBooksList()
}