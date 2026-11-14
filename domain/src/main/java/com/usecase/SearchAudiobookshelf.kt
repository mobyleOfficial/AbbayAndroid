package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class SearchAudiobookshelf @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, query: String) = repository.search(libraryId, query)
}
