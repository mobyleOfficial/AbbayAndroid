package com.mobyle.abbay.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * The colour of the strip behind the status bar (MainActivity): the app bars' `primary`, unless something drawn right
 * under it, such as the open player, gives its own top colour. The colour is read while drawing, so one that follows
 * a drag repaints the strip without recomposing anything.
 */
class StatusBarColor {
    internal var override by mutableStateOf<(() -> Color)?>(null)
}

val LocalStatusBarColor = staticCompositionLocalOf { StatusBarColor() }

@Composable
fun Modifier.statusBarBackground(statusBarColor: StatusBarColor): Modifier {
    val appBarColor = MaterialTheme.colorScheme.primary
    return drawBehind { drawRect(statusBarColor.override?.invoke() ?: appBarColor) }
}

/** While composed, [color] paints the status bar; null leaves it to the app bars. */
@Composable
fun OverrideStatusBarColor(color: (() -> Color)?) {
    val statusBarColor = LocalStatusBarColor.current
    SideEffect { statusBarColor.override = color }
    DisposableEffect(statusBarColor) { onDispose { statusBarColor.override = null } }
}
