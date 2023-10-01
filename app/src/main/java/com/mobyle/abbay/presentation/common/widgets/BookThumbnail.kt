package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R

@Composable
fun BookThumbnail(byteArray: ByteArray?, modifier: Modifier = Modifier) {
    val defaultModifier = Modifier
        .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
        .height(86.dp)
        .width(86.dp)

    Box(
        modifier = modifier.then(defaultModifier)
    ) {
        AsyncImage(
            contentScale = ContentScale.FillBounds,
            model = ImageRequest.Builder(LocalContext.current)
                .data(byteArray)
                .fallback(R.drawable.file_music)
                .error(R.drawable.file_music)
                .crossfade(true)
                .build(),
            contentDescription = ""
        )
    }
}
