package com.usecase

import com.repository.CharacterRepository
import javax.inject.Inject

class ObserveCharacters @Inject constructor(private val repository: CharacterRepository) {
    operator fun invoke(bookId: String) = repository.observeCharacters(bookId)
}
