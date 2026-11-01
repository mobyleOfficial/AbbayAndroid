package com.usecase

import com.model.AudiobookshelfSort
import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class GetAudiobookshelfBooks @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, sort: AudiobookshelfSort, page: Int) = repository.books(libraryId, sort, page)
}
