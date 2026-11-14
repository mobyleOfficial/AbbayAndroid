package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.mobyle.abbay.presentation.common.theme.Background2Color
import com.mobyle.abbay.presentation.common.theme.SpanishGrayColor
import com.mobyle.abbay.presentation.common.theme.SurfaceColor
import com.mobyle.abbay.presentation.common.theme.isDarkTheme

@Composable
fun BookThumbnail(
    byteArray: String?,
    modifier: Modifier = Modifier.size(DEFAULT_SIDE),
    cornerRadius: Dp? = null,
    shadow: Boolean = false,
    requestSizePx: Int? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val side = minOf(maxWidth, maxHeight)
        val shape = RoundedCornerShape(cornerRadius ?: coverCornerRadius(side))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (shadow) Modifier.shadow(SHADOW_ELEVATION, shape, clip = false) else Modifier)
                .clip(shape)
                .background(placeholderBrush)
                .border(EDGE_WIDTH, Color.White.copy(alpha = EDGE_ALPHA), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Headphones,
                contentDescription = null,
                tint = SpanishGrayColor.copy(alpha = if (isDarkTheme) 1f else LIGHT_GLYPH_ALPHA),
                modifier = Modifier.size(side * GLYPH_FRACTION),
            )
            if (byteArray != null) {
                AsyncImage(
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(byteArray)
                        .apply { requestSizePx?.let { size(it) } }
                        .precision(Precision.EXACT)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

fun coverCornerRadius(side: Dp): Dp = when {
    side < 40.dp -> 4.dp
    side < 120.dp -> 8.dp
    side < 200.dp -> 12.dp
    else -> 14.dp
}

private val placeholderBrush = Brush.linearGradient(listOf(SurfaceColor, Background2Color))

private val DEFAULT_SIDE = 64.dp
private val SHADOW_ELEVATION = 12.dp
private val EDGE_WIDTH = 0.5.dp
private const val EDGE_ALPHA = 0.15f
private const val GLYPH_FRACTION = 0.4f
private const val LIGHT_GLYPH_ALPHA = 0.9f
