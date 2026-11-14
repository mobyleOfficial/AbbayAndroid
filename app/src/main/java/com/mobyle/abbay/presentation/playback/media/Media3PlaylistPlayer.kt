package com.mobyle.abbay.presentation.playback.media

import androidx.media3.common.C
import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.command.PlaylistPlayer
import com.mobyle.abbay.presentation.playback.command.TrackPosition

class Media3PlaylistPlayer(
    private val player: Player,
    private val libraryItems: List<androidx.media3.common.MediaItem>? = null,
) : PlaylistPlayer {
    override val currentPosition: TrackPosition
        get() = TrackPosition(player.currentMediaItemIndex, player.currentPosition.coerceAtLeast(0L))

    override val trackDurationsMs: List<Long>
        get() = (0 until player.mediaItemCount).map { index ->
            val libraryDuration = BookMediaItems.trackDurationOf(libraryItems?.getOrNull(index) ?: player.getMediaItemAt(index))
            if (libraryDuration > 0L) libraryDuration else playerDurationOf(index)
        }

    override val chapterStartsMs: List<Long>
        get() = (
            libraryItems?.getOrNull(
                player.currentMediaItemIndex
            ) ?: player.currentMediaItem
            )?.let(BookMediaItems::chapterStartsOf).orEmpty()

    override val isPlaying: Boolean get() = player.isPlaying

    override fun seekTo(position: TrackPosition) = player.seekTo(position.trackIndex, position.positionMs)

    override fun play() = player.play()

    override fun pause() = player.pause()

    private fun playerDurationOf(index: Int): Long {
        val timeline = player.currentTimeline
        if (index >= timeline.windowCount) return 0L
        val durationMs = timeline.getWindow(index, androidx.media3.common.Timeline.Window()).durationMs
        return if (durationMs == C.TIME_UNSET) 0L else durationMs
    }
}
