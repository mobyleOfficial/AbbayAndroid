package com.mobyle.abbay.presentation.playback.remote

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * Remote playback (Cast phase 1) plugged into the playback service. The
 * implementation lives in `:cast` (optional binding: the service works without it).
 */
interface RemotePlaybackBridge {
    fun attach(host: RemotePlaybackHost)

    fun release()
}

/** What the playback service lends to remote playback. */
interface RemotePlaybackHost {
    /** The book loaded now, or null when nothing is. */
    fun snapshot(): LocalPlaybackSnapshot?

    /** Remote playback took over [player]: the session, the notification and the app's controls now drive it. */
    fun onRemoteStarted(player: Player)

    /** Remote playback ended at [trackIndex] / [positionMs]: local playback continues there. */
    fun onRemoteEnded(trackIndex: Int, positionMs: Long, playWhenReady: Boolean)

    /** Saves the remote player's position like a local one. */
    fun saveRemotePosition()

    /**
     * Cast phase 2: local playback goes on — with its effects, session, notification, sleep timer, history and
     * statistics — but its sound goes to [stream] instead of the device. False when nothing is loaded.
     */
    fun startProcessedStream(stream: ProcessedAudioStream): Boolean

    /** The sound comes back to the device, at the receiver's position; playback continues if [playWhenReady]. */
    fun stopProcessedStream(playWhenReady: Boolean)
}

class LocalPlaybackSnapshot(
    val items: List<MediaItem>,
    val trackIndex: Int,
    val positionMs: Long,
    val speed: Float,
    val playWhenReady: Boolean,
    /** "Apply audio effects when casting": stream the processed audio instead of the files. */
    val processedAudio: Boolean = false,
)
