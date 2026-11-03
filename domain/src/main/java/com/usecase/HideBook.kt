package com.usecase

import com.repository.LibraryRepository
import com.repository.ReadAlongRepository
import javax.inject.Inject

class HideBook @Inject constructor(private val repository: LibraryRepository, private val readAlong: ReadAlongRepository) {
    suspend operator fun invoke(bookId: String) {
        repository.hideBook(bookId)
        readAlong.delete(bookId)
        readAlong.setLanguage(bookId, null)
        readAlong.setSyncOffset(bookId, 0)
        readAlong.chooseEpub(bookId, null)
        readAlong.setIntroSeen(bookId, false)
    }
}
