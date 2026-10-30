package com.player

/** Repeat modes of a book. */
enum class RepeatMode { OFF, CHAPTER, TRACK, BOOK, AB }

/**
 * Per-book playback options: repeat, the A–B loop (book positions) and the
 * seconds skipped at the start and end of every file.
 */
data class PlaybackOptions(
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val loopAMs: Long? = null,
    val loopBMs: Long? = null,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
) {
    /** The A–B loop only runs when both ends are set and A is before B. */
    val hasLoop: Boolean get() = repeatMode == RepeatMode.AB && loopAMs != null && loopBMs != null && loopAMs < loopBMs

    companion object {
        const val MAXIMUM_SKIP_SECONDS = 600
    }
}
