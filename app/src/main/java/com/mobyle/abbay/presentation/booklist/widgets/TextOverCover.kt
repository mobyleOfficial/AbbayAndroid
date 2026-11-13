package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerReadAlong
import kotlin.math.roundToInt

/** A pane is narrow and can be tall (a tablet in portrait): the cover stays by its title, the spare height above. */
internal fun coverAlignment(isPane: Boolean): Alignment = if (isPane) Alignment.BottomCenter else Alignment.Center

/**
 * The read-along text exactly over the cover's slot. The lock dims it and takes its touches too, like the rest of the
 * player: the lock's own overlay sits inside the motion layout, under this.
 */
@Composable
internal fun TextOverCover(readAlong: PlayerReadAlong, bounds: Rect, isPane: Boolean, isScreenLocked: Boolean, actions: PlayerActions) {
    if (bounds.isEmpty) return
    val size = with(LocalDensity.current) { DpSize(bounds.width.toDp(), bounds.height.toDp()) }
    Box(modifier = Modifier.offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }.size(size)) {
        readAlong.square(Modifier.fillMaxSize(), coverAlignment(isPane))
        LockOverlay(isScreenLocked = isScreenLocked, onUnlock = { actions.onLockScreen(false) }, modifier = Modifier.matchParentSize())
    }
}
