package com.mobyle.abbay.presentation.playback.media

import androidx.annotation.OptIn
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler

@OptIn(UnstableApi::class)
class AudiobookPlayer(
    player: Player,
    private val commands: PlaybackCommandHandler,
    private val beforeResume: () -> Unit = {},
    private val showsCover: () -> Boolean = { true },
) : ForwardingPlayer(player) {
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
