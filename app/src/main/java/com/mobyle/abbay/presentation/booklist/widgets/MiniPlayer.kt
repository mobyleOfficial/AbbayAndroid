package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.models.BookSpeed
import com.mobyle.abbay.presentation.booklist.widgets.models.LayoutId
import com.mobyle.abbay.presentation.common.mappers.toBookSpeed
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.utils.currentFraction
import com.mobyle.abbay.presentation.utils.debounceClick
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.playMultipleBooks
import com.mobyle.abbay.presentation.utils.prepareBook
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalMotionApi::class, ExperimentalFoundationApi::class
)
@Composable
fun MiniPlayer(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    isScreenLocked: Boolean,
    scaffoldState: BottomSheetScaffoldState,
    updateCurrentBookPosition: (Int) -> Unit,
    updateProgress: (Long) -> Unit,
    updateBookSpeed: (Float) -> Unit,
    onLockScreen: (Boolean) -> Unit,
    modifier: Modifier
) {
    val showUnlockDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val playerIcon = remember {
        val state = if (player.isPlaying) {
            PlayingState.PLAYING
        } else {
            PlayingState.PAUSED
        }

        mutableStateOf(state)
    }

    // Keep player icon in sync with actual player state
    LaunchedEffect(player.isPlaying, player.playbackState) {
        playerIcon.value = if (player.isPlaying && player.playbackState == Player.STATE_READY) {
            PlayingState.PLAYING
        } else {
            PlayingState.PAUSED
        }
    }

    // Add player listener to observe playback state changes
    LaunchedEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (player.isPlaying) {
                            playerIcon.value = PlayingState.PLAYING
                        } else {
                            playerIcon.value = PlayingState.PAUSED
                        }

                        onPlayingChange(player.isPlaying)
                    }

                    Player.STATE_BUFFERING -> {
                        // Keep current icon during buffering
                    }

                    Player.STATE_ENDED -> {
                        playerIcon.value = PlayingState.PAUSED
                        onPlayingChange(false)
                    }

                    Player.STATE_IDLE -> {
                        playerIcon.value = PlayingState.PAUSED
                        onPlayingChange(false)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player.playbackState == Player.STATE_READY) {
                    playerIcon.value = if (isPlaying) {
                        PlayingState.PLAYING
                    } else {
                        PlayingState.PAUSED
                    }

                    onPlayingChange(isPlaying)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onLockScreen(false)
                scope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }
        }

        player.addListener(listener)
    }

    if (book is BookFile) {
        SingleFilePlayer(
            player = player,
            book = book,
            isScreenLocked = isScreenLocked,
            onPlayingChange = onPlayingChange,
            progress = progress,
            scaffoldState = scaffoldState,
            playerIcon = playerIcon,
            onLockScreen = {
                onLockScreen(true)
            },
            showScreenLockedAlert = {
                showUnlockDialog.value = true
            },
            unlockScreen = {
                onLockScreen(false)
            },
            updateProgress = updateProgress,
            updateBookSpeed = updateBookSpeed,
            modifier = modifier
        )
    } else if (book is MultipleBooks) {
        MultipleFilePlayer(
            player = player,
            book = book,
            isScreenLocked = isScreenLocked,
            onPlayingChange = onPlayingChange,
            progress = progress,
            scaffoldState = scaffoldState,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            onLockScreen = {
                onLockScreen(true)
            },
            showScreenLockedAlert = {
                showUnlockDialog.value = true
            },
            unlockScreen = {
                onLockScreen(false)
            },
            updateCurrentBookPosition = updateCurrentBookPosition,
            updateBookSpeed = updateBookSpeed,
            modifier = modifier
        )
    }

    if (showUnlockDialog.value) {
        ScreenLockedAlert {
            showUnlockDialog.value = false
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMotionApi
@ExperimentalMaterialApi
@Composable
private fun SingleFilePlayer(
    player: MediaController,
    book: Book,
    isScreenLocked: Boolean,
    progress: Long,
    scaffoldState: BottomSheetScaffoldState,
    playerIcon: MutableState<PlayingState>,
    unlockScreen: () -> Unit,
    showScreenLockedAlert: () -> Unit,
    onLockScreen: (Boolean) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    updateProgress: (Long) -> Unit,
    updateBookSpeed: (Float) -> Unit,
    modifier: Modifier
) {
    val swipeProgress = scaffoldState.currentFraction
    val motionProgress = max(min(swipeProgress, 1f), 0f)
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources.openRawResource(R.raw.motion_scene).readBytes().decodeToString()
    }
    val scope = rememberCoroutineScope()

    MotionLayout(
        motionScene = MotionScene(content = motionSceneContent),
        progress = motionProgress,
        modifier = Modifier.fillMaxSize()
    ) {
        MiniPlayerContent(
            player = player,
            book = book,
            isScreenLocked = isScreenLocked,
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
                .layoutId(LayoutId.CONTENT.id)
                .padding(horizontal = 28.dp)
                .padding(bottom = 30.dp, top = 16.dp),
        )

        Box(
            Modifier
                .padding(8.dp)
                .layoutId(LayoutId.THUMBNAIL.id)
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
                .layoutId(LayoutId.TOP_CONTENT.id),
        ) {
            BooksTopBar(
                book = book,
                isScreenLocked = isScreenLocked,
                onCollapseMiniPlayer = {
                    scope.launch {
                        scaffoldState.bottomSheetState.collapse()
                    }
                },
                onSpeedChange = {
                    player.playbackParameters = player.playbackParameters.withSpeed(it)
                    updateBookSpeed(it)
                },
                onLockToggle = { onLockScreen(it) }
            )
        }

        Box(
            modifier = Modifier.layoutId(LayoutId.LOCK_OVERLAY.id)
        ) {
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .combinedClickable(
                            onClick = {
                                showScreenLockedAlert()
                            },
                            onDoubleClick = {
                                unlockScreen()
                            }
                        )
                )
            }
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
    isScreenLocked: Boolean,
    progress: Long,
    scaffoldState: BottomSheetScaffoldState,
    playerIcon: MutableState<PlayingState>,
    unlockScreen: () -> Unit,
    showScreenLockedAlert: () -> Unit,
    onLockScreen: (Boolean) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    updateCurrentBookPosition: (Int) -> Unit,
    updateProgress: (Long) -> Unit,
    updateBookSpeed: (Float) -> Unit,
    modifier: Modifier
) {
    val swipeProgress = scaffoldState.currentFraction
    val motionProgress = max(min(swipeProgress, 1f), 0f)
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources.openRawResource(R.raw.motion_scene).readBytes().decodeToString()
    }
    val scope = rememberCoroutineScope()
    val showChapters = remember { mutableStateOf(false) }
    val files = book.bookFileList
    val currentIndex = book.currentBookPosition
    val duration = book.bookFileList[book.currentBookPosition].duration

    LaunchedEffect(swipeProgress) {
        if (swipeProgress < 1f) {
            showChapters.value = false
        }
    }

    MotionLayout(
        motionScene = MotionScene(content = motionSceneContent),
        progress = motionProgress,
        modifier = Modifier.fillMaxSize()
    ) {
        MiniPlayerContent(
            player = player,
            book = book,
            isScreenLocked = isScreenLocked,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            modifier = modifier
        )

        PlayerControls(
            player = player,
            book = book,
            receivedDuration = duration,
            onPlayingChange = onPlayingChange,
            progress = progress,
            playerIcon = playerIcon,
            updateProgress = updateProgress,
            modifier = Modifier
                .fillMaxWidth()
                .layoutId(LayoutId.CONTENT.id)
                .padding(horizontal = 28.dp)
                .padding(bottom = 30.dp, top = 16.dp),
        )

        LaunchedEffect(player.currentMediaItemIndex) {
            if (player.playWhenReady) {
                val index = player.currentMediaItemIndex
                updateCurrentBookPosition(index)
            }
        }

        Box(
            Modifier
                .padding(8.dp)
                .layoutId(LayoutId.THUMBNAIL.id)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .clip(RoundedCornerShape(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                AnimatedVisibility(
                    visible = swipeProgress == 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    BookFileItem(
                        file = files.getOrNull(currentIndex),
                        onClick = {
                            showChapters.value = !showChapters.value
                        },
                        isExpanded = showChapters.value
                    )
                }

                AnimatedVisibility(visible = showChapters.value && swipeProgress == 1f) {
                    BookFilesList(
                        files = files,
                        currentIndex = currentIndex,
                        onClick = { index ->
                            showChapters.value = false
                            updateCurrentBookPosition(index)
                            player.seekTo(index, 0L)
                            updateProgress(0L)
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId(LayoutId.TOP_CONTENT.id),
        ) {
            BooksTopBar(book = book, isScreenLocked = isScreenLocked, onCollapseMiniPlayer = {
                scope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }, onSpeedChange = {
                player.playbackParameters = player.playbackParameters.withSpeed(it)
                updateBookSpeed(it)
            }, onLockToggle = { onLockScreen(it) })
        }

        Box(
            modifier = Modifier.layoutId(LayoutId.LOCK_OVERLAY.id)
        ) {
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .combinedClickable(
                            onClick = {
                                showScreenLockedAlert()
                            },
                            onDoubleClick = {
                                unlockScreen()
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun BookFileItem(
    file: BookFile?,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .debounceClick { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = file?.fileName ?: stringResource(R.string.unknown_file),
            style = AbbayTextStyles.chapterTitle,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) stringResource(R.string.hide_chapters) else stringResource(
                R.string.show_chapters
            ),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BookFilesList(
    files: List<BookFile>,
    currentIndex: Int,
    onClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(currentIndex)
    }

    LazyColumn(
        state = listState, modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
    ) {
        itemsIndexed(files) { index, file ->
            val isSelected = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .debounceClick { onClick(index) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        ), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = AbbayTextStyles.buttonTextMedium,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    )
                    Text(
                        text = file.duration.toHHMMSS(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerController(
    book: Book,
    player: MediaController,
    progress: Long,
    playerIcon: MutableState<PlayingState>,
    onPlayingChange: (Boolean) -> Unit,
) {
    IconButton(
        onClick = {
            if (playerIcon.value != PlayingState.LOADING) {
                if (player.isPlaying) {
                    onPlayingChange(false)
                    player.pause()
                } else {
                    when (book) {
                        is MultipleBooks -> {
                            player.playMultipleBooks(
                                currentPosition = book.currentBookPosition,
                                idList = book.bookFileList.map { it.id },
                                progress = book.progress,
                                isPlaying = MutableStateFlow(true)
                            )
                        }

                        is BookFile -> {
                            player.prepareBook(
                                id = book.id,
                                progress = progress,
                                isPlaying = MutableStateFlow(true)
                            )
                        }
                    }

                    playerIcon.value = PlayingState.LOADING

                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                player.seekTo(progress)
                                onPlayingChange(true)
                                player.play()
                                player.removeListener(this)
                            }
                        }
                    })
                }
            }
        }
    ) {
        playerIcon.value.toIcon()?.let {
            Icon(it, contentDescription = "", tint = Color.White)
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier.background(Color.Transparent),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun BooksTopBar(
    book: Book,
    isScreenLocked: Boolean,
    onCollapseMiniPlayer: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onLockToggle: (Boolean) -> Unit,
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

    TopAppBar(backgroundColor = MaterialTheme.colorScheme.surface, title = {

    }, navigationIcon = {
        IconButton(onClick = onCollapseMiniPlayer) {
            Icon(
                Icons.Default.ChevronLeft, contentDescription = "", tint = Color.White
            )
        }
    }, actions = {
        IconButton(onClick = { speedMenuExpanded.value = true }) {
            Row(
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSpeed.value.text, color = Color.White
                )
                Icon(
                    Icons.Default.Speed,
                    contentDescription = stringResource(R.string.change_speed_content_description),
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = speedMenuExpanded.value,
            onDismissRequest = { speedMenuExpanded.value = false }) {
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
            onLockToggle(!isScreenLocked)
        }) {
            Icon(
                if (isScreenLocked) {
                    Icons.Default.LockOpen
                } else {
                    Icons.Default.Lock
                }, contentDescription = "", tint = Color.White
            )
        }
    })
}

@ExperimentalFoundationApi
@Composable
private fun MiniPlayerContent(
    player: MediaController,
    book: Book,
    onPlayingChange: (Boolean) -> Unit,
    progress: Long,
    playerIcon: MutableState<PlayingState>,
    isScreenLocked: Boolean,
    modifier: Modifier
) {
    Row(
        modifier = modifier.then(
            Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .layoutId(LayoutId.MINI_PLAYER.id)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val intermediaryProgress = if (book is MultipleBooks) {
            book.bookFileList.intermediateProgress(book.currentBookPosition)
        } else {
            0L
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                book.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Row {
                Text(
                    "${intermediaryProgress.plus(progress).toHHMMSS()}/${book.duration.toHHMMSS()}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        if (isScreenLocked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "",
                tint = Color.White
            )
        } else {
            PlayerController(
                book = book,
                player = player,
                onPlayingChange = onPlayingChange,
                progress = progress,
                playerIcon = playerIcon
            )
        }
    }
}

@Composable
private fun BookImage(
    modifier: Modifier,
    progress: Long,
    player: MediaController,
    book: Book,
    playerIcon: MutableState<PlayingState>,
    onPlayingChange: (Boolean) -> Unit
) {
    AsyncImage(
        contentScale = ContentScale.FillBounds,
        model = ImageRequest.Builder(LocalContext.current).data(book.thumbnail)
            .fallback(R.drawable.file_music)
            .error(R.drawable.file_music)
            .crossfade(true).build(),
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
            .debounceClick {
                if (playerIcon.value != PlayingState.LOADING) {
                    if (player.isPlaying) {
                        onPlayingChange(false)
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_READY) {
                            player.seekTo(progress)
                            onPlayingChange(true)
                            player.play()
                        } else {
                            when (book) {
                                is MultipleBooks -> {
                                    player.playMultipleBooks(
                                        currentPosition = book.currentBookPosition,
                                        idList = book.bookFileList.map { it.id },
                                        progress = book.progress,
                                        isPlaying = MutableStateFlow(true)
                                    )
                                }

                                is BookFile -> {
                                    player.prepareBook(
                                        id = book.id,
                                        progress = progress,
                                        isPlaying = MutableStateFlow(true)
                                    )
                                }
                            }

                            playerIcon.value = PlayingState.LOADING

                            player.addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        player.seekTo(progress)
                                        onPlayingChange(true)
                                        player.play()
                                        player.removeListener(this)
                                    }
                                }
                            })
                        }
                    }
                }
            }
            .clip(shape = RoundedCornerShape(percent = 10))) {
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            playerIcon.value.toIcon()?.let {
                Icon(it, contentDescription = "", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    player: MediaController,
    book: Book,
    receivedDuration: Long? = null,
    progress: Long,
    playerIcon: MutableState<PlayingState>,
    onPlayingChange: (Boolean) -> Unit,
    updateProgress: (Long) -> Unit,
    modifier: Modifier
) {
    var slideValue by remember { mutableFloatStateOf(0f) }
    val duration = receivedDuration ?: book.duration
    fun onSliderValueChange(percentage: Float) {
        slideValue = percentage
        updateProgress((duration * percentage).toLong())
    }

    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
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
                value = progress.toFloat() / duration, onValueChange = { percentage ->
                    onSliderValueChange(percentage)
                }, onValueChangeFinished = {
                    val newPosition = (duration * slideValue).toLong()
                    player.seekTo(newPosition)
                    updateProgress(newPosition)
                }, colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                ), modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    progress.toHHMMSS(), style = MaterialTheme.typography.titleSmall
                )
                Text(
                    duration.toHHMMSS(), style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    val newPosition = (progress - 30_000L).coerceAtLeast(0L)
                    player.seekTo(newPosition)
                    updateProgress(newPosition)
                }
            ) {
                Icon(Icons.Default.Replay30, contentDescription = "", tint = Color.White)
            }
            IconButton(
                onClick = {
                    val forwardTo = (progress - 10_000L).coerceAtMost(duration)
                    player.seekTo(forwardTo)
                    updateProgress(forwardTo)
                }
            ) {
                Icon(Icons.Default.Replay10, contentDescription = "", tint = Color.White)
            }

            PlayerController(
                book = book,
                player = player,
                onPlayingChange = onPlayingChange,
                progress = progress,
                playerIcon = playerIcon,
            )

            IconButton(
                onClick = {
                    val forwardTo = (progress + 10_000L).coerceAtMost(duration)
                    player.seekTo(forwardTo)
                    updateProgress(forwardTo)
                }
            ) {
                Icon(Icons.Default.Forward10, contentDescription = "", tint = Color.White)
            }

            IconButton(
                onClick = {
                    val forwardTo = (progress + 30_000L).coerceAtMost(duration)
                    player.seekTo(forwardTo)
                    updateProgress(forwardTo)
                }
            ) {
                Icon(Icons.Default.Forward30, contentDescription = "", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ScreenLockedAlert(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    stringResource(R.string.screen_locked),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.screen_locked_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    stringResource(R.string.screen_locked_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            // No confirm button, unlock is by long press
        },
        dismissButton = {
            Text(
                stringResource(R.string.dismiss),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .debounceClick { onDismissRequest() }
                    .padding(8.dp))
        }
    )
}

private enum class PlayingState {
    PAUSED,
    PLAYING,
    LOADING
}

private fun PlayingState.toIcon(): ImageVector? {
    return when (this) {
        PlayingState.PLAYING -> Icons.Default.Pause
        PlayingState.PAUSED -> Icons.Default.PlayArrow
        PlayingState.LOADING -> null
    }
}