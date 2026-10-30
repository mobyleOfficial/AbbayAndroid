package com.mobyle.abbay.presentation.cast

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
import com.mobyle.abbay.presentation.playback.remote.LocalPlaybackSnapshot
import com.mobyle.abbay.presentation.playback.remote.ProcessedAudioStream
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackBridge
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackHost
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject

@OptIn(UnstableApi::class)
class CastBridge @Inject constructor(@ApplicationContext private val context: Context) : RemotePlaybackBridge, SessionAvailabilityListener {
    private var host: RemotePlaybackHost? = null
    private var castPlayer: CastPlayer? = null
    private var server: LocalMediaServer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastIndex = 0
    private var lastPositionMs = 0L
    private var lastPlayWhenReady = false

    private val poll = object : Runnable {
        override fun run() {
            val player = castPlayer ?: return
            lastIndex = player.currentMediaItemIndex
            lastPositionMs = player.currentPosition
            lastPlayWhenReady = player.playWhenReady
            if (++pollCount % SAVE_EVERY_POLLS == 0) host?.saveRemotePosition()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }
    private var pollCount = 0

    private var stream: ProcessedAudioStream? = null
    private var isStreamPlaying = false
    private var streamMetadata: MediaMetadata = MediaMetadata.EMPTY

    private val streamPoll = object : Runnable {
        override fun run() {
            val player = castPlayer ?: return
            val current = stream ?: return
            if (player.currentMediaItem?.mediaId == liveMediaId(current.generation)) {
                current.updateReceiver(current.generation, player.currentPosition, player.isPlaying)
            }
            handler.postDelayed(this, STREAM_POLL_INTERVAL_MS)
        }
    }

    override fun attach(host: RemotePlaybackHost) {
        this.host = host
        val castContext = runCatching { CastContext.getSharedInstance(context) }.getOrNull() ?: return
        castPlayer = CastPlayer(castContext).also { player ->
            player.setSessionAvailabilityListener(this)
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) host.saveRemotePosition()
                }
            })
        }
    }

    override fun release() {
        handler.removeCallbacks(poll)
        handler.removeCallbacks(streamPoll)
        stream?.close()
        stream = null
        server?.stop()
        server = null
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        castPlayer = null
        host = null
    }

    override fun onCastSessionAvailable() {
        val player = castPlayer ?: return
        val currentHost = host ?: return
        val snapshot = currentHost.snapshot() ?: return
        val address = localAddress() ?: run {
            Log.w(LOG_TAG, "No local network address to serve the files from")
            return
        }
        if (snapshot.processedAudio) {
            startProcessedStream(player, currentHost, snapshot, address)
            return
        }
        val fileServer = LocalMediaServer(address, { index, offset -> open(snapshot, index, offset) }).also {
            it.start()
            server = it
        }
        val items = snapshot.items.mapIndexed { index, item -> remoteItem(item, fileServer.urlOf(index)) }
        player.setMediaItems(items, snapshot.trackIndex, snapshot.positionMs)
        player.playbackParameters = PlaybackParameters(snapshot.speed)
        player.playWhenReady = snapshot.playWhenReady
        player.prepare()
        lastIndex = snapshot.trackIndex
        lastPositionMs = snapshot.positionMs
        lastPlayWhenReady = snapshot.playWhenReady
        currentHost.onRemoteStarted(player)
        handler.post(poll)
    }

    override fun onCastSessionUnavailable() {
        handler.removeCallbacks(poll)
        handler.removeCallbacks(streamPoll)
        val endedStream = stream
        stream = null
        endedStream?.listener = null
        endedStream?.close()
        server?.stop()
        server = null
        if (endedStream != null) {
            host?.stopProcessedStream(isStreamPlaying)
        } else {
            host?.onRemoteEnded(lastIndex, lastPositionMs, lastPlayWhenReady)
        }
    }

    private fun startProcessedStream(
        player: CastPlayer,
        currentHost: RemotePlaybackHost,
        snapshot: LocalPlaybackSnapshot,
        address: InetAddress,
    ) {
        val processed = ProcessedAudioStream()
        val liveServer = LocalMediaServer(address, { _, _ -> null }, processed.asLiveSource()).also {
            it.start()
            server = it
        }
        val current = snapshot.items.getOrNull(snapshot.trackIndex)?.mediaMetadata ?: MediaMetadata.EMPTY
        streamMetadata = MediaMetadata.Builder()
            .setTitle(current.albumTitle ?: current.title)
            .setArtist(current.albumArtist ?: current.artist)
            .build()
        isStreamPlaying = snapshot.playWhenReady
        stream = processed
        processed.listener = object : ProcessedAudioStream.Listener {
            override fun onPlay() = runOnMain { setStreamPlaying(player, true) }

            override fun onPause() = runOnMain { setStreamPlaying(player, false) }

            override fun onNewGeneration(generation: Int) = runOnMain { loadGeneration(player, liveServer, generation) }
        }
        if (!currentHost.startProcessedStream(processed)) {
            onCastSessionUnavailable()
            return
        }
        handler.post(streamPoll)
    }

    private fun setStreamPlaying(player: CastPlayer, isPlaying: Boolean) {
        isStreamPlaying = isPlaying
        if (stream != null) player.playWhenReady = isPlaying
    }

    private fun loadGeneration(player: CastPlayer, liveServer: LocalMediaServer, generation: Int) {
        if (stream?.generation != generation) return
        val item = MediaItem.Builder()
            .setMediaId(liveMediaId(generation))
            .setUri(liveServer.liveUrlOf(generation))
            .setMimeType(ProcessedAudioStream.MIME_TYPE)
            .setMediaMetadata(streamMetadata)
            .build()
        player.setMediaItem(item)
        player.playWhenReady = isStreamPlaying
        player.prepare()
    }

    private fun runOnMain(action: () -> Unit) {
        handler.post(action)
    }

    private fun liveMediaId(generation: Int): String = "$LIVE_MEDIA_ID_PREFIX$generation"

    private fun remoteItem(item: MediaItem, url: String): MediaItem = MediaItem.Builder()
        .setMediaId(item.mediaId)
        .setUri(url)
        .setMimeType(mimeTypeOf(item.localConfiguration?.uri))
        .setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkUri(null).setArtworkData(null, null).build())
        .build()

    private fun open(snapshot: LocalPlaybackSnapshot, index: Int, offset: Long): LocalMediaServer.OpenedFile? {
        val uri = snapshot.items.getOrNull(index)?.localConfiguration?.uri ?: return null
        val descriptor = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
        val stream = FileInputStream(descriptor.fileDescriptor)
        stream.channel.position(offset)
        return LocalMediaServer.OpenedFile(
            descriptor.statSize,
            mimeTypeOf(uri),
            object : java.io.FilterInputStream(stream) {
                override fun close() {
                    super.close()
                    descriptor.close()
                }
            }
        )
    }

    private fun localAddress(): InetAddress? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
    }.getOrNull()

    private fun mimeTypeOf(uri: Uri?): String {
        val name = uri?.lastPathSegment.orEmpty().lowercase()
        return MIME_TYPES.entries.firstOrNull { (extension, _) -> name.endsWith(".$extension") }?.value ?: DEFAULT_MIME_TYPE
    }

    private companion object {
        const val LOG_TAG = "Cast"
        const val POLL_INTERVAL_MS = 1_000L
        const val SAVE_EVERY_POLLS = 10
        const val STREAM_POLL_INTERVAL_MS = 250L
        const val LIVE_MEDIA_ID_PREFIX = "abbay-live-"
        const val DEFAULT_MIME_TYPE = "audio/mpeg"
        val MIME_TYPES = mapOf(
            "mp3" to "audio/mpeg",
            "m4a" to "audio/mp4",
            "m4b" to "audio/mp4",
            "mp4" to "audio/mp4",
            "aac" to "audio/aac",
            "ogg" to "audio/ogg",
            "oga" to "audio/ogg",
            "opus" to "audio/ogg",
            "flac" to "audio/flac",
            "wav" to "audio/wav",
            "webm" to "audio/webm",
        )
    }
}
