package com.mobyle.abbay.presentation.playback.listening

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.media3.common.Player
import com.listening.MotionGestureDetector
import com.listening.MotionGestureDetector.Gesture
import com.mobyle.abbay.presentation.playback.sleep.TonePlayer
import com.settings.AppSettings

class MotionGestures(
    context: Context,
    private val player: Player,
    private val settings: () -> AppSettings,
    private val tones: TonePlayer,
    private val actions: Actions,
    private val clock: () -> Long = System::currentTimeMillis,
) : Player.Listener, SensorEventListener {
    class Actions(val pause: () -> Unit, val resume: () -> Unit, val rewind: (deltaMs: Long) -> Unit)

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val detector = MotionGestureDetector()
    private var isListening = false
    private var pausedFaceDownAt: Long? = null

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            pausedFaceDownAt = null
            detector.resetFace()
        }
        update()
    }

    fun update() {
        val current = settings()
        val isResumeWindowOpen = pausedFaceDownAt?.let { clock() - it < MotionGestureDetector.RESUME_WINDOW_MS } == true &&
            current.faceDownToPause
        val shouldListen = (player.playWhenReady && (current.faceDownToPause || current.flipToRewind)) || isResumeWindowOpen
        if (!isResumeWindowOpen && !player.playWhenReady) pausedFaceDownAt = null
        if (shouldListen == isListening) return
        if (shouldListen) {
            accelerometer?.takeIf {
                current.faceDownToPause
            }?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            gyroscope?.takeIf { current.flipToRewind }?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            sensorManager?.unregisterListener(this)
        }
        isListening = shouldListen
    }

    fun release() {
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gesture = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> detector.onAcceleration(event.timestamp, event.values[2])
            Sensor.TYPE_GYROSCOPE -> detector.onRotation(event.timestamp, event.values[1])
            else -> null
        } ?: return
        when (gesture) {
            Gesture.FACE_DOWN -> onFaceDown()
            Gesture.FACE_UP -> onFaceUp()
            Gesture.FLIP -> onFlip()
        }
    }

    private fun onFaceDown() {
        if (!player.playWhenReady || !isAllowed()) return
        pausedFaceDownAt = clock()
        actions.pause()
        confirm()
    }

    private fun onFaceUp() {
        val pausedAt = pausedFaceDownAt ?: return
        pausedFaceDownAt = null
        if (!player.playWhenReady && clock() - pausedAt < MotionGestureDetector.RESUME_WINDOW_MS) {
            actions.resume()
            confirm()
        }
        update()
    }

    private fun onFlip() {
        if (!player.playWhenReady || !isAllowed()) return
        actions.rewind(-FLIP_REWIND_MS)
        confirm()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun confirm() = tones.playConfirmation(TONE_VOLUME)

    private fun isAllowed(): Boolean {
        if (powerManager?.isInteractive == false) return true
        val state = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private companion object {
        const val FLIP_REWIND_MS = 30_000L
        const val TONE_VOLUME = 0.6f
    }
}
