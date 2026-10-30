package com.mobyle.abbay.presentation.booklist

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * How the library screen is split: one pane, or browsing beside a player pane. Decided from the room the
 * app is given and the font scale, never from the device model, so a phone, both displays of a foldable,
 * a tablet and a resized window follow the same rules.
 */
sealed interface PaneLayout {
    /** One pane: the player covers browsing, and the chapters open in a sheet over the player. */
    data object Single : PaneLayout

    /** Browsing (or the chapters) beside a player pane [playerWidth] wide. */
    data class Split(val playerWidth: Dp) : PaneLayout
}

val PaneLayout.isSplit: Boolean get() = this is PaneLayout.Split

/**
 * The pure decision, so it is unit tested. The minimums are what each pane needs to read well: browsing keeps
 * a grid of at least three covers, and the player keeps its transport, scrubber and pills on one line with a
 * cover worth showing.
 */
object AdaptiveLayout {
    /** Browsing's least width: three covers with their titles. */
    const val MIN_BROWSING_WIDTH_DP = 360f

    /** The player pane's least and largest width. Wider than that, the extra room is browsing's. */
    const val MIN_PLAYER_WIDTH_DP = 360f
    const val MAX_PLAYER_WIDTH_DP = 460f

    /** The player pane's share of the width, between those two. */
    const val PLAYER_SHARE = 0.42f

    /** Below this height the player's cover would shrink to a thumbnail beside a grid: one pane. */
    const val MIN_SPLIT_HEIGHT_DP = 500f

    /** [widthDp] and [heightDp] are the space the screen's content is given (inside the system bars). */
    fun decide(widthDp: Float, heightDp: Float, fontScale: Float): PaneLayout {
        val scale = textScale(fontScale)
        val minPlayer = MIN_PLAYER_WIDTH_DP * scale
        val minBrowsing = MIN_BROWSING_WIDTH_DP * scale
        val player = (widthDp * PLAYER_SHARE).coerceIn(minPlayer, maxOf(MAX_PLAYER_WIDTH_DP, minPlayer))
        val fits = heightDp >= MIN_SPLIT_HEIGHT_DP && widthDp - player >= minBrowsing
        return if (fits) PaneLayout.Split(player.roundToInt().dp) else PaneLayout.Single
    }

    /** How much more room both panes need at this font scale. */
    fun textScale(fontScale: Float): Float = when {
        fontScale >= LARGE_FONT_SCALE -> LARGE_TEXT_ROOM
        fontScale >= ENLARGED_FONT_SCALE -> ENLARGED_TEXT_ROOM
        else -> 1f
    }

    private const val ENLARGED_FONT_SCALE = 1.15f
    private const val LARGE_FONT_SCALE = 1.3f
    private const val ENLARGED_TEXT_ROOM = 1.15f
    private const val LARGE_TEXT_ROOM = 1.4f
}

/** Browsing covered by the chapters pane: kept composed (so it keeps its state) but invisible, silent and untouchable. */
internal fun Modifier.hiddenUnder(isCovered: Boolean): Modifier = if (!isCovered) {
    this
} else {
    alpha(0f)
        .clearAndSetSemantics { }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
}
