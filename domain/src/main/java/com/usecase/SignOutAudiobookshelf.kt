package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class SignOutAudiobookshelf @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke() = repository.signOut()
}
