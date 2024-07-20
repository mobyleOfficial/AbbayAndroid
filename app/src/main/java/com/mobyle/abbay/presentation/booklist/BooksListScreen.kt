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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.documentfile.provider.DocumentFile
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
import com.mobyle.abbay.presentation.common.mappers.toBook
import com.mobyle.abbay.presentation.common.mappers.toMultipleBooks
import com.mobyle.abbay.presentation.utils.LaunchedEffectAndCollect
import com.mobyle.abbay.presentation.utils.getId
import com.mobyle.abbay.presentation.utils.getTitle
import com.mobyle.abbay.presentation.utils.musicCursor
import com.mobyle.abbay.presentation.utils.playBook
import com.mobyle.abbay.presentation.utils.prepareBook
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.BookFolder
import com.model.MultipleBooks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val pickedDir = DocumentFile.fromTreeUri(context, it)
            val list = pickedDir?.listFiles() ?: emptyArray()
            val audioList = mutableListOf<Book>()
            val folderList = mutableListOf<Book>()
            val audioFolderList = mutableListOf<Book>()

            list.toList().filter { document ->
                document.type?.contains("audio") ?: false || document.type == null
            }.forEach { document ->
                if (document.type?.contains("audio") == true) {
                    val fileUri = document.uri
                    val metadataRetriever = MediaMetadataRetriever()
                    metadataRetriever.setDataSource(context, fileUri)
                    audioList.add(metadataRetriever.toBook(context, fileUri.path!!))
                } else {
                    DocumentFile.fromTreeUri(context, document.uri)?.let { documentsTree ->
                        val audiobooksFromFiles = mutableListOf<BookFile>()
                        val childDocuments = documentsTree.listFiles()
                        for (childDocument in childDocuments) {
                            if (childDocument.type?.contains("audio") == true) {
                                val fileUri = childDocument.uri
                                val metadataRetriever = MediaMetadataRetriever()
                                metadataRetriever.setDataSource(context, fileUri)
                                audiobooksFromFiles.add(
                                    metadataRetriever.toBook(
                                        context,
                                        fileUri.path!!
                                    )
                                )
                            } else if (childDocument.type == null) {
                                childDocument.name?.let { name ->
                                    val folder = BookFolder(
                                        name = name,
                                        id = "",
                                        thumbnail = null
                                    )
                                    folderList.add(folder)
                                }
                            }
                        }

                        audiobooksFromFiles.toMultipleBooks()?.let {
                            audioFolderList.add(it)
                        }
                    }
                }
            }

            /*
            * Como tratar pastas dentro de pastas:
            * O comportamento esperado para tratamento de pasta dentro de pastas é adicionar a
            * possibilidade de navegar para uma nova pasta caso exista OU mostras arquivos de
            * audio.
            * As pastas aninhadas devem se comportar da mesma maneira que se comportam na tela inicial
            * onde devemos tratar 3 casos
            * 1. Apenas audio: mostra o arquivo de audio, se clicar toca;
            * 2. Pasta com arquivos de audio: mostra a pasta de audio, se clicar toca;
            * 3. Pasta com arquivos de audio e pastas: separar os audios das pastas, portanto mostrar
            * uma pasta assim ocmo é mostrado no 2 e as pastas separadamente
            * */

            audioList.addAll(audioFolderList)
            audioList.addAll(folderList)
            viewModel.addAllBookTypes(audioList.toList())
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
            viewModel.setCurrentProgress(player.currentPosition)
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
                        updateProgress = viewModel::setCurrentProgress,
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
                                        viewModel.setCurrentProgress(book.progress)
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
                                            is BookFolder -> {
                                                Text(book.name, color = Color.Yellow)
                                            }

                                            is BookFile, is MultipleBooks -> {
                                                BookItem(
                                                    book = book,
                                                    isSelected = book.id == (selectedBook as? BookFile)?.id,
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

                                                        viewModel.setCurrentProgress(book.progress)
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
                                    openFolderSelector.launch(null)
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
                        is Loading -> {}
                    }
                }
            }
        }
    }
}
