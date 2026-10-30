package com.mobyle.abbay.presentation.playback.history

import com.model.HistoryEventType

/** Names a user move for the history. */
object MoveClassifier {
    /** Moves shorter than this are not worth an entry (a re-seek to the same place). */
    const val MINIMUM_MOVE_MS = 500L

    fun classify(fromMs: Long, toMs: Long, landsOnChapterStart: Boolean): HistoryEventType? = when {
        kotlin.math.abs(toMs - fromMs) < MINIMUM_MOVE_MS -> null
        landsOnChapterStart -> HistoryEventType.CHAPTER_JUMP
        else -> HistoryEventType.SEEK
    }
}
