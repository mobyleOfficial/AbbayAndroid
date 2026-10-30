package com.mobyle.abbay.presentation.playback.policy

import android.view.KeyEvent
import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.settings.AppSettings
import com.settings.HeadsetLongPressAction
import com.settings.HeadsetSkipAction
import kotlinx.coroutines.CoroutineScope

class MediaButtonRouter(
    scope: CoroutineScope,
    private val sessionPlayer: Player,
    private val commands: PlaybackCommandHandler,
    private val settings: () -> AppSettings,
    private val onLongPressAction: (HeadsetLongPressAction) -> Unit,
) {
    private val headset = HeadsetButtons(scope) { gesture ->
        when (gesture) {
            HeadsetButtons.Gesture.SINGLE -> playPause()
            HeadsetButtons.Gesture.DOUBLE -> next()
            HeadsetButtons.Gesture.TRIPLE -> previous()
            HeadsetButtons.Gesture.LONG -> settings().headsetLongPressAction.takeIf {
                it != HeadsetLongPressAction.NONE
            }?.let(onLongPressAction)
        }
    }

    fun handle(event: KeyEvent): Boolean {
        val isFirstDown = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        when (event.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                if (event.action == KeyEvent.ACTION_DOWN) headset.onDown(event.repeatCount, event.isLongPress || event.repeatCount > 0)

            KeyEvent.KEYCODE_MEDIA_NEXT -> if (isFirstDown) next()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (isFirstDown) previous()
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> if (isFirstDown) commands.skipForward()
            KeyEvent.KEYCODE_MEDIA_REWIND -> if (isFirstDown) commands.skipBack()
            else -> return false
        }
        return true
    }

    private fun playPause() = if (sessionPlayer.playWhenReady) sessionPlayer.pause() else sessionPlayer.play()

    private fun next() = when (settings().headsetNextAction) {
        HeadsetSkipAction.SKIP -> commands.skipForward()
        HeadsetSkipAction.CHAPTER -> commands.nextChapter()
        HeadsetSkipAction.TRACK -> commands.nextTrack()
    }

    private fun previous() = when (settings().headsetPreviousAction) {
        HeadsetSkipAction.SKIP -> commands.skipBack()
        HeadsetSkipAction.CHAPTER -> commands.previousChapter()
        HeadsetSkipAction.TRACK -> commands.previousTrack()
    }
}
