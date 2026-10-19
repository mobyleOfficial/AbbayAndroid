package com.mobyle.abbay.presentation.playback.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookNavigatorTest {
    private val navigator = BookNavigator(listOf(60_000L, 30_000L, 90_000L))

    @Test
    fun `book position adds the previous tracks`() {
        assertEquals(75_000L, navigator.bookPositionOf(TrackPosition(1, 15_000L)))
        assertEquals(180_000L, navigator.bookDurationMs)
    }

    @Test
    fun `seek by crosses file boundaries both ways`() {
        assertEquals(TrackPosition(1, 5_000L), navigator.seekBy(TrackPosition(0, 50_000L), 15_000L))
        assertEquals(TrackPosition(0, 55_000L), navigator.seekBy(TrackPosition(1, 10_000L), -15_000L))
        assertEquals(TrackPosition(2, 10_000L), navigator.seekBy(TrackPosition(0, 0L), 100_000L))
    }

    @Test
    fun `seek by clamps to the book`() {
        assertEquals(TrackPosition(0, 0L), navigator.seekBy(TrackPosition(0, 5_000L), -30_000L))
        assertEquals(TrackPosition(2, 90_000L), navigator.seekBy(TrackPosition(2, 80_000L), 30_000L))
    }

    @Test
    fun `a track boundary belongs to the next track`() {
        assertEquals(TrackPosition(1, 0L), navigator.positionAt(60_000L))
    }

    @Test
    fun `unknown durations keep seeks inside the current track`() {
        val partial = BookNavigator(listOf(60_000L, 0L))
        assertEquals(TrackPosition(1, 40_000L), partial.seekBy(TrackPosition(1, 10_000L), 30_000L))
        assertEquals(TrackPosition(1, 0L), partial.seekBy(TrackPosition(1, 10_000L), -30_000L))
    }

    @Test
    fun `next track stops at the last track`() {
        assertEquals(TrackPosition(1, 0L), navigator.nextTrack(TrackPosition(0, 10_000L)))
        assertNull(navigator.nextTrack(TrackPosition(2, 10_000L)))
    }

    @Test
    fun `previous track restarts the current track after 3 seconds`() {
        assertEquals(TrackPosition(1, 0L), navigator.previousTrack(TrackPosition(1, 3_001L)))
        assertEquals(TrackPosition(0, 0L), navigator.previousTrack(TrackPosition(1, 3_000L)))
        assertEquals(TrackPosition(0, 0L), navigator.previousTrack(TrackPosition(0, 1_000L)))
    }

    private val withChapters = BookNavigator(listOf(60_000L, 30_000L, 90_000L), chapterStartsMs = listOf(0L, 20_000L, 75_000L, 150_000L))

    @Test
    fun `chapter navigation uses the chapter list across files`() {
        assertEquals(2, withChapters.chapterIndexAt(80_000L))
        assertEquals(TrackPosition(1, 15_000L), withChapters.nextChapter(TrackPosition(0, 30_000L)))
        assertNull(withChapters.nextChapter(TrackPosition(2, 70_000L)))
    }

    @Test
    fun `previous chapter restarts after 3 seconds, otherwise goes back one chapter`() {
        assertEquals(TrackPosition(1, 15_000L), withChapters.previousChapter(TrackPosition(1, 25_000L)))
        assertEquals(TrackPosition(0, 20_000L), withChapters.previousChapter(TrackPosition(1, 16_000L)))
        assertEquals(TrackPosition(0, 0L), withChapters.previousChapter(TrackPosition(0, 2_000L)))
    }

    @Test
    fun `without a chapter list the tracks are the chapters`() {
        assertEquals(TrackPosition(1, 0L), navigator.nextChapter(TrackPosition(0, 10_000L)))
        assertEquals(TrackPosition(0, 0L), navigator.previousChapter(TrackPosition(1, 2_000L)))
    }
}
