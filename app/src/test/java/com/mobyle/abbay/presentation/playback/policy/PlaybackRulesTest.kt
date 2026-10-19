package com.mobyle.abbay.presentation.playback.policy

import com.mobyle.abbay.presentation.playback.command.TrackPosition
import com.player.PlaybackOptions
import com.player.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRulesTest {
    // Three 60 s files; chapters at 0, 30, 60 and 120 s.
    private val rules = PlaybackRules(listOf(60_000L, 60_000L, 60_000L), listOf(0L, 30_000L, 60_000L, 120_000L))

    @Test
    fun repeatChapterGoesBackWhenPlaybackLeavesTheChapter() {
        val options = PlaybackOptions(repeatMode = RepeatMode.CHAPTER)
        assertEquals(TrackPosition(0, 30_000L), rules.decide(options, at(0, 59_900L), at(1, 100L)))
        assertNull(rules.decide(options, at(0, 40_000L), at(0, 40_200L)))
    }

    @Test
    fun theLoopReturnsToAWhenPlaybackPassesB() {
        val options = PlaybackOptions(repeatMode = RepeatMode.AB, loopAMs = 70_000L, loopBMs = 80_000L)
        assertEquals(TrackPosition(1, 10_000L), rules.decide(options, at(1, 19_950L), at(1, 20_050L)))
        // A seek beyond B is not a crossing, and B without A does nothing.
        assertNull(rules.decide(options, at(1, 30_000L), at(1, 30_200L)))
        assertNull(rules.decide(options.copy(loopAMs = null), at(1, 19_950L), at(1, 20_050L)))
    }

    @Test
    fun outroSkipsToTheNextFileOrTheEnd() {
        val options = PlaybackOptions(skipOutroSeconds = 5)
        assertEquals(TrackPosition(1, 0L), rules.decide(options, at(0, 54_900L), at(0, 55_100L)))
        assertEquals(TrackPosition(2, 60_000L), rules.decide(options, at(2, 54_900L), at(2, 55_100L)))
        assertEquals(TrackPosition(2, 0L), rules.decide(options.copy(repeatMode = RepeatMode.TRACK), at(2, 54_900L), at(2, 55_100L)))
        assertEquals(TrackPosition(0, 0L), rules.decide(options.copy(repeatMode = RepeatMode.BOOK), at(2, 54_900L), at(2, 55_100L)))
        assertNull(rules.decide(options, at(0, 56_000L), at(0, 56_200L)))
    }

    @Test
    fun introIsSkippedOnlyAtTheStartOfAFile() {
        val options = PlaybackOptions(skipIntroSeconds = 8)
        assertEquals(TrackPosition(2, 8_000L), rules.introTarget(options, at(2, 0L)))
        assertNull(rules.introTarget(options, at(2, 3_000L)))
        assertNull(rules.introTarget(options.copy(skipIntroSeconds = 120), at(2, 0L)))
        assertNull(rules.introTarget(PlaybackOptions(), at(2, 0L)))
    }

    private fun at(track: Int, positionMs: Long) = TrackPosition(track, positionMs)
}
