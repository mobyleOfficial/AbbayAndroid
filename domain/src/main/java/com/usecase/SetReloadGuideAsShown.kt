package com.usecase

import com.repository.BooksRepository
import javax.inject.Inject

class SetReloadGuideAsShown @Inject constructor(
    private val repository: BooksRepository
) {
    operator fun invoke() = repository.setReloadGuideAsShown()
}