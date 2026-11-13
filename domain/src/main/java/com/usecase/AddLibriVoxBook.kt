package com.usecase

import com.model.DataResult
import com.model.LibriVoxBook
import com.readalong.ReadAlongLanguage
import com.repository.LibriVoxRepository
import com.repository.ReadAlongRepository
import javax.inject.Inject

class AddLibriVoxBook @Inject constructor(private val repository: LibriVoxRepository, private val readAlong: ReadAlongRepository) {
    /** The catalog names the book's language: read-along then reads it in that language, without detecting it. */
    suspend operator fun invoke(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean = true): DataResult<String> {
        val result = repository.add(book, allowsCellular, download)
        val code = ReadAlongLanguage.languageCode(book.language)
        if (result is DataResult.Success && code != null && readAlong.language(result.value) == null) {
            readAlong.setLanguage(result.value, code)
        }
        return result
    }
}
