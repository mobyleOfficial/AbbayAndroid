package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class GetAudiobookshelfSeriesBooks @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, seriesId: String) = repository.seriesBooks(libraryId, seriesId)
}
