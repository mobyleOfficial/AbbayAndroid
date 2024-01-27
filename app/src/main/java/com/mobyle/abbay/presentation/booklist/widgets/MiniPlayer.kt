package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.utils.currentFraction
import com.model.Book
import com.model.BookFolder
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterialApi::class, ExperimentalMotionApi::class)
@Composable
fun MiniPlayer(book: Book, scaffoldState: BottomSheetScaffoldState, modifier: Modifier) {
    val swipeProgress = scaffoldState.currentFraction
    val motionProgress = max(min(swipeProgress, 1f), 0f)
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
    ) {
        MotionLayout(
            motionScene = MotionScene(content = motionSceneContent),
            progress = motionProgress,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = modifier.then(
                    Modifier
                        .layoutId("miniPlayer")
                ),
            ) {
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
                        .layoutId("thumbnail")
                ),
                contentDescription = ""
            )
        }
    }
}