package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R

@Composable
fun BoxScope.StreamingBadge(size: Dp = 22.dp, inset: Dp = 4.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(inset)
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
    ) {
        Icon(
            Icons.Outlined.Cloud,
            contentDescription = stringResource(R.string.librivox_streaming_only_badge),
            tint = Color.White,
            modifier = Modifier.size(size * ICON_FRACTION),
        )
    }
}

private const val SCRIM_ALPHA = 0.6f
private const val ICON_FRACTION = 0.62f
