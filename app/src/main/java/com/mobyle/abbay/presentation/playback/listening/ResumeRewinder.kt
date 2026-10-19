package com.mobyle.abbay.presentation.playback.listening

import com.listening.SleepTimer
import com.listening.SmartRewind
import com.listening.SmartRewindLevel

/**
 * Remembers the last pause and decides where a resume starts. The pause of
 * this process wins; after a restart the pause stored with the book is used once.
 */
class ResumeRewinder(private val clock: () -> Long = System::currentTimeMillis) {
    private var lastPause: PauseRecord? = null
    private var sleepStop: SleepStop? = null
    private var hasUsedStoredPause = false

    fun onPaused(bookId: String, bookPositionMs: Long) {
        lastPause = PauseRecord(bookId, clock(), bookPositionMs)
    }

    /** The sleep timer paused playback after a fade of [fadeMs]. */
    fun onSleepTimerStopped(bookId: String, bookPositionMs: Long, fadeMs: Long) {
        onPaused(bookId, bookPositionMs)
        sleepStop = SleepStop(bookId, clock(), fadeMs)
    }

    /** A new playlist was loaded: its stored pause may be used once. */
    fun onPlaylistChanged() {
        hasUsedStoredPause = false
    }

    /**
     * The book position to resume [bookId] from, or null to resume where it is. [storedPause] comes from the database
     * through the loaded media item.
     */
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
