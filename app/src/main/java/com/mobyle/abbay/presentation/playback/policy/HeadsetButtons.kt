package com.mobyle.abbay.presentation.playback.policy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single-button headsets: one press plays or pauses, two presses act as NEXT,
 * three as PREVIOUS, and holding the button is the long press. Media3 passes only key-down events to the session, so a
 * press is a first key-down and a long press is a repeat the system flags as long; presses count when they come within
 * [PRESS_WINDOW_MS] of each other, which also leaves time for a long press to show up before a single press fires.
 */
class HeadsetButtons(private val scope: CoroutineScope, private val onGesture: (Gesture) -> Unit) {
    enum class Gesture { SINGLE, DOUBLE, TRIPLE, LONG }

    private var presses = 0
    private var pending: Job? = null
    private var longPressSent = false

    /** A key-down; [repeatCount] is 0 for a new press and grows while the key is held. */
    fun onDown(repeatCount: Int, isLongPress: Boolean) {
        if (repeatCount == 0) {
            longPressSent = false
            press()
        } else if (isLongPress && !longPressSent) {
            longPressSent = true
            pending?.cancel()
            pending = null
            presses = 0
            onGesture(Gesture.LONG)
        }
    }

    private fun press() {
        presses++
        pending?.cancel()
        if (presses >= MAXIMUM_PRESSES) {
            fire()
            return
        }
        pending = scope.launch {
            delay(PRESS_WINDOW_MS)
            fire()
        }
    }

    private fun fire() {
        val gesture = when (presses) {
            1 -> Gesture.SINGLE
            2 -> Gesture.DOUBLE
            else -> Gesture.TRIPLE
        }
        presses = 0
        pending = null
        onGesture(gesture)
    }

    private companion object {
        const val PRESS_WINDOW_MS = 600L
        const val MAXIMUM_PRESSES = 3
    }
}
