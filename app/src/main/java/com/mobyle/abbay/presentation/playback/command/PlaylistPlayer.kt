package com.mobyle.abbay.presentation.playback.command

/** The part of the media player the commands need; adapted from Media3 in the service, faked in tests. */
interface PlaylistPlayer {
    val currentPosition: TrackPosition
    val trackDurationsMs: List<Long>

    /** Book-absolute chapter starts; empty when the book has no chapter list. */
    val chapterStartsMs: List<Long>
    val isPlaying: Boolean

    fun seekTo(position: TrackPosition)

    fun play()

    fun pause()
}
