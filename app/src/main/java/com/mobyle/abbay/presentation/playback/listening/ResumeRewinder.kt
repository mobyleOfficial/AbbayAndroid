package com.mobyle.abbay.presentation.playback.listening

import com.listening.SleepTimer
import com.listening.SmartRewind
import com.listening.SmartRewindLevel

class ResumeRewinder(private val clock: () -> Long = System::currentTimeMillis) {
    private var lastPause: PauseRecord? = null
    private var sleepStop: SleepStop? = null
    private var hasUsedStoredPause = false

    fun onPaused(bookId: String, bookPositionMs: Long) {
        lastPause = PauseRecord(bookId, clock(), bookPositionMs)
    }

    fun onSleepTimerStopped(bookId: String, bookPositionMs: Long, fadeMs: Long) {
        onPaused(bookId, bookPositionMs)
        sleepStop = SleepStop(bookId, clock(), fadeMs)
    }

    fun onPlaylistChanged() {
        hasUsedStoredPause = false
    }

    fun resumePositionMs(bookId: String, bookPositionMs: Long, level: SmartRewindLevel, storedPause: PauseRecord?): Long? {
        val pause = lastPause?.takeIf { it.bookId == bookId }
            ?: storedPause?.takeIf { it.bookId == bookId && !hasUsedStoredPause }
            ?: return null
        val now = clock()
        val extraRewindMs = sleepStop
            ?.takeIf { it.bookId == bookId && now - it.stoppedAt <= SleepTimer.REWIND_AFTER_STOP_WINDOW_MS }
            ?.fadeMs ?: 0L

        lastPause = null
        sleepStop = null
        hasUsedStoredPause = true

        val target = SmartRewind.resumePositionMs(bookPositionMs, pause.bookPositionMs, now - pause.pausedAt, level, extraRewindMs)
        return target.takeIf { it < bookPositionMs }
    }

    private data class SleepStop(val bookId: String, val stoppedAt: Long, val fadeMs: Long)
}
