package com.player

/** Coarse state of the media engine, independent of the playback library. */
enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}
