package com.mobyle.abbay.presentation.playback.media

import androidx.annotation.OptIn
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler

/**
 * The player the session exposes. It does not advertise "next/previous item", so the notification and the lock
 * screen show the skip buttons of the custom layout; "seek forward/back"
 * and any remaining next/previous calls are book-time skips that cross file boundaries. Every resume passes through
 * [beforeResume] first. Without "Cover on lock screen" ([showsCover]) the session's metadata carries no artwork, so
 * neither the notification nor the lock screen shows the cover.
 */
@OptIn(UnstableApi::class)
class AudiobookPlayer(
    player: Player,
    private val commands: PlaybackCommandHandler,
    private val beforeResume: () -> Unit = {},
    private val showsCover: () -> Boolean = { true },
) : ForwardingPlayer(player) {
    /**
     * The session's listeners, kept only to re-announce the metadata when the cover setting changes. They are not
     * wrapped: Kotlin's `by` delegation does not forward Java default methods (all of `Player.Listener`), and a wrapper
     * hid every playback event from the session (no notification, no foreground service). The notification and the
     * platform session (lock screen) read [getMediaMetadata], which applies the cover rule.
     */
    private val listeners = mutableListOf<Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        listeners += listener
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
        super.removeListener(listener)
    }

    override fun getMediaMetadata(): MediaMetadata = withCoverRule(super.getMediaMetadata())

    /** Tells the session the metadata changed after "Cover on lock screen" was switched. */
    fun republishMetadata() {
        val metadata = mediaMetadata
        val events = Player.Events(FlagSet.Builder().add(Player.EVENT_MEDIA_METADATA_CHANGED).build())
        listeners.toList().forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
            listener.onEvents(this, events)
        }
    }

    private fun withCoverRule(metadata: MediaMetadata): MediaMetadata =
        if (showsCover()) metadata else metadata.buildUpon().setArtworkUri(null).setArtworkData(null, null).build()

    /** Smart rewind runs before playback starts, so the rewound audio is the first heard. */
    override fun play() {
        if (!playWhenReady) beforeResume()
        super.play()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady && !this.playWhenReady) beforeResume()
        super.setPlayWhenReady(playWhenReady)
    }

    override fun seekToNext() = commands.skipForward()

    override fun seekToPrevious() = commands.skipBack()

    override fun seekForward() = commands.skipForward()

    override fun seekBack() = commands.skipBack()

    override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
        .apply { HIDDEN_COMMANDS.forEach { remove(it) } }
        .addAll(Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_BACK)
        .build()

    override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    private companion object {
        val HIDDEN_COMMANDS = listOf(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )
    }
}
