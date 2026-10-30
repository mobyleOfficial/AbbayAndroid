package com.mobyle.abbay.presentation.playback.policy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PauseTimeout(
    private val scope: CoroutineScope,
    private val timeoutMinutes: () -> Int,
    private val onTimeout: () -> Unit,
) {
    private var job: Job? = null

    fun onPaused() {
        job?.cancel()
        val minutes = timeoutMinutes()
        if (minutes <= 0) return
        job = scope.launch {
            delay(minutes * MILLISECONDS_PER_MINUTE)
            onTimeout()
        }
    }

    fun onPlaying() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val MILLISECONDS_PER_MINUTE = 60_000L
    }
}
