package com.mobyle.abbay.presentation.playback.sleep

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import com.listening.ShakeDetector
import com.listening.ShakeSensitivity

/**
 * Accelerometer for "shake to extend": registered only while the timer runs,
 * at `SENSOR_DELAY_UI`; shakes count only while the screen is off.
 */
class ShakeSensor(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var detector: ShakeDetector? = null

    fun start(sensitivity: ShakeSensitivity) {
        val sensor = accelerometer ?: return
        if (detector != null) stop()
        detector = ShakeDetector(sensitivity.thresholdMetersPerSecondSquared)
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (detector == null) return
        sensorManager?.unregisterListener(this)
        detector = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentDetector = detector ?: return
        val isShake = currentDetector.onSample(event.timestamp, event.values[0], event.values[1], event.values[2])
        if (isShake && powerManager?.isInteractive == false) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
