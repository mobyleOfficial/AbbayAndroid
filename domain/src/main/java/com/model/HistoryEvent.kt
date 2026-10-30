package com.model

/** Kinds of history events. */
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

/**
 * One entry of a book's history. [positionMs] is the book position where the event happened; for seeks and chapter
 * jumps it is the position before the move and [targetPositionMs] the position after it.
 */
data class HistoryEvent(
    val id: Long,
    val bookId: String,
    val type: HistoryEventType,
    val at: Long,
    val positionMs: Long,
    val targetPositionMs: Long? = null,
) {
    companion object {
        /** Older events of a book are dropped beyond this count. */
        const val MAX_EVENTS_PER_BOOK = 500

        /** Moves made in quick succession (holding a skip button) are kept as one entry. */
        const val MERGE_WINDOW_MS = 3_000L

        /** True when a new move of [type] at [at] should extend [previous] instead of adding an entry. */
        fun shouldMerge(previous: HistoryEvent?, type: HistoryEventType, at: Long): Boolean =
            previous != null &&
                previous.type == type &&
                (type == HistoryEventType.SEEK || type == HistoryEventType.CHAPTER_JUMP) &&
                at - previous.at in 0..MERGE_WINDOW_MS
    }
}
