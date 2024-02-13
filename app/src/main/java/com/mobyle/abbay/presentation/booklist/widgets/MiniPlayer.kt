package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.utils.currentFraction
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFolder
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterialApi::class, ExperimentalMotionApi::class)
@Composable
fun MiniPlayer(
    player: ExoPlayer,
    book: Book,
    scaffoldState: BottomSheetScaffoldState,
    modifier: Modifier
) {
    val swipeProgress = scaffoldState.currentFraction
    val motionProgress = max(min(swipeProgress, 1f), 0f)
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources
            .openRawResource(R.raw.motion_scene)
            .readBytes()
            .decodeToString()
    }
    var playerIcon = remember {
        mutableStateOf(Icons.Default.Pause)
    }

    MotionLayout(
        motionScene = MotionScene(content = motionSceneContent),
        progress = motionProgress,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = modifier.then(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .layoutId("miniPlayer")
            ),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(book.name)
                Row {
                    Text("00:18:43/${book.duration.toHHMMSS()}")
                    if (book is BookFolder) {
                        Text("1/${book.bookFileList.size}")
                    }
                }
            }

            PlayerController(player = player, playerIcon = playerIcon)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("content")
        ) {
            Text(book.name)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastRewind, contentDescription = "")
                }
                PlayerController(player = player, playerIcon = playerIcon)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastForward, contentDescription = "")
                }
            }
            Text("Tempo")
        }

        Box(
            Modifier
                .padding(8.dp)
                .layoutId("thumbnail")
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
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(percent = 10))
                ),
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun PlayerController(player: ExoPlayer, playerIcon: MutableState<ImageVector>) {
    IconButton(onClick = {
        playerIcon.value = if (player.isPlaying) {
            player.pause()
            Icons.Default.PlayArrow
        } else {
            player.playWhenReady = true
            Icons.Default.Pause
        }
    }) {
        Icon(playerIcon.value, contentDescription = "")
    }
}
