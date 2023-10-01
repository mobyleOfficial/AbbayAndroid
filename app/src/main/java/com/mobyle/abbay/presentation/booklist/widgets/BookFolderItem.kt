package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.model.BookFolder

@Composable
fun BookFolderItem(book: BookFolder, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable {
                onClick.invoke()
            },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            BookThumbnail(book.thumbnail)
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
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