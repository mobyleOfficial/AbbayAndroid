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

class StatusBarColor {
    internal var override by mutableStateOf<(() -> Color)?>(null)
}

val LocalStatusBarColor = staticCompositionLocalOf { StatusBarColor() }

@Composable
fun Modifier.statusBarBackground(statusBarColor: StatusBarColor): Modifier {
    val appBarColor = MaterialTheme.colorScheme.primary
    return drawBehind { drawRect(statusBarColor.override?.invoke() ?: appBarColor) }
}

@Composable
fun OverrideStatusBarColor(color: (() -> Color)?) {
    val statusBarColor = LocalStatusBarColor.current
    SideEffect { statusBarColor.override = color }
    DisposableEffect(statusBarColor) { onDispose { statusBarColor.override = null } }
}
