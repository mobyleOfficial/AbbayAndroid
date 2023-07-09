package com.mobyle.abbay.presentation.booklist

import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.GenericError
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.model.BookFile
import com.model.BookFolder

@ExperimentalMaterial3Api
@Composable
fun BooksListScreen() {
    val viewModel: BooksListViewModel = hiltViewModel()
    val context = LocalContext.current

    val openFileSelector = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.OpenMultipleDocuments()
    ) { filesList ->
        if (filesList.isNotEmpty()) {
            viewModel.addBooksList(filesList.filter { it.path != null }
                .map { uri ->
                    val metadataRetriever = MediaMetadataRetriever()
                    metadataRetriever.setDataSource(context, uri)
                    metadataRetriever.toBook(uri.path!!)
                }
            )
        }
    }

    // Launchers
    val openFolderSelector = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.OpenDocumentTree()
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

    Scaffold(topBar = {
        TopAppBar(title = {
            Text("Abbay")
        }, actions = {
            IconButton(onClick = { openFolderSelector.launch(null) }) {
                Text("Folder")
            }
            IconButton(onClick = { openFileSelector.launch(arrayOf("audio/*")) }) {
                Text("Files")
            }
        })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BooksListBody(
                viewModel,
                { openFileSelector.launch(arrayOf("audio/*")) },
                { openFolderSelector.launch(null) })
        }
    }
}

@Composable
private fun BooksListBody(
    viewModel: BooksListViewModel,
    openFileSelector: () -> Unit,
    openFolderSelector: () -> Unit,
) {

    val booksListState = viewModel.uiState.collectAsState()

    when (val state = booksListState.value) {
        is BookListSuccess -> {
            val bookList = state.audiobookList

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bookList.size) { index ->
                    val book = bookList[index]
                    Row {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.thumbnail)
                                .crossfade(true)
                                .build(), contentDescription = ""
                        )
                        Text(book.name)
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
                    openFileSelector.invoke()
                }) {
                    Text("Add a file")
                }

                Text("Or")

                Button(onClick = {
                    openFolderSelector.invoke()
                }) {
                    Text(text = "Add a folder")
                }
            }

        }

        is GenericError -> {}
        is Loading -> {}
    }
}

private fun MediaMetadataRetriever.toBook(path: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
    return BookFile(path, title ?: "", embeddedPicture, Integer.parseInt(duration))
}

private fun List<BookFile>.toFolder(): BookFolder {
    val firstBook = first()
    return BookFolder(this, firstBook.name, firstBook.thumbnail, this.sumOf { it.duration })
}