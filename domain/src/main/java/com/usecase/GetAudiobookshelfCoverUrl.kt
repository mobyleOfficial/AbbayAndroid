package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class GetAudiobookshelfCoverUrl @Inject constructor(private val repository: AudiobookshelfRepository) {
    operator fun invoke(itemId: String) = repository.coverUrlOf(itemId)
}
