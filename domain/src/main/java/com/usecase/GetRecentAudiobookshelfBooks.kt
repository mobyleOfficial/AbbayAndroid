package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class GetRecentAudiobookshelfBooks @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String) = repository.recentlyAdded(libraryId)
}
