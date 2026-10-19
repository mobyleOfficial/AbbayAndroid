package com.mobyle.abbay.presentation.playback.auto

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.playback.command.PlaybackCommands
import com.settings.AppSettings

/**
 * Android Auto's playing screen: Back · Forward · Speed (cycles) · Bookmark, plus
 * the sleep timer when "Sleep timer in car" is on. Tracks which connected controllers are cars.
 */
@OptIn(UnstableApi::class)
@Suppress("LongParameterList") // The service parts the car buttons use.
class CarControls(
    private val context: Context,
    private val player: Player,
    private val session: () -> MediaSession?,
    private val settings: () -> AppSettings,
    private val phoneButtons: () -> ImmutableList<CommandButton>,
    private val onSpeedChanged: () -> Unit,
) {
    private val controllers = mutableSetOf<MediaSession.ControllerInfo>()

    /** True while a car is connected (the sleep-timer schedule skips it unless "Sleep timer in car" is on). */
    val isConnected: Boolean get() = controllers.isNotEmpty()

    /** Returns true when [controller] is a car; it then gets [buttons]. */
    fun onConnected(session: MediaSession, controller: MediaSession.ControllerInfo): Boolean {
        val isCar = session.isAutoCompanionController(controller) || session.isAutomotiveController(controller)
        if (isCar) controllers += controller
        return isCar
    }

    fun onDisconnected(controller: MediaSession.ControllerInfo) {
        controllers.remove(controller)
    }

    fun buttons(): ImmutableList<CommandButton> {
        val phone = phoneButtons()
        val speed = CommandButton.Builder(CommandButton.ICON_PLAYBACK_SPEED)
            .setDisplayName(context.getString(R.string.playback_speed_cycle, formatSpeed(player.playbackParameters.speed)))
            .setSessionCommand(SessionCommand(PlaybackCommands.SPEED_CYCLE, Bundle.EMPTY))
            .build()
        val back = phone[BACK]
        val forward = phone[FORWARD]
        val bookmark = phone[BOOKMARK]
        return ImmutableList.copyOf(listOf(back, forward, speed, bookmark) + listOfNotNull(phone[SLEEP].takeIf { settings().sleepInCar }))
    }

    /** 1.0 → 1.25 → 1.5 → 1.75 → 2.0 → 1.0. */
    fun cycleSpeed() {
        val current = player.playbackParameters.speed
        val next = SPEEDS.firstOrNull { it > current + SPEED_TOLERANCE } ?: SPEEDS.first()
        player.playbackParameters = player.playbackParameters.withSpeed(next)
        onSpeedChanged()
        val activeSession = session() ?: return
        controllers.forEach { controller -> activeSession.setCustomLayout(controller, buttons()) }
    }

    private fun formatSpeed(speed: Float): String = "%.2f".format(speed).trimEnd('0').trimEnd('.', ',') + "×"

    private companion object {
        /** Positions in the phone notification's layout: Back · Forward · Bookmark · Sleep timer. */
        const val BACK = 0
        const val FORWARD = 1
        const val BOOKMARK = 2
        const val SLEEP = 3
        val SPEEDS = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        const val SPEED_TOLERANCE = 0.01f
    }
}
