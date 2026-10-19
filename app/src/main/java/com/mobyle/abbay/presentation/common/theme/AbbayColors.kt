package com.mobyle.abbay.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Colors of text and icons drawn on the app's backgrounds; replaces the fixed white of the dark-only design. */
object AbbayColors {
    val content: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface
}
