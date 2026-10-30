package com.mobyle.abbay.presentation.playback.media

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.mobyle.abbay.R

@OptIn(UnstableApi::class)
class AudiobookNotificationProvider(private val context: Context) : DefaultMediaNotificationProvider(context) {
    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val skipBack = customLayout.getOrNull(0)
        val skipForward = customLayout.getOrNull(1)
        val playPause = CommandButton.Builder(if (showPauseButton) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY)
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setDisplayName(context.getString(if (showPauseButton) R.string.playback_pause else R.string.playback_play))
            .build()

        val compactButtons = listOfNotNull(skipBack, playPause, skipForward).mapIndexed { index, button -> button.inCompactView(index) }
        return ImmutableList.copyOf(compactButtons + customLayout.drop(COMPACT_CUSTOM_BUTTONS))
    }

    private companion object {
        const val COMPACT_CUSTOM_BUTTONS = 2
    }

    private fun CommandButton.inCompactView(index: Int): CommandButton {
        val builder = CommandButton.Builder()
            .setDisplayName(displayName)
            .setIconResId(iconResId)
            .setEnabled(isEnabled)
            .setExtras(Bundle(extras).apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, index) })
        sessionCommand?.let(builder::setSessionCommand) ?: builder.setPlayerCommand(playerCommand)
        return builder.build()
    }
}
