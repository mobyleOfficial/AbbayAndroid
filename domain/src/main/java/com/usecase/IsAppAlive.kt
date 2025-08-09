package com.usecase

import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class IsAppAlive @Inject constructor(
    private val repository: BooksRepository
) {
    operator fun invoke() = repository.isAppAlive()
}