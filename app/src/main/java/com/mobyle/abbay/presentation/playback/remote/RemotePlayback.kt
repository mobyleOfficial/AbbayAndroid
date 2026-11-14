package com.mobyle.abbay.presentation.playback.remote

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

interface RemotePlaybackBridge {
    fun attach(host: RemotePlaybackHost)

    fun release()
}

interface RemotePlaybackHost {
    fun snapshot(): LocalPlaybackSnapshot?

    fun onRemoteStarted(player: Player)

    fun onRemoteEnded(trackIndex: Int, positionMs: Long, playWhenReady: Boolean)

    fun saveRemotePosition()

    fun startProcessedStream(stream: ProcessedAudioStream): Boolean

    fun stopProcessedStream(playWhenReady: Boolean)
}

class LocalPlaybackSnapshot(
    val items: List<MediaItem>,
    val trackIndex: Int,
    val positionMs: Long,
    val speed: Float,
    val playWhenReady: Boolean,
    val processedAudio: Boolean = false,
)
