package com.usecase

import com.repository.BooksRepository
import javax.inject.Inject

class SaveBookFolderPath @Inject constructor(
    private val repository: BooksRepository
) {
    operator fun invoke(path: String) = repository.saveBookFolderPath(path)
}