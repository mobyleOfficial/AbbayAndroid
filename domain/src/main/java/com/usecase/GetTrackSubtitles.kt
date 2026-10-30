package com.usecase

import com.repository.SubtitleRepository
import javax.inject.Inject

class GetTrackSubtitles @Inject constructor(private val repository: SubtitleRepository) {
    suspend operator fun invoke(bookId: String, trackIndex: Int) = repository.subtitles(bookId, trackIndex)
}
