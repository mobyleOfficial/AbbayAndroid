package com.usecase

import com.player.PlaybackOptions
import com.repository.PlaybackOptionsRepository
import javax.inject.Inject

/** Stores a book's repeat and skip options, with the skips clamped to 0–600 s. */
class UpdatePlaybackOptions @Inject constructor(private val repository: PlaybackOptionsRepository) {
    suspend operator fun invoke(bookId: String, options: PlaybackOptions) = repository.updateOptions(
        bookId,
        options.copy(
            skipIntroSeconds = options.skipIntroSeconds.coerceIn(0, PlaybackOptions.MAXIMUM_SKIP_SECONDS),
            skipOutroSeconds = options.skipOutroSeconds.coerceIn(0, PlaybackOptions.MAXIMUM_SKIP_SECONDS),
        ),
    )
}
