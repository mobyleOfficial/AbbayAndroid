package com.usecase

import com.repository.PlaybackOptionsRepository
import javax.inject.Inject

class ObservePlaybackOptions @Inject constructor(private val repository: PlaybackOptionsRepository) {
    operator fun invoke(bookId: String) = repository.observeOptions(bookId)
}
