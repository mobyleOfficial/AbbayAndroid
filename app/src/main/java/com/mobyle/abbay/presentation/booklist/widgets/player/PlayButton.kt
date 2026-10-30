package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun PlayButton(playerIcon: PlayingState, size: Dp = 48.dp, filled: Boolean = false, onClick: () -> Unit) {
    val glyphColor = if (filled) MaterialTheme.colorScheme.primary else AbbayColors.content
    IconButton(onClick = { if (playerIcon != PlayingState.LOADING) onClick() }, modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (filled) AbbayColors.content else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            val icon = playerIcon.toIcon()
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = playerIcon.label(),
                    tint = glyphColor,
                    modifier = Modifier.size(size * if (filled) FILLED_ICON_RATIO else ICON_RATIO),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(size * SPINNER_RATIO), color = glyphColor, strokeWidth = 2.dp)
            }
        }
    }
}

private const val ICON_RATIO = 0.6f
private const val FILLED_ICON_RATIO = 0.5f
private const val SPINNER_RATIO = 0.45f
