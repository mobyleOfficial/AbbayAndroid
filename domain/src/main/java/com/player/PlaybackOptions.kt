package com.player

enum class RepeatMode { OFF, CHAPTER, TRACK, BOOK, AB }

data class PlaybackOptions(
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val loopAMs: Long? = null,
    val loopBMs: Long? = null,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
) {
    val hasLoop: Boolean get() = repeatMode == RepeatMode.AB && loopAMs != null && loopBMs != null && loopAMs < loopBMs

    companion object {
        const val MAXIMUM_SKIP_SECONDS = 600
    }
}
