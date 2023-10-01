package com.mobyle.abbay.presentation.booklist

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobyle.abbay.R
import com.model.BookFile

@Composable
fun BookFileItem(book: BookFile, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable {
                onClick.invoke()
            },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
                    .background(Color.Gray)
                    .height(86.dp)
                    .width(86.dp)
            ) {
                AsyncImage(
                    contentScale = ContentScale.FillBounds,
                    model = ImageRequest.Builder(LocalContext.current).data(book.thumbnail)
                        .fallback(R.drawable.file_music).error(R.drawable.file_music)
                        .crossfade(true).build(),
                    contentDescription = ""
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
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