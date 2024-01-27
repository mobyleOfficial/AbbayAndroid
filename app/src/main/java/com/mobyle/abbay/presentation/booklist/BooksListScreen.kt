package com.mobyle.abbay.presentation.booklist

import android.media.MediaMetadataRetriever
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
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.GenericError
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.mobyle.abbay.presentation.booklist.widgets.BookItem
import com.mobyle.abbay.presentation.booklist.widgets.BookListTopBar
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.common.mappers.toBook
import com.mobyle.abbay.presentation.common.mappers.toFolder
import com.model.BookFile
import com.model.BookFolder

@ExperimentalMaterialApi
@Composable
fun BooksListScreen() {
    val viewModel: BooksListViewModel = hiltViewModel()
    val context = LocalContext.current
    val density = LocalDensity.current
    val asyncScope = rememberCoroutineScope()
    val fileFilterList = arrayOf("audio/*")

    // States
    val bottomSheetState = rememberBottomSheetScaffoldState()
    val booksListState by viewModel.uiState.collectAsState()
    var componentHeight by remember { mutableStateOf(0.dp) }
    var hasBookSelected by remember { mutableStateOf(false) }
    var selectedBookIndex by remember { mutableIntStateOf(-1) }

    // Launchers
    val openFileSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { filesList ->
        if (filesList.isNotEmpty()) {
            viewModel.addBooksList(filesList.filter { it.path != null }.map { uri ->
                val metadataRetriever = MediaMetadataRetriever()
                metadataRetriever.setDataSource(context, uri)
                metadataRetriever.toBook(uri.path!!)
            })
        }
    }

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val pickedDir = DocumentFile.fromTreeUri(context, it)
            val list = pickedDir?.listFiles() ?: emptyArray()
            val bookFolder = list.toList().filter { document ->
                document.type?.contains("audio") ?: false
            }.filter { document ->
                document.uri.path != null
            }.map { document ->
                val fileUri = document.uri
                val metadataRetriever = MediaMetadataRetriever()
                metadataRetriever.setDataSource(context, fileUri)
                metadataRetriever.toBook(fileUri.path!!)
            }.toFolder()

            viewModel.addBookFolder(bookFolder)
        }
    }

    // lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val eventObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                }

                else -> {
                    // Do nothing in other states
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(eventObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(eventObserver)
        }
    })

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetContent = {
            if (selectedBookIndex != -1) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MiniPlayer(
                        book = viewModel.booksList[selectedBookIndex],
                        scaffoldState = bottomSheetState,
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
        sheetPeekHeight = if (hasBookSelected) componentHeight.value.dp else 0.dp,
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

                            Column {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    items(bookList.size) { index ->
                                        when (val book = bookList[index]) {
                                            is BookFolder -> {
                                                BookItem(book) {
                                                    hasBookSelected = true
                                                    selectedBookIndex = index
                                                }
                                            }

                                            is BookFile -> {
                                                BookItem(book) {
                                                    hasBookSelected = true
                                                    selectedBookIndex = index
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
