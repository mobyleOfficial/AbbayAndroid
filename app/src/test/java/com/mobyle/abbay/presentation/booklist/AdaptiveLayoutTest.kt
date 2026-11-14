package com.mobyle.abbay.presentation.booklist

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun `a phone in portrait is one pane`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 411f, heightDp = 860f, fontScale = 1f))
    }

    @Test
    fun `a phone in landscape is one pane because it is too short`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 915f, heightDp = 380f, fontScale = 1f))
    }

    @Test
    fun `an unfolded foldable splits at the player's least width`() {
        assertEquals(PaneLayout.Split(360.dp), AdaptiveLayout.decide(widthDp = 841f, heightDp = 650f, fontScale = 1f))
    }

    @Test
    fun `a landscape tablet gives the player its largest width and the rest to browsing`() {
        assertEquals(PaneLayout.Split(460.dp), AdaptiveLayout.decide(widthDp = 1280f, heightDp = 760f, fontScale = 1f))
    }

    @Test
    fun `a portrait tablet is one pane, like a phone`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 800f, heightDp = 1200f, fontScale = 1f))
    }

    @Test
    fun `an unfolded foldable held in portrait is one pane`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 673f, heightDp = 841f, fontScale = 1f))
    }

    @Test
    fun `just under both minimums is one pane`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 719f, heightDp = 600f, fontScale = 1f))
        assertEquals(PaneLayout.Split(360.dp), AdaptiveLayout.decide(widthDp = 720f, heightDp = 600f, fontScale = 1f))
    }

    @Test
    fun `large text needs more room for both panes`() {
        assertEquals(PaneLayout.Single, AdaptiveLayout.decide(widthDp = 841f, heightDp = 650f, fontScale = 1.3f))
        assertEquals(PaneLayout.Split(504.dp), AdaptiveLayout.decide(widthDp = 1280f, heightDp = 760f, fontScale = 1.3f))
    }

    @Test
    fun `text scale steps`() {
        assertEquals(1f, AdaptiveLayout.textScale(1.1f))
        assertEquals(1.15f, AdaptiveLayout.textScale(1.15f))
        assertEquals(1.4f, AdaptiveLayout.textScale(2f))
    }
}
