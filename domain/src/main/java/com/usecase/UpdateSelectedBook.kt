package com.usecase

import com.repository.BooksRepository
import javax.inject.Inject

class UpdateSelectedBook @Inject constructor(
    private val repository: BooksRepository
) {
    suspend operator fun invoke(position: Long, currentBookPosition: Int?) =
        repository.updateSelectedBook(position, currentBookPosition)
}