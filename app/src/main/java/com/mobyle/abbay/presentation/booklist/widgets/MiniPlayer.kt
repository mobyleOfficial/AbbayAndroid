package com.mobyle.abbay.presentation.booklist.widgets

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mobyle.abbay.presentation.booklist.widgets.models.BookSpeed
import com.mobyle.abbay.presentation.common.mappers.toBookSpeed
import com.mobyle.abbay.presentation.utils.currentFraction
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import kotlinx.coroutines.launch
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
    updateBookSpeed: (Float) -> Unit,
    isGestureDisabled: (Boolean) -> Unit,
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
            updateBookSpeed = updateBookSpeed,
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
            isGestureDisabled = isGestureDisabled,
            updateBookSpeed = updateBookSpeed,
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
    updateBookSpeed: (Float) -> Unit,
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
    val scope = rememberCoroutineScope()

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

        PlayerControls(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("content")
                .padding(bottom = 30.dp, top = 16.dp),
        )

        Box(
            Modifier
                .padding(8.dp)
                .layoutId("thumbnail")
                .clip(shape = RoundedCornerShape(percent = 10)),
        ) {
            BookImage(
                modifier = modifier,
                player = player,
                playerIcon = playerIcon,
                book = book,
                progress = progress,
                onPlayingChange = onPlayingChange,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("topContent"),
        ) {
            BooksTopBar(
                book = book,
                onCollapseMiniPlayer = {
                    scope.launch {
                        scaffoldState.bottomSheetState.collapse()
                    }
                },
                onSpeedChange = {
                    player.playbackParameters = player.playbackParameters.withSpeed(it)
                    updateBookSpeed(it)
                }
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
    isGestureDisabled: (Boolean) -> Unit,
    updateBookSpeed: (Float) -> Unit,
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
    val scope = rememberCoroutineScope()
    val showChapters = remember { mutableStateOf(false) }
    val chapters = book.bookFileList
    val currentIndex = book.currentBookPosition

    LaunchedEffect(showChapters.value) {
        isGestureDisabled(showChapters.value)
    }

    LaunchedEffect(swipeProgress) {
        if (swipeProgress < 1f) {
            showChapters.value = false
        }
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

        PlayerControls(
            player = player,
            book = book,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("content")
                .padding(horizontal = 28.dp)
                .padding(bottom = 30.dp, top = 16.dp),
        )

        Box(
            Modifier
                .padding(8.dp)
                .layoutId("thumbnail")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (swipeProgress == 1f) {
                    Text(
                        text = chapters.getOrNull(currentIndex)?.name ?: "Unknown Chapter",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChapters.value = !showChapters.value }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                if (showChapters.value && swipeProgress == 1f) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        itemsIndexed(chapters) { index, chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChapters.value = false
                                        updateCurrentBookPosition(index)
                                        player.seekTo(index, 0L)
                                        updateProgress(0L)
                                    }
                                    .background(
                                        if (index == currentIndex)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = chapter.duration.toHHMMSS(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape = RoundedCornerShape(percent = 10)),
                ) {
                    BookImage(
                        modifier = modifier,
                        player = player,
                        playerIcon = playerIcon,
                        book = book,
                        progress = progress,
                        onPlayingChange = onPlayingChange,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("topContent"),
        ) {
            BooksTopBar(
                book = book,
                onCollapseMiniPlayer = {
                    scope.launch {
                        scaffoldState.bottomSheetState.collapse()
                    }
                },
                onSpeedChange = {
                    player.playbackParameters = player.playbackParameters.withSpeed(it)
                    updateBookSpeed(it)
                }
            )
        }
    }
}

@Composable
private fun BooksTopBar(
    book: Book,
    onCollapseMiniPlayer: () -> Unit,
    onSpeedChange: (Float) -> Unit,
) {
    val speedOptions = listOf(
        BookSpeed.Half,
        BookSpeed.Normal,
        BookSpeed.OnePointTwoFive,
        BookSpeed.OnePointFive,
        BookSpeed.Double
    )
    val speedMenuExpanded = remember { mutableStateOf(false) }
    val currentSpeed = remember { mutableStateOf(book.speed.toBookSpeed()) }

    LaunchedEffect(book.id) {
        currentSpeed.value = book.speed.toBookSpeed()
        onSpeedChange(currentSpeed.value.speed)
    }

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.surface,
        title = {

        },
        navigationIcon = {
            IconButton(onClick = onCollapseMiniPlayer) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = { speedMenuExpanded.value = true }) {
                Row {
                    Text(
                        text = currentSpeed.value.text,
                        color = Color.White
                    )
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "Change speed",
                        tint = Color.White
                    )
                }
            }
            DropdownMenu(
                expanded = speedMenuExpanded.value,
                onDismissRequest = { speedMenuExpanded.value = false }
            ) {
                speedOptions.forEach { speedModel ->
                    DropdownMenuItem(onClick = {
                        currentSpeed.value = speedModel
                        speedMenuExpanded.value = false
                        onSpeedChange(speedModel.speed)
                    }, text = {
                        Text(
                            text = speedModel.text,
                            color = if (currentSpeed.value == speedModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    })
                }
            }

            IconButton(onClick = {
                // Change volume
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "",
                    tint = Color.White
                )
            }

            IconButton(onClick = {
                // Lock controllers
            }) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "",
                    tint = Color.White
                )
            }
        }
    )
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

@Composable
private fun BookImage(
    modifier: Modifier,
    progress: Long,
    player: MediaController,
    book: Book,
    playerIcon: MutableState<ImageVector>,
    onPlayingChange: (Boolean) -> Unit
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable {
                playerIcon.value = if (player.isPlaying) {
                    onPlayingChange(false)
                    player.pause()
                    Icons.Default.Pause
                } else {
                    player.seekTo(progress)
                    onPlayingChange(true)
                    player.playWhenReady = true
                    Icons.Default.PlayArrow
                }
            }
            .clip(shape = RoundedCornerShape(percent = 10))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            playerIcon.value = if (player.isPlaying) {
                Icons.Default.Pause
            } else {
                Icons.Default.PlayArrow
            }

            Icon(playerIcon.value, contentDescription = "", tint = Color.White)
        }
    }
}

@Composable
private fun PlayerControls(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    playerIcon: MutableState<ImageVector>,
    updateProgress: (Long) -> Unit,
    modifier: Modifier
) {
    var slideValue by remember { mutableFloatStateOf(0f) }

    fun onSliderValueChange(percentage: Float) {
        slideValue = percentage
        updateProgress((book.duration * percentage).toLong())
    }

    Column(
        modifier = modifier,
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
}