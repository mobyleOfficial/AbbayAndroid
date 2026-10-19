package com.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryEventTest {
    private val seek = HistoryEvent(1, "1", HistoryEventType.SEEK, at = 10_000L, positionMs = 0L, targetPositionMs = 30_000L)

    @Test
    fun quickSeeksOfTheSameKindMerge() {
        assertTrue(HistoryEvent.shouldMerge(seek, HistoryEventType.SEEK, 12_999L))
        assertTrue(HistoryEvent.shouldMerge(seek.copy(type = HistoryEventType.CHAPTER_JUMP), HistoryEventType.CHAPTER_JUMP, 11_000L))
    }

    @Test
    fun otherEventsSlowMovesAndMixedKindsDoNotMerge() {
        assertFalse(HistoryEvent.shouldMerge(seek, HistoryEventType.SEEK, 13_001L))
        assertFalse(HistoryEvent.shouldMerge(seek, HistoryEventType.CHAPTER_JUMP, 11_000L))
        assertFalse(HistoryEvent.shouldMerge(seek.copy(type = HistoryEventType.PLAY), HistoryEventType.PLAY, 10_500L))
        assertFalse(HistoryEvent.shouldMerge(null, HistoryEventType.SEEK, 10_500L))
    }
}
