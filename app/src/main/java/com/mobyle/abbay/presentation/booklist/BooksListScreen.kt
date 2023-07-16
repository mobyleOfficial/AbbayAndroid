package com.mobyle.abbay.presentation.booklist

import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.GenericError
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.mobyle.abbay.presentation.common.mappers.toBook
import com.mobyle.abbay.presentation.common.mappers.toFolder
import com.mobyle.abbay.presentation.common.widgets.SVGIcon
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

    val booksListState by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                "Your Audiobooks", style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }, actions = {
            IconButton(onClick = { openFolderSelector.launch(null) }) {
                SVGIcon(
                    R.drawable.folder_plus,
                    "Add folder icon"
                )
            }
            IconButton(onClick = { openFileSelector.launch(arrayOf("audio/*")) }) {
                SVGIcon(
                    R.drawable.file_plus,
                    "Add folder icon"
                )
            }
        })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = booksListState) {
                is BookListSuccess -> {
                    val bookList = state.audiobookList

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                        }
                        items(bookList.size) { index ->
                            when (val book = bookList[index]) {
                                is BookFolder -> {
                                    BookFolderItem(book)
                                }

                                is BookFile -> {
                                    BookFileItem(book)
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
                            openFileSelector.launch(arrayOf("audio/*"))
                        }) {
                            Text("Add a file")
                        }

                        Text("Or")

                        Button(onClick = {
                            openFolderSelector.launch(null)
                        }) {
                            Text(text = "Add a folder")
                        }
                    }

                }

                is GenericError -> {}
                is Loading -> {}
            }
        }
    }
}

@Composable
fun BookFileItem(book: BookFile) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
                    .height(86.dp)
                    .width(86.dp)
            ) {
                AsyncImage(
                    contentScale = ContentScale.FillBounds,
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.thumbnail)
                        .fallback(R.drawable.file_music)
                        .error(R.drawable.file_music)
                        .crossfade(true)
                        .build(), contentDescription = ""
                )
            }
            Column {
                Text(book.name)
                Row {
                    Row {
                        Text("Icon")
                        Text("00:18:43/8:44:09")
                    }
                }
            }
        }
        Divider(
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun BookFolderItem(book: BookFolder) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
                    .height(86.dp)
                    .width(86.dp)
            ) {
                AsyncImage(
                    contentScale = ContentScale.FillBounds,
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.thumbnail)
                        .fallback(R.drawable.file_music)
                        .error(R.drawable.file_music)
                        .crossfade(true)
                        .build(), contentDescription = ""
                )
            }
            Column {
                Text(book.name)
                Row {
                    Row {
                        Text("Icon")
                        Text("01/18")
                    }
                }
                Row {
                    Row {
                        Text("Icon")
                        Text("00:18:43/8:44:09")
                    }
                }
            }
        }
        Divider(
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(horizontal = 16.dp)
        )
    }
}
