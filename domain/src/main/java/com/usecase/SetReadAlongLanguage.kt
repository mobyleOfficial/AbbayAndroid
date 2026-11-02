package com.usecase

import com.readalong.ReadAlongLanguage
import com.repository.ReadAlongRepository
import javax.inject.Inject

/** Keeps the language for the book and drops its text, which was made in the previous one. */
class SetReadAlongLanguage @Inject constructor(private val repository: ReadAlongRepository) {
    suspend operator fun invoke(bookId: String, language: ReadAlongLanguage) {
        repository.setLanguage(bookId, language)
        repository.delete(bookId)
    }
}
