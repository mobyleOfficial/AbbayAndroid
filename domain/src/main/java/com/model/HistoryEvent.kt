package com.model

enum class HistoryEventType {
    PLAY,
    PAUSE,
    SEEK,
    CHAPTER_JUMP,
    SLEEP_TIMER_STOP,
    SLEEP_TIMER_EXTEND,
    BOOKMARK_ADDED,
    FINISHED,
}

data class HistoryEvent(
    val id: Long,
    val bookId: String,
    val type: HistoryEventType,
    val at: Long,
    val positionMs: Long,
    val targetPositionMs: Long? = null,
) {
    companion object {
        const val MAX_EVENTS_PER_BOOK = 500

        const val MERGE_WINDOW_MS = 3_000L

        fun shouldMerge(previous: HistoryEvent?, type: HistoryEventType, at: Long): Boolean =
            previous != null &&
                previous.type == type &&
                (type == HistoryEventType.SEEK || type == HistoryEventType.CHAPTER_JUMP) &&
                at - previous.at in 0..MERGE_WINDOW_MS
    }
}
