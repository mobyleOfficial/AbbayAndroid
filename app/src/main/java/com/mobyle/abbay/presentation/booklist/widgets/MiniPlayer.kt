package com.mobyle.abbay.presentation.booklist.widgets

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.utils.currentFraction
import com.model.Book
import com.model.BookFolder
import java.math.RoundingMode
import java.text.DecimalFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MiniPlayer(book: Book, scaffoldState: BottomSheetScaffoldState, modifier: Modifier) {
    val originalFraction = scaffoldState.currentFraction
    val df = DecimalFormat("#.#")
    df.roundingMode = RoundingMode.CEILING
    val fraction = df.format(originalFraction).toFloat()
    val aux = if(fraction <= 0.3) 0.3f else fraction

    val size by animateDpAsState(
        targetValue = (200 * aux).dp,
        animationSpec = tween(durationMillis = 50),
        label = ""
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)

    ) {
        AsyncImage(
            contentScale = ContentScale.FillBounds,
            model = ImageRequest.Builder(LocalContext.current)
                .data(book.thumbnail)
                .fallback(R.drawable.file_music)
                .error(R.drawable.file_music)
                .crossfade(true)
                .build(),
            modifier = modifier.then(
                Modifier
                    .size(size)
            ),
            contentDescription = ""
        )
//        Box(
//            modifier = modifier.then(
//                Modifier
//                    .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
//            )
//        ) {
//            AsyncImage(
//                contentScale = ContentScale.FillBounds,
//                model = ImageRequest.Builder(LocalContext.current)
//                    .data(book.thumbnail)
//                    .fallback(R.drawable.file_music)
//                    .error(R.drawable.file_music)
//                    .crossfade(true)
//                    .build(),
//                modifier = Modifier
//                    .size(200.dp)
//                    .graphicsLayer(scaleX = scale, scaleY = scale),
//                contentDescription = ""
//            )
//        }
        AnimatedVisibility(
            visible = fraction <= 0.3,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column {
                Text(book.name)
                Row {
                    Text("Ouvido")
                    Text("Faltando")
                }
                Row {
                    Text("Controles")
                    if (book is BookFolder) {
                        Text("1/${book.bookFileList.size}")
                    }

                }
            }
        }
    }
}