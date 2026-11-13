package com.mobyle.abbay.presentation.readalong

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The square's text, or the full screen's: larger there, in a readable column so a wide window doesn't run edge to edge. */
enum class ReadAlongStyle(val maxLineWidth: Dp?, val lineMargin: Dp, val lineSpacing: Dp) {
    SQUARE(maxLineWidth = null, lineMargin = 20.dp, lineSpacing = 20.dp),
    FULL_SCREEN(maxLineWidth = 600.dp, lineMargin = 24.dp, lineSpacing = 18.dp),
}
