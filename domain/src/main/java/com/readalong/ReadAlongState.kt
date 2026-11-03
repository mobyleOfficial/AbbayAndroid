package com.readalong

/** What the read-along text says. */
sealed interface ReadAlongState {
    /** Finding the language, loading, or no line yet at the shown time. */
    data object Preparing : ReadAlongState

    /** Downloading the speech model for [locale]; [progress] in 0..1, null while unknown. */
    data class DownloadingModel(val locale: String, val progress: Float?) : ReadAlongState

    /** No speech model exists for [locale]. */
    data class Unsupported(val locale: String) : ReadAlongState

    /** A streamed book's audio at the shown time isn't downloaded. */
    data object NeedsDownload : ReadAlongState

    /** The book's file isn't readable on the device. */
    data object FileMissing : ReadAlongState

    data object Failed : ReadAlongState

    /** The shown time is covered: by a line, or by silence. */
    data object Ready : ReadAlongState
}
