package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun BookThumbnail(byteArray: String?, modifier: Modifier = Modifier) {
    val defaultModifier = Modifier
        .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
        .height(64.dp)
        .width(64.dp)

    Box(
        modifier = modifier.then(defaultModifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            contentScale = ContentScale.FillBounds,
            model = ImageRequest.Builder(LocalContext.current)
                .data(byteArray)
                // Each size gets its own decode: the grid's larger covers must not reuse the list's small one.
                .precision(Precision.EXACT)
                .fallback(R.drawable.file_music)
                .error(R.drawable.file_music)
                .crossfade(true)
                .build(),
            // Decorative: the book title is always next to it.
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            // The fallback icon takes the theme's content color: white in Dark (as before), dark in Light.
            colorFilter = if (byteArray == null) ColorFilter.tint(AbbayColors.content) else null,
        )
    }
}
