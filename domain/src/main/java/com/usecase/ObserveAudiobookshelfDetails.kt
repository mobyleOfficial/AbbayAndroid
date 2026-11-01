package com.usecase

import com.model.LibriVoxBook
import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class ObserveAudiobookshelfDetails @Inject constructor(private val repository: AudiobookshelfRepository) {
    operator fun invoke(book: LibriVoxBook) = repository.details(book)
}
