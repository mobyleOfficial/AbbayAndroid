package com.mobyle.abbay.presentation.booklist

import android.app.Activity
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.GenericError
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoPermissionsGranted
import com.mobyle.abbay.presentation.booklist.widgets.BookItem
import com.mobyle.abbay.presentation.booklist.widgets.BookListTopBar
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.common.mappers.toBook
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.utils.LaunchedEffectAndCollect
import com.mobyle.abbay.presentation.utils.audioCursor
import com.mobyle.abbay.presentation.utils.fileExists
import com.mobyle.abbay.presentation.utils.getId
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.playBook
import com.mobyle.abbay.presentation.utils.playMultipleBooks
import com.mobyle.abbay.presentation.utils.prepareBook
import com.mobyle.abbay.presentation.utils.prepareMultipleBooks
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

private const val AUTO_DENIAL_THRESHOLD = 300

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BooksListScreen(
    viewModel: BooksListViewModel = hiltViewModel(),
    player: MediaController,
    navigateToSettings: () -> Unit,
    openAppSettings: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val asyncScope = rememberCoroutineScope()
    val fileFilterList = arrayOf("audio/*")

    // States
    val bottomSheetState = rememberBottomSheetScaffoldState()
    val booksListState by viewModel.uiState.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val hasBookSelected by viewModel.hasBookSelected.collectAsState(false)
    var componentHeight by remember { mutableStateOf(0.dp) }
    val activity = LocalContext.current as Activity
    val permissionRequestAttemptTime = remember { mutableLongStateOf(0) }
    val isGestureDisabled = remember {
        mutableStateOf(true)
    }
    val permissionState = rememberMultiplePermissionsState(
        permissions = viewModel.getPermissionsList(),
        onPermissionsResult = { permissions ->
            if (permissions.entries.all { it.value }) {
                viewModel.getAudiobookList()
            } else if (Date().time - permissionRequestAttemptTime.longValue < AUTO_DENIAL_THRESHOLD) {
                openAppSettings()
            }
        }
    )
    val showErrorDialog = remember { mutableStateOf(false) }

    LaunchedEffectAndCollect(viewModel.booksIdList) {
        asyncScope.launch(Dispatchers.IO) {
            it?.let {
                val booksWithThumbnails = it.mapNotNull { book ->
                    book.getThumb(context)
                }

                if (booksWithThumbnails.isNotEmpty()) {
                    viewModel.addThumbnails(booksWithThumbnails)
                }
            }
        }

    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {
        viewModel.shouldOpenPlayerInStartup()
    }

    LaunchedEffect(viewModel.shouldOpenPlayerInStartup) {
        if (viewModel.shouldOpenPlayerInStartup && selectedBook?.hasError == false) {
            bottomSheetState.bottomSheetState.expand()
            bottomSheetState.bottomSheetState.expand()
        }
    }

    LaunchedEffect(Unit, viewModel.booksList, bottomSheetState.bottomSheetState.isCollapsed) {
        if (viewModel.booksList.isNotEmpty()) {
            val updatedList = viewModel.booksList.map {
                val newBook = when (it) {
                    is MultipleBooks -> {
                        it.copy(hasError = !context.fileExists(it.id))
                    }

                    is BookFile -> {
                        it.copy(hasError = !context.fileExists(it.id))
                    }

                    else -> it
                }

                if (newBook.hasError && selectedBook?.id == newBook.id) {
                    viewModel.updateSelectedBook(newBook)
                }

                newBook
            }

            viewModel.updateBookList(updatedList)
        }
    }

    val playerIcon = remember {
        val icon = if (player.isPlaying) {
            Icons.Default.Pause
        } else {
            Icons.Default.PlayArrow
        }

        mutableStateOf(icon)
    }

    // Launchers
    val openFileSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { filesList ->
        asyncScope.launch(Dispatchers.IO) {
            if (filesList.isNotEmpty()) {
                val newBooksList = filesList.filter { it.path != null }.map { uri ->
                    var id: String? = null
                    val metadataRetriever = MediaMetadataRetriever()
                    metadataRetriever.setDataSource(context, uri)
                    context.audioCursor {
                        id = it.getId()
                    }

                    metadataRetriever.toBook(context, id.orEmpty())
                }

                viewModel.updateBookList(newBooksList)
            }
        }
    }

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.showLoading()
            asyncScope.launch(Dispatchers.IO) {
                delay(500)
                uri.getBooks(context)?.let {
                    viewModel.addAllBookTypes(it)
                }
            }
        }
    }

    // SideEffects
    BackHandler {
        asyncScope.launch {
            if (bottomSheetState.bottomSheetState.isCollapsed) {
                activity.finishAffinity()
            } else {
                bottomSheetState.bottomSheetState.collapse()
            }

        }
    }

    LaunchedEffect(Unit) {
        viewModel.isPlaying.value = player.isPlaying
    }

    LaunchedEffectAndCollect(viewModel.isPlaying) {
        while (it == true) {
            selectedBook?.let {
                if (player.isPlaying) {
                    viewModel.setCurrentProgress(
                        id = it.id,
                        progress = player.currentPosition
                    )
                }
            }

            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                selectedBook?.let { book ->
                    viewModel.markBookAsError(book)
                }
            }
        })
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        selectedBook?.let {
            viewModel.updateBookList(it.id, player.currentPosition)
        }
    }

    LaunchedEffect(selectedBook) {
        if (selectedBook?.hasError == true) {
            showErrorDialog.value = true
            bottomSheetState.bottomSheetState.collapse()
        }

        isGestureDisabled.value = selectedBook?.hasError == false
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetContent = {
            selectedBook?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    MiniPlayer(
                        player = player,
                        book = it,
                        scaffoldState = bottomSheetState,
                        progress = it.progress,
                        playerIcon = playerIcon,
                        onPlayingChange = { isPlaying ->
                            viewModel.isPlaying.value = isPlaying


                            if (!isPlaying) {
                                selectedBook?.let {
                                    viewModel.updateBookProgress(
                                        it.id,
                                        currentProgress
                                    )
                                }
                            }
                        },
                        updateProgress = {
                            selectedBook?.let { book ->
                                viewModel.setCurrentProgress(
                                    id = book.id,
                                    progress = it
                                )
                            }
                        },
                        updateCurrentBookPosition = {
                            selectedBook?.let { book ->
                                viewModel.updateBookPosition(
                                    id = book.id,
                                    position = it
                                )
                            }
                        },
                        onDisableGesture = {
                            isGestureDisabled.value = !it
                        },
                        updateBookSpeed = {
                            selectedBook?.let { book ->
                                viewModel.updateBookSpeed(
                                    id = book.id,
                                    speed = it
                                )
                            }
                        },
                        modifier = Modifier
                            .onGloballyPositioned {
                                componentHeight = with(density) {
                                    it.size.height.toDp()
                                }
                            }
                    )
                }
            }
        },
        sheetGesturesEnabled = isGestureDisabled.value,
        sheetPeekHeight = if (hasBookSelected) 72.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Scaffold(topBar = {
                BookListTopBar(
                    openFolderSelector = {
                        openFolderSelector.launch(null)
                    },
                    openSettings = navigateToSettings,
                    openFileSelector = {
                        openFileSelector.launch(fileFilterList)
                    }
                )
            }) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (val state = booksListState) {
                        is BookListSuccess -> {
                            val bookList = state.audiobookList

                            if (selectedBook == null) {
                                val id = viewModel.getCurrentSelectedBook().orEmpty()

                                bookList.firstOrNull { book ->
                                    book.id == id && !book.hasError
                                }?.let { book ->
                                    viewModel.setCurrentProgress(
                                        id = book.id,
                                        progress = book.progress
                                    )

                                    viewModel.selectBook(book)

                                    if (!player.isPlaying) {
                                        if (book is MultipleBooks) {
                                            player.prepareMultipleBooks(
                                                currentPosition = book.currentBookPosition,
                                                idList = book.bookFileList.map { it.id },
                                                progress = book.progress,
                                                isPlaying = viewModel.isPlaying
                                            )
                                        } else {
                                            player.prepareBook(
                                                id = id,
                                                progress = book.progress,
                                                isPlaying = viewModel.isPlaying
                                            )
                                        }
                                    }
                                }
                            }

                            val bookToDelete = remember { mutableStateOf<Book?>(null) }
                            val showDeleteDialog = remember {
                                mutableStateOf(false)
                            }

                            Column {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    items(
                                        items = bookList,
                                        key = { book -> book.id },
                                        itemContent = { book ->
                                            val dismissState = rememberSwipeToDismissBoxState(
                                                confirmValueChange = { dismissValue ->
                                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                        bookToDelete.value = book
                                                        showDeleteDialog.value = true
                                                    }

                                                    false
                                                }
                                            )

                                            SwipeToDismissBox(
                                                state = dismissState,
                                                enableDismissFromStartToEnd = false,
                                                backgroundContent = {
                                                    val color = MaterialTheme.colorScheme.error
                                                    val alignment = Alignment.CenterEnd
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(color)
                                                            .padding(horizontal = 20.dp),
                                                        contentAlignment = alignment
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = Color.White
                                                        )
                                                    }
                                                },
                                                content = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        when (book) {
                                                            is MultipleBooks -> {
                                                                val intermediaryProgress =
                                                                    book.bookFileList
                                                                        .intermediateProgress(book.currentBookPosition)

                                                                BookItem(
                                                                    book = book,
                                                                    currentMediaIndex = book.currentBookPosition + 1,
                                                                    isSelected = book.id == (selectedBook as? BookFile)?.id,
                                                                    intermediaryProgress = intermediaryProgress,
                                                                    progress = if (book.id == (selectedBook as? BookFile)?.id) {
                                                                        if (isPlaying) {
                                                                            intermediaryProgress.plus(
                                                                                currentProgress
                                                                            ).toHHMMSS()
                                                                        } else {
                                                                            intermediaryProgress.plus(
                                                                                book.progress
                                                                            ).toHHMMSS()
                                                                        }
                                                                    } else {
                                                                        intermediaryProgress.plus(
                                                                            book.progress
                                                                        ).toHHMMSS()
                                                                    }
                                                                ) {
                                                                    if (selectedBook?.id != book.id &&
                                                                        !book.hasError
                                                                    ) {
                                                                        selectedBook?.let {
                                                                            viewModel.updateBookProgress(
                                                                                it.id,
                                                                                currentProgress
                                                                            )
                                                                        }

                                                                        viewModel.setCurrentProgress(
                                                                            id = book.id,
                                                                            progress = book.progress
                                                                        )

                                                                        viewModel.selectBook(book)
                                                                        player.playMultipleBooks(
                                                                            currentPosition = book.currentBookPosition,
                                                                            idList = book.bookFileList.map { it.id },
                                                                            progress = book.progress,
                                                                            isPlaying = viewModel.isPlaying
                                                                        )
                                                                    }

                                                                    if (book.hasError) {
                                                                        showErrorDialog.value = true
                                                                        viewModel.selectBook(null)
                                                                    } else {
                                                                        asyncScope.launch {
                                                                            bottomSheetState.bottomSheetState.expand()
                                                                            bottomSheetState.bottomSheetState.expand()
                                                                        }
                                                                    }

                                                                }
                                                            }

                                                            is BookFile -> {
                                                                BookItem(
                                                                    book = book,
                                                                    currentMediaIndex = player.currentMediaItemIndex + 1,
                                                                    isSelected = book.id == (selectedBook as? BookFile)?.id,
                                                                    intermediaryProgress = 0L,
                                                                    progress = if (book.id == (selectedBook as? BookFile)?.id) {
                                                                        if (isPlaying) {
                                                                            currentProgress.toHHMMSS()
                                                                        } else {
                                                                            book.progress.toHHMMSS()
                                                                        }
                                                                    } else {
                                                                        book.progress.toHHMMSS()
                                                                    }
                                                                ) {
                                                                    if (selectedBook?.id != book.id &&
                                                                        !book.hasError
                                                                    ) {
                                                                        selectedBook?.let {
                                                                            viewModel.updateBookProgress(
                                                                                it.id,
                                                                                currentProgress
                                                                            )
                                                                        }

                                                                        viewModel.setCurrentProgress(
                                                                            id = book.id,
                                                                            progress = book.progress
                                                                        )

                                                                        viewModel.selectBook(
                                                                            book
                                                                        )

                                                                        player.playBook(
                                                                            id = book.id,
                                                                            progress = book.progress,
                                                                            isPlaying = viewModel.isPlaying
                                                                        )
                                                                    }

                                                                    if (book.hasError) {
                                                                        showErrorDialog.value = true
                                                                        viewModel.selectBook(
                                                                            null
                                                                        )
                                                                    } else {
                                                                        asyncScope.launch {
                                                                            bottomSheetState.bottomSheetState.expand()
                                                                            bottomSheetState.bottomSheetState.expand()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                enableDismissFromEndToStart = true,
                                            )
                                        }
                                    )
                                }
                            }

                            if (showDeleteDialog.value) {
                                AbbayActionDialog(
                                    onDismiss = {
                                        showDeleteDialog.value = false
                                        bookToDelete.value = null
                                    },
                                    title = "Delete Book",
                                    body = "Are you sure you want to delete this book?",
                                    actionButtonTitle = "Delete",
                                    onAction = {
                                        bookToDelete.value?.let { book ->
                                            viewModel.removeBook(book)
                                        }
                                        showDeleteDialog.value = false
                                        bookToDelete.value = null
                                    },
                                )
                            }
                        }

                        is NoBookSelected -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Button(onClick = {
                                    openFileSelector.launch(fileFilterList)
                                }) {
                                    Text(
                                        getString(
                                            context,
                                            R.string.no_book_selected_primary_button_title
                                        )
                                    )
                                }

                                Text(getString(context, R.string.no_book_selected_or))

                                Button(onClick = {
                                    asyncScope.launch {
                                        openFolderSelector.launch(null)
                                    }
                                }) {
                                    Text(
                                        text = getString(
                                            context,
                                            R.string.no_book_selected_secondary_button_title
                                        )
                                    )
                                }
                            }

                        }

                        is GenericError -> {}
                        is Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        is NoPermissionsGranted -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    stringResource(id = R.string.no_permission_error_title),
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontSize = 24.sp,
                                    ),
                                )

                                AsyncImage(
                                    contentScale = ContentScale.None,
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(R.drawable.img_no_permissions)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "",
                                    modifier = Modifier.graphicsLayer(
                                        scaleX = 0.6f,
                                        scaleY = 0.5f
                                    )
                                )
                                Button(onClick = {
                                    asyncScope.launch {
                                        permissionRequestAttemptTime.longValue = Date().time
                                        permissionState.launchMultiplePermissionRequest()
                                    }
                                }, modifier = Modifier.padding(bottom = 16.dp)) {
                                    Text(
                                        stringResource(id = R.string.no_permission_error_button_text),
                                        style = TextStyle(
                                            color = Color.White,
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            fontSize = 18.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showErrorDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog.value = false
                viewModel.selectBook(null)
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "File Not Found",
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
                        "The audio file for this book could not be found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        "The file might have been moved or deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog.value = false
                        viewModel.selectBook(null)
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
