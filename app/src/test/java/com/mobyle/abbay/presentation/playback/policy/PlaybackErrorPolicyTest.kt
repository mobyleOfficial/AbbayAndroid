package com.mobyle.abbay.presentation.playback.policy

import com.mobyle.abbay.presentation.playback.policy.PlaybackErrorPolicy.Decision
import com.mobyle.abbay.presentation.playback.policy.PlaybackErrorPolicy.ErrorKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorPolicyTest {
    @Test
    fun `an undecodable track is skipped`() {
        assertEquals(Decision.SkipToNext(1), PlaybackErrorPolicy.decide(ErrorKind.UNPLAYABLE, 1, 3, emptySet()))
    }

    @Test
    fun `when every track failed the book is unplayable`() {
        assertEquals(Decision.StopBookUnplayable, PlaybackErrorPolicy.decide(ErrorKind.UNPLAYABLE, 2, 3, setOf(0, 1)))
        assertEquals(Decision.StopBookUnplayable, PlaybackErrorPolicy.decide(ErrorKind.UNPLAYABLE, 0, 1, emptySet()))
    }

    @Test
    fun `a failing last track ends the book when others played`() {
        assertEquals(Decision.StopAtEnd, PlaybackErrorPolicy.decide(ErrorKind.UNPLAYABLE, 2, 3, emptySet()))
    }

    @Test
    fun `storage errors stop playback instead of skipping`() {
        assertEquals(Decision.StopStorageUnavailable, PlaybackErrorPolicy.decide(ErrorKind.STORAGE, 0, 3, emptySet()))
    }

    @Test
    fun `a lost connection pauses on the same track instead of skipping`() {
        assertEquals(Decision.PauseForNetwork, PlaybackErrorPolicy.decide(ErrorKind.NETWORK, 1, 3, setOf(0)))
    }
}
