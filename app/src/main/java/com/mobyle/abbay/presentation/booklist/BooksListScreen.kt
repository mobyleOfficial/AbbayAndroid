package com.mobyle.abbay.presentation.booklist

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment.getExternalStorageDirectory
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.session.MediaController
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.GenericError
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.mobyle.abbay.presentation.booklist.widgets.BookItem
import com.mobyle.abbay.presentation.booklist.widgets.BookListTopBar
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.common.mappers.getThumbnail
import com.mobyle.abbay.presentation.common.mappers.toBook
import com.mobyle.abbay.presentation.common.mappers.toMultipleBooks
import com.mobyle.abbay.presentation.utils.LaunchedEffectAndCollect
import com.mobyle.abbay.presentation.utils.getDuration
import com.mobyle.abbay.presentation.utils.getId
import com.mobyle.abbay.presentation.utils.getTitle
import com.mobyle.abbay.presentation.utils.musicCursor
import com.mobyle.abbay.presentation.utils.playBook
import com.mobyle.abbay.presentation.utils.playMultipleBooks
import com.mobyle.abbay.presentation.utils.prepareBook
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.BookFile
import com.model.MultipleBooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


private const val LAST_SELECTED_BOOK_ID = "LAST_SELECTED_BOOK_ID"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BooksListScreen(player: MediaController) {
    val viewModel: BooksListViewModel = hiltViewModel()
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

    LaunchedEffectAndCollect(viewModel.booksIdList) {
        asyncScope.launch(Dispatchers.IO) {
            it?.let {
                val booksWithThumbnails = it.map {
                    val metadataRetriever = MediaMetadataRetriever()
                    metadataRetriever.setDataSource(
                        context,
                        Uri.parse("content://media/external/audio/media/${it?.id}")
                    )
                    val thumb = metadataRetriever.getThumbnail(context, it?.id.orEmpty())
                    when (it) {
                        is MultipleBooks -> it.copy(thumbnail = thumb)
                        is BookFile -> it.copy(thumbnail = thumb)
                        else -> it
                    }
                }.filterNotNull()

                if (booksWithThumbnails.isNotEmpty()) {
                    viewModel.addThumbnails(booksWithThumbnails)
                }
            }
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
        if (filesList.isNotEmpty()) {
            viewModel.updateBookList(filesList.filter { it.path != null }.map { uri ->
                var id: String? = null
                val metadataRetriever = MediaMetadataRetriever()
                metadataRetriever.setDataSource(context, uri)
                context.musicCursor {
                    val title = it.getTitle()
                    if ((uri.path?.split("/")?.lastOrNull()) == title.orEmpty()) {
                        id = it.getId()
                    }
                }

                metadataRetriever.toBook(context, id.orEmpty())
            })
        }
    }

    fun resolveContentUri(uri: Uri): String {

        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        val docCursor = context.contentResolver.query(docUri, null, null, null, null)

        var str: String = ""

        // get a string of the form : primary:Audiobooks or 1407-1105:Audiobooks
        while (docCursor!!.moveToNext()) {
            str = docCursor.getString(0)
            if (str.matches(Regex(".*:.*"))) break //Maybe useless
        }

        docCursor.close()

        val split = str.split(":")

        val base: File =
            if (split[0] == "primary") getExternalStorageDirectory()
            else File("/storage/${split[0]}")

        if (!base.isDirectory) throw Exception("'$uri' cannot be resolved in a valid path")

        return File(base, split[1]).canonicalPath
    }

    fun getBooksFromUri(uri: Uri?) {
        uri?.let {
            val folderPath = resolveContentUri(uri)
            val contentResolver: ContentResolver = context.contentResolver
            val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val songCursor = contentResolver.query(songUri, null, null, null, null)
            if (songCursor != null && songCursor.moveToFirst()) {
                val filesHashMap = mutableMapOf<String, List<BookFile>>()

                do {
                    songCursor.getColumnIndex(MediaStore.Audio.Media.DATA).let {
                        if (it != -1) {
                            val filePath = songCursor.getString(it)

                            if (filePath.contains(folderPath)) {
                                val id = songCursor.getId().orEmpty()
                                val title = songCursor.getTitle().orEmpty()
                                val progress = 0L
                                val duration = songCursor.getDuration() ?: 0L
                                val fileFolderPath =
                                    songCursor.getString(it).substringBeforeLast("/")
                                val thumbnail = null

                                val book = BookFile(
                                    id = id,
                                    name = title,
                                    thumbnail = thumbnail,
                                    progress = progress,
                                    duration = duration
                                )

                                filesHashMap[fileFolderPath]?.let {
                                    val newList = it.toMutableList()
                                    newList.add(book)
                                    filesHashMap[fileFolderPath] = newList.toList()
                                } ?: run {
                                    filesHashMap[fileFolderPath] = listOf(book)
                                }
                            }
                        }
                    }
                } while (songCursor.moveToNext())
                songCursor.close()


                val filesList = filesHashMap.mapValues {
                    if (it.value.size == 1) {
                        it.value.first()
                    } else {
                        it.value.toMultipleBooks()
                    }
                }.values.toList().filterNotNull()

                viewModel.addAllBookTypes(filesList.toList())
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
                getBooksFromUri(uri)
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
                viewModel.setCurrentProgress(
                    id = it.id,
                    progress = player.currentPosition
                )
            }

            delay(1000)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        selectedBook?.let {
            viewModel.updateBookList(it.id, player.currentPosition)
        }
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
                        progress = currentProgress,
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
                                    position = player.currentMediaItemIndex
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
                    openSettings = {},
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
                                activity.getPreferences(Context.MODE_PRIVATE)?.let {
                                    val id =
                                        it.getString(LAST_SELECTED_BOOK_ID, "").orEmpty()

                                    bookList.firstOrNull { book ->
                                        book.id == id
                                    }?.let { book ->
                                        viewModel.setCurrentProgress(
                                            id = book.id,
                                            progress = book.progress
                                        )
                                        viewModel.selectBook(book)

                                        if (!player.isPlaying) {
                                            player.prepareBook(
                                                id,
                                                book.progress,
                                                viewModel.isPlaying
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    items(bookList.size) { index ->
                                        when (val book = bookList[index]) {
                                            is MultipleBooks -> {
                                                val intermediaryProgress = book.bookFileList.map { it.duration }.subList(0, book.currentBookPosition).sum()

                                                BookItem(
                                                    book = book,
                                                    currentMediaIndex = book.currentBookPosition + 1,
                                                    isSelected = book.id == (selectedBook as? BookFile)?.id,
                                                    intermediaryProgress = intermediaryProgress,
                                                    progress = if (book.id == (selectedBook as? BookFile)?.id) {
                                                        if (isPlaying) {
                                                            intermediaryProgress.plus(currentProgress).toHHMMSS()
                                                        } else {
                                                            intermediaryProgress.plus(book.progress).toHHMMSS()
                                                        }
                                                    } else {
                                                        intermediaryProgress.plus(book.progress).toHHMMSS()
                                                    }
                                                ) {
                                                    if (selectedBook?.id != book.id) {
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
                                                        activity.getPreferences(Context.MODE_PRIVATE)
                                                            ?.let { sharedPref ->
                                                                with(sharedPref.edit()) {
                                                                    putString(
                                                                        LAST_SELECTED_BOOK_ID,
                                                                        book.id
                                                                    )
                                                                    apply()
                                                                }
                                                            }

                                                        viewModel.selectBook(book)
                                                        player.playMultipleBooks(
                                                            book.bookFileList.map { it.id },
                                                            book.progress,
                                                            viewModel.isPlaying
                                                        )
                                                    }

                                                    asyncScope.launch {
                                                        bottomSheetState.bottomSheetState.expand()
                                                        bottomSheetState.bottomSheetState.expand()
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
                                                    if (selectedBook?.id != book.id) {
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
                                                        activity.getPreferences(Context.MODE_PRIVATE)
                                                            ?.let { sharedPref ->
                                                                with(sharedPref.edit()) {
                                                                    putString(
                                                                        LAST_SELECTED_BOOK_ID,
                                                                        book.id
                                                                    )
                                                                    apply()
                                                                }
                                                            }

                                                        viewModel.selectBook(book)
                                                        player.playBook(
                                                            book.id,
                                                            book.progress,
                                                            viewModel.isPlaying
                                                        )
                                                    }

                                                    asyncScope.launch {
                                                        bottomSheetState.bottomSheetState.expand()
                                                        bottomSheetState.bottomSheetState.expand()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                    }
                }
            }
        }
    }
}
