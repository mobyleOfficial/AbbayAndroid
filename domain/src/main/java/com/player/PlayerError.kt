package com.player

/** Playback problems reported by the playback service. */
sealed interface PlayerError {
    /** A track could not be decoded; playback continued with the next track. */
    data class TrackSkipped(val fileName: String) : PlayerError

    /** No track of the book could be played; playback stopped. */
    data object BookUnplayable : PlayerError

    /** A file is missing or the folder permission was revoked; playback stopped. */
    data object StorageUnavailable : PlayerError

    /** The device could not keep up with noise reduction (audio underruns); it was turned off for the book. */
    data object NoiseReductionTooSlow : PlayerError
}
