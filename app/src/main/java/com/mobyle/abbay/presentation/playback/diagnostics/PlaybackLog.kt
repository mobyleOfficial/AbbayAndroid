package com.mobyle.abbay.presentation.playback.diagnostics

import com.diagnostics.LogLine
import com.diagnostics.LogRingBuffer

object PlaybackLog {
    val buffer = LogRingBuffer()

    fun add(message: String, fileName: String? = null) = buffer.add(LogLine(System.currentTimeMillis(), message, fileName))
}
