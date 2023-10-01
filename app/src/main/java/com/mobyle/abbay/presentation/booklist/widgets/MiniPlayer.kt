package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.model.Book
import com.model.BookFolder

@Composable
fun MiniPlayer(book: Book, modifier: Modifier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        BookThumbnail(book.thumbnail, modifier = Modifier.padding(8.dp))
        Column {
            Text(book.name)
            Column {
                Row {
                    Text("Ouvido")
                    Text("Faltando")
                }
            }
            Row {
                Text("Controles")
                if (book is BookFolder) {
                    Text("1/${book.bookFileList.size}")
                }

            }
        }
    }
}