package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.utils.currentFraction
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import kotlin.math.max
import kotlin.math.min

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalMotionApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MiniPlayer(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    scaffoldState: BottomSheetScaffoldState,
    playerIcon: MutableState<ImageVector>,
    updateCurrentBookPosition: (Int) -> Unit,
    updateProgress: (Long) -> Unit,
    modifier: Modifier
) {
    if (book is BookFile) {
        SingleFilePlayer(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            scaffoldState = scaffoldState,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            modifier = modifier
        )
    } else if (book is MultipleBooks) {
        MultipleFilePlayer(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            scaffoldState = scaffoldState,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            updateCurrentBookPosition = updateCurrentBookPosition,
            modifier = modifier
        )
    }
}

@Composable
private fun PlayerController(
    player: MediaController,
    position: Long,
    playerIcon: MutableState<ImageVector>,
    onPlayingChange: (Boolean) -> Unit,
) {
    IconButton(onClick = {
        playerIcon.value = if (player.isPlaying) {
            onPlayingChange(false)
            player.pause()
            Icons.Default.PlayArrow
        } else {
            player.seekTo(position)
            onPlayingChange(true)
            player.playWhenReady = true
            Icons.Default.Pause
        }
    }) {
        Icon(playerIcon.value, contentDescription = "", tint = Color.White)
    }
}

@ExperimentalFoundationApi
@ExperimentalMotionApi
@ExperimentalMaterialApi
@Composable
private fun SingleFilePlayer(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    scaffoldState: BottomSheetScaffoldState,
    playerIcon: MutableState<ImageVector>,
    updateProgress: (Long) -> Unit,
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

    var slideValue by remember { mutableFloatStateOf(0f) }

    fun onSliderValueChange(percentage: Float) {
        slideValue = percentage
        updateProgress((book.duration * percentage).toLong())
    }

    MotionLayout(
        motionScene = MotionScene(content = motionSceneContent),
        progress = motionProgress,
        modifier = Modifier
            .fillMaxSize()
    ) {
        MiniPlayerContent(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            modifier = modifier
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("content")
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                book.name,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    lineHeight = 400.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .basicMarquee()
                    .fillMaxWidth()
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Slider(
                    value = progress.toFloat() / book.duration,
                    onValueChange = { percentage ->
                        onSliderValueChange(percentage)
                    },
                    onValueChangeFinished = {
                        val newPosition = (book.duration * slideValue).toLong()
                        player.seekTo(newPosition)
                        updateProgress(newPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        progress.toHHMMSS(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        book.duration.toHHMMSS(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastRewind, contentDescription = "", tint = Color.White)
                }
                PlayerController(
                    player = player,
                    onPlayingChange = onPlayingChange,
                    position = progress,
                    playerIcon = playerIcon
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastForward, contentDescription = "", tint = Color.White)
                }
            }
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

@ExperimentalFoundationApi
@ExperimentalMotionApi
@ExperimentalMaterialApi
@Composable
private fun MultipleFilePlayer(
    player: MediaController,
    book: MultipleBooks,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    scaffoldState: BottomSheetScaffoldState,
    playerIcon: MutableState<ImageVector>,
    updateCurrentBookPosition: (Int) -> Unit,
    updateProgress: (Long) -> Unit,
    modifier: Modifier
) {
    val duration = book.bookFileList[book.currentBookPosition].duration
    val swipeProgress = scaffoldState.currentFraction
    val motionProgress = max(min(swipeProgress, 1f), 0f)
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources
            .openRawResource(R.raw.motion_scene)
            .readBytes()
            .decodeToString()
    }

    var slideValue by remember { mutableFloatStateOf(0f) }
    fun onSliderValueChange(percentage: Float) {
        slideValue = percentage
        updateProgress((duration * percentage).toLong())
    }

    MotionLayout(
        motionScene = MotionScene(content = motionSceneContent),
        progress = motionProgress,
        modifier = Modifier
            .fillMaxSize()
    ) {
        MiniPlayerContent(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            modifier = modifier
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("content")
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                book.name,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    lineHeight = 400.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .basicMarquee()
                    .fillMaxWidth()
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Slider(
                    value = progress.toFloat() / duration,
                    onValueChange = { percentage ->
                        onSliderValueChange(percentage)
                    },
                    onValueChangeFinished = {
                        val newPosition = (duration * slideValue).toLong()
                        player.seekTo(newPosition)
                        updateProgress(newPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        progress.toHHMMSS(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        duration.toHHMMSS(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    if(book.currentBookPosition > 0) {
                        player.seekToPreviousMediaItem()
                        updateProgress(0L)
                        updateCurrentBookPosition(book.currentBookPosition - 1)
                    }
                }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "", tint = Color.White)
                }
                PlayerController(
                    player = player,
                    onPlayingChange = onPlayingChange,
                    position = progress,
                    playerIcon = playerIcon
                )
                IconButton(onClick = {
                    if(book.currentBookPosition < book.bookFileList.size - 1) {
                        player.seekToNextMediaItem()
                        updateProgress(0L)
                        updateCurrentBookPosition(book.currentBookPosition + 1)
                    }

                }) {
                    Icon(Icons.Default.FastForward, contentDescription = "", tint = Color.White)
                }
            }
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

@ExperimentalFoundationApi
@Composable
private fun MiniPlayerContent(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    playerIcon: MutableState<ImageVector>,
    modifier: Modifier
) {
    Row(
        modifier = modifier.then(
            Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .layoutId("miniPlayer")
        ),
    ) {
        val intermediaryProgress = if (book is MultipleBooks) {
            book.bookFileList
                .intermediateProgress(book.currentBookPosition)
        } else {
            0L
        }

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                book.name, style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .basicMarquee()
            )
            Row {
                Text(
                    "${intermediaryProgress.plus(progress).toHHMMSS()}/${book.duration.toHHMMSS()}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        PlayerController(
            player = player,
            onPlayingChange = onPlayingChange,
            position = progress,
            playerIcon = playerIcon
        )
    }
}