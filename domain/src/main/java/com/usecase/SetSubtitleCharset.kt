package com.usecase

import com.repository.SubtitleRepository
import javax.inject.Inject

class SetSubtitleCharset @Inject constructor(private val repository: SubtitleRepository) {
    suspend operator fun invoke(bookId: String, charset: String?) = repository.setCharset(bookId, charset)
}
