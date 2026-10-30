package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.utils.debounceClick
import com.model.Book

@Composable
fun BookCompactItem(book: Book, progressLabel: String, isPlaying: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(if (book.hasError) ErrorRowColor else Color.Transparent)
                .debounceClick(onLongClick = onLongClick, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookTitle(book, isPlaying, maxLines = 1, modifier = Modifier.weight(1f))
            Text(
                bookStatusLine(book),
                style = statusStyle(),
                color = secondaryContent(),
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(progressLabel, style = timeStyle(), color = secondaryContent(), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private const val DIVIDER_ALPHA = 0.12f
