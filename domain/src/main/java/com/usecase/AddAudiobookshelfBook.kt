package com.usecase

import com.model.LibriVoxBook
import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class AddAudiobookshelfBook @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean = true) =
        repository.add(book, allowsCellular, download)
}
