package com.mobyle.abbay.presentation.playback.diagnostics

import com.diagnostics.LogLine
import com.diagnostics.LogRingBuffer

/** The playback service's in-memory log for About → Diagnostics. Never written to disk. */
object PlaybackLog {
    val buffer = LogRingBuffer()

    fun add(message: String, fileName: String? = null) = buffer.add(LogLine(System.currentTimeMillis(), message, fileName))
}
