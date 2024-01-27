package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.model.Book
import java.lang.Float
import kotlin.OptIn
import kotlin.math.max
import kotlin.to
import kotlin.with

@OptIn(ExperimentalMaterialApi::class, ExperimentalMotionApi::class)
@Composable
fun MiniPlayer(book: Book, scaffoldState: BottomSheetScaffoldState, modifier: Modifier) {
    val swipeableState = rememberSwipeableState(0)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val swipeAreaHeight = screenHeight
    val anchors = mapOf(0f to 0, -swipeAreaHeight to 1)

    // Calculate swipe progress -> swipe offset / max swipe offset
    val swipeProgress = swipeableState.offset.value / -swipeAreaHeight

    // Limit swipe progress between 0f and 1f
    // ( Swipe progress could be < 0f and > 1f and this may cause some problems )
    val motionProgress = max(Float.min(swipeProgress, 1f), 0f)
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources
            .openRawResource(R.raw.motion_scene)
            .readBytes()
            .decodeToString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Vertical,
                enabled = true,
            ),
    ) {
        MotionLayout(
            motionScene = MotionScene(content = motionSceneContent),
            progress = motionProgress,
            modifier = Modifier
                .fillMaxSize()
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
                        .alpha(1f)
                        .zIndex(1f)
                        .layoutId("thumbnail")
                ),
                contentDescription = ""
            )
        }
    }


//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(MaterialTheme.colors.surface)
//
//    ) {
//        AsyncImage(
//            contentScale = ContentScale.FillBounds,
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(book.thumbnail)
//                .fallback(R.drawable.file_music)
//                .error(R.drawable.file_music)
//                .crossfade(true)
//                .build(),
//            modifier = modifier.then(
//                Modifier
//                    .size(size)
//            ),
//            contentDescription = ""
//        )
//        Column {
//            Text(book.name)
//            Row {
//                Text("Ouvido")
//                Text("Faltando")
//            }
//            Row {
//                Text("Controles")
//                if (book is BookFolder) {
//                    Text("1/${book.bookFileList.size}")
//                }
//
//            }
//        }
//    }
}