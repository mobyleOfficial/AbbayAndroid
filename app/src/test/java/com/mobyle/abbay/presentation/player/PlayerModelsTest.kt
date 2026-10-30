package com.mobyle.abbay.presentation.player

import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.SpeedSteps
import com.mobyle.abbay.presentation.booklist.widgets.player.progressFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerModelsTest {
    @Test
    fun speedSnapsToFiveHundredthsInsideTheRange() {
        assertEquals(1.2f, SpeedSteps.snap(1.2000001f))
        assertEquals(1.25f, SpeedSteps.snap(1.23f))
        assertEquals(0.5f, SpeedSteps.snap(0.1f))
        assertEquals(3.5f, SpeedSteps.snap(9f))
        assertEquals(1.05f, SpeedSteps.snap(1f + SpeedSteps.STEP))
    }

    @Test
    fun sliderHasOneStopPerFiveHundredths() {
        assertEquals(59, SpeedSteps.SLIDER_STEPS)
    }

    @Test
    fun positionFindsTheCurrentChapter() {
        val book = SampleBooks.singleFileBook.copy(progress = 700_000L)
        val position = PlayerPosition.of(book)

        assertEquals(1, position.chapter?.index)
        assertEquals(100_000L, position.chapterElapsedMs)
        assertEquals(6_600_000L, position.chapterDurationMs)
        assertEquals(3, position.chapterCount)
    }

    @Test
    fun theLastThirtySecondsCountAsFinished() {
        assertEquals(1f, progressFraction(bookPositionMs = 9_975_000L, durationMs = 10_000_000L))
        assertEquals(0.5f, progressFraction(bookPositionMs = 5_000_000L, durationMs = 10_000_000L))
        assertEquals(0f, progressFraction(bookPositionMs = 5_000L, durationMs = 0L))
    }
}
