package com.mobyle.abbay.presentation.playback.command

interface PlaylistPlayer {
    val currentPosition: TrackPosition
    val trackDurationsMs: List<Long>

    val chapterStartsMs: List<Long>
    val isPlaying: Boolean

    fun seekTo(position: TrackPosition)

    fun play()

    fun pause()
}
