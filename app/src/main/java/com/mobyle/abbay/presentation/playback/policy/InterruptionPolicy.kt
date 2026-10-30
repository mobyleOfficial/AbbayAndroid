package com.mobyle.abbay.presentation.playback.policy

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.settings.AppSettings
import com.settings.ShortInterruptionBehavior

/**
 * Audio focus and interruptions. Media3 pauses on a permanent loss and pauses
 * or ducks on a transient one; this adds the user options: lower the volume instead of pausing, no automatic resume
 * after interruptions of 5 min or more or after a call when "Resume after calls" is off, and resume when headphones
 * connect.
 */
@OptIn(UnstableApi::class)
class InterruptionPolicy(
    context: Context,
    private val player: ExoPlayer,
    private val settings: () -> AppSettings,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : Player.Listener {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var interruptedAt: Long? = null
    private var interruptionWasCall = false
    private var appliedBehavior: ShortInterruptionBehavior? = null
    private val registeredAt = clock()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            // Registration reports the devices already connected; only later additions count.
            if (clock() - registeredAt < REGISTRATION_GRACE_MS) return
            if (!settings().resumeOnHeadsetConnect || player.mediaItemCount == 0 || player.playWhenReady) return
            if (addedDevices.any { it.isSink && it.type in HEADPHONE_TYPES }) player.play()
        }
    }

    init {
        audioManager?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    /** Speech content makes Media3 pause on a "may duck" loss; music content makes it lower the volume. */
    fun applySettings() {
        val behavior = settings().shortInterruptions
        if (behavior == appliedBehavior) return
        appliedBehavior = behavior
        val contentType = if (behavior == ShortInterruptionBehavior.PAUSE) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(contentType).build(), true)
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            interruptedAt = clock()
            interruptionWasCall = audioManager?.mode in CALL_MODES
            return
        }
        val startedAt = interruptedAt ?: return
        interruptedAt = null
        if (playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE || !player.playWhenReady) return
        val tooLong = clock() - startedAt >= MAXIMUM_RESUMABLE_MS
        val callWithoutResume = interruptionWasCall && !settings().resumeAfterCalls
        if (tooLong || callWithoutResume) player.pause()
    }

    fun release() {
        audioManager?.unregisterAudioDeviceCallback(deviceCallback)
    }

    private companion object {
        const val MAXIMUM_RESUMABLE_MS = 5 * 60_000L
        const val REGISTRATION_GRACE_MS = 1_000L
        val CALL_MODES = setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_RINGTONE)
        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
        )
    }
}
