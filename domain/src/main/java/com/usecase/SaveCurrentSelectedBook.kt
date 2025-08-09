package com.usecase

import com.repository.BooksRepository
import javax.inject.Inject

class SaveCurrentSelectedBook @Inject constructor(
    private val repository: BooksRepository
) {
    operator fun invoke(id: String?, position: Int?) =
        repository.saveCurrentSelectedBook(id, position)
}