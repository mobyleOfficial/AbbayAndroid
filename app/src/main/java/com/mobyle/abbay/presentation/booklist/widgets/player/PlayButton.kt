package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.theme.AbbayColors

/** Play/pause; a spinner while the book is loading. */
@Composable
fun PlayButton(playerIcon: PlayingState, size: Dp = 48.dp, onClick: () -> Unit) {
    IconButton(onClick = { if (playerIcon != PlayingState.LOADING) onClick() }, modifier = Modifier.size(size)) {
        val icon = playerIcon.toIcon()
        if (icon != null) {
            Icon(
                icon,
                contentDescription = playerIcon.label(),
                tint = AbbayColors.content,
                modifier = Modifier.size(size * ICON_RATIO),
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.background(Color.Transparent), color = AbbayColors.content, strokeWidth = 2.dp)
        }
    }
}

private const val ICON_RATIO = 0.6f
