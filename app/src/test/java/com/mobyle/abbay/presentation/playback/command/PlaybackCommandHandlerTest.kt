package com.mobyle.abbay.presentation.playback.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCommandHandlerTest {
    private class FakePlaylistPlayer(
        override var currentPosition: TrackPosition,
        override val trackDurationsMs: List<Long> = listOf(60_000L, 30_000L, 90_000L),
        override val chapterStartsMs: List<Long> = emptyList(),
        override var isPlaying: Boolean = false,
    ) : PlaylistPlayer {
        override fun seekTo(position: TrackPosition) {
            currentPosition = position
        }

        override fun play() {
            isPlaying = true
        }

        override fun pause() {
            isPlaying = false
        }
    }

    private val player = FakePlaylistPlayer(TrackPosition(1, 20_000L))
    private val handler = PlaybackCommandHandler(player, smallBackMs = { 15_000L }, smallForwardMs = { 30_000L })

    @Test
    fun `play pause toggles`() {
        handler.playPause()
        assertTrue(player.isPlaying)
        handler.playPause()
        assertFalse(player.isPlaying)
    }

    @Test
    fun `skips use the configured amounts and cross tracks`() {
        handler.skipForward()
        assertEquals(TrackPosition(2, 20_000L), player.currentPosition)
        handler.skipBack()
        assertEquals(TrackPosition(2, 5_000L), player.currentPosition)
    }

    @Test
    fun `custom actions map to commands`() {
        assertTrue(handler.execute(PlaybackCommands.SEEK_TO_BOOK_POSITION, deltaMs = 0L, positionMs = 170_000L))
        assertEquals(TrackPosition(2, 80_000L), player.currentPosition)

        assertTrue(handler.execute(PlaybackCommands.SEEK_BY, deltaMs = -100_000L, positionMs = 0L))
        assertEquals(TrackPosition(1, 10_000L), player.currentPosition)

        assertTrue(handler.execute(PlaybackCommands.NEXT_CHAPTER, 0L, 0L))
        assertEquals(TrackPosition(2, 0L), player.currentPosition)

        assertTrue(handler.execute(PlaybackCommands.PREVIOUS_TRACK, 0L, 0L))
        assertEquals(TrackPosition(1, 0L), player.currentPosition)

        assertTrue(handler.execute(PlaybackCommands.NEXT_TRACK, 0L, 0L))
        assertEquals(TrackPosition(2, 0L), player.currentPosition)

        assertTrue(handler.execute(PlaybackCommands.PREVIOUS_CHAPTER, 0L, 0L))
        assertEquals(TrackPosition(1, 0L), player.currentPosition)

        assertFalse(handler.execute("unknown", 0L, 0L))
    }

    @Test
    fun `book position follows the current track`() {
        assertEquals(80_000L, handler.bookPositionMs())
    }

    @Test
    fun `commands do nothing without a loaded book`() {
        val empty = FakePlaylistPlayer(TrackPosition(0, 0L), trackDurationsMs = emptyList())
        val emptyHandler = PlaybackCommandHandler(empty, { 15_000L }, { 30_000L })

        emptyHandler.skipForward()
        emptyHandler.previousTrack()

        assertEquals(TrackPosition(0, 0L), empty.currentPosition)
    }
}
