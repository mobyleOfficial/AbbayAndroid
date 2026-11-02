package com.mobyle.abbay.presentation.readalong

import androidx.compose.ui.graphics.Color

/** How the text is drawn in the cover's square, and how it is drawn over the whole window. */
internal enum class ReadAlongStyle(val spacing: Int, val maxWidth: Int, val horizontalPadding: Int) {
    SQUARE(spacing = 14, maxWidth = 0, horizontalPadding = 20),
    FULL_SCREEN(spacing = 18, maxWidth = 600, horizontalPadding = 24),
}

/** The dark square the text sits on, over the player's cover tint. */
internal val ReadAlongFill = Color.Black.copy(alpha = 0.28f)

/** The full screen's own ground, dimming the player behind it. */
internal val ReadAlongFullScreenBackground = Color.Black.copy(alpha = 0.92f)

/** The smallest tappable side Material asks for, in dp. */
internal const val TOUCH_TARGET = 48

internal const val CONTROLS_HIDE_MS = 3_000L
internal const val CENTER_OFFSET_PX = 300
internal const val VERTICAL_PADDING = 96
internal const val READ_ALPHA = 0.35f
internal const val UPCOMING_ALPHA = 0.55f
internal const val DIMMED_ALPHA = 0.6f
internal const val CHIP_FILL_ALPHA = 0.12f
internal const val FADE_FRACTION = 0.18f
