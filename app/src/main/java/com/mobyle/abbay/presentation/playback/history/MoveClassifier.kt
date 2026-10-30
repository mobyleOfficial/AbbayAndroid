package com.mobyle.abbay.presentation.playback.history

import com.model.HistoryEventType

object MoveClassifier {
    const val MINIMUM_MOVE_MS = 500L

    fun classify(fromMs: Long, toMs: Long, landsOnChapterStart: Boolean): HistoryEventType? = when {
        kotlin.math.abs(toMs - fromMs) < MINIMUM_MOVE_MS -> null
        landsOnChapterStart -> HistoryEventType.CHAPTER_JUMP
        else -> HistoryEventType.SEEK
    }
}
