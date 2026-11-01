package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class ObserveAudiobookshelfAccount @Inject constructor(private val repository: AudiobookshelfRepository) {
    operator fun invoke() = repository.observeAccount()
}
