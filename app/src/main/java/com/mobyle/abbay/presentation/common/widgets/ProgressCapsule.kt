package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun ProgressCapsule(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    fill: Color = MaterialTheme.colorScheme.tertiary,
    track: Color = AbbayColors.content.copy(alpha = TRACK_ALPHA),
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val filled = size.width * fraction.coerceIn(0f, 1f)
        if (filled > 0f) drawRoundRect(color = fill, size = Size(filled, size.height), cornerRadius = radius)
    }
}

private const val TRACK_ALPHA = 0.15f
