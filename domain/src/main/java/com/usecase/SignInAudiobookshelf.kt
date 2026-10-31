package com.usecase

import com.repository.AudiobookshelfRepository
import javax.inject.Inject

class SignInAudiobookshelf @Inject constructor(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(serverUrl: String, username: String, password: String) =
        repository.signIn(serverUrl, username, password)
}
