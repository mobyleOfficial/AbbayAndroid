package com.mobyle.abbay.presentation.playback.remote

import com.mobyle.abbay.presentation.playback.command.PlaylistPlayer
import com.mobyle.abbay.presentation.playback.command.TrackPosition

class SwitchablePlaylistPlayer(var current: PlaylistPlayer) : PlaylistPlayer {
    override val currentPosition: TrackPosition get() = current.currentPosition
    override val trackDurationsMs: List<Long> get() = current.trackDurationsMs
    override val chapterStartsMs: List<Long> get() = current.chapterStartsMs
    override val isPlaying: Boolean get() = current.isPlaying

    override fun seekTo(position: TrackPosition) = current.seekTo(position)

    override fun play() = current.play()

    override fun pause() = current.pause()
}
