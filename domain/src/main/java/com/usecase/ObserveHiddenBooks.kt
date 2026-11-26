package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class ObserveHiddenBooks @Inject constructor(private val repository: LibraryRepository) {
    operator fun invoke() = repository.observeHiddenBooks()
}
