package com.model

/** The Audiobookshelf server the user signed in to; [isSignedIn] is false once its session ended, to sign in again. */
data class AudiobookshelfAccount(val serverUrl: String, val username: String, val isSignedIn: Boolean)

data class AudiobookshelfLibrary(val id: String, val name: String)

/** How a library's books are listed. */
enum class AudiobookshelfSort {
    RECENTLY_ADDED,
    TITLE,
    AUTHOR,
}

/** One page of a long list; [hasMore] when the server has more after it. */
data class AudiobookshelfPage<T>(val items: List<T>, val hasMore: Boolean)

/** A series of a library, shown with the cover of its first book. */
data class AudiobookshelfSeries(val id: String, val name: String, val bookCount: Int, val coverUrl: String)

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
