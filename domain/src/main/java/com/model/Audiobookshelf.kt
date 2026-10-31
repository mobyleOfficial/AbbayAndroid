package com.model

/** The Audiobookshelf server the user signed in to; [isSignedIn] is false once its session ended, to sign in again. */
data class AudiobookshelfAccount(val serverUrl: String, val username: String, val isSignedIn: Boolean)

data class AudiobookshelfLibrary(val id: String, val name: String)

enum class AudiobookshelfSignInError {
    INVALID_ADDRESS,
    UNREACHABLE,
    NOT_A_SERVER,
    WRONG_CREDENTIALS,
}

sealed interface AudiobookshelfSignIn {
    data class Success(val account: AudiobookshelfAccount) : AudiobookshelfSignIn

    data class Failure(val error: AudiobookshelfSignInError) : AudiobookshelfSignIn
}
