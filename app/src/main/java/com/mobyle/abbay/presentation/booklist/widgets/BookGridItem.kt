package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.utils.debounceClick
import com.model.Book

/** Cover-first grid cell. */
@Composable
fun BookGridItem(book: Book, progressLabel: String, progressFraction: Float, onLongClick: () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (book.hasError) Color(0xff1f1113) else MaterialTheme.colorScheme.primary)
            .debounceClick(onLongClick = onLongClick, onClick = onClick)
    ) {
        BookThumbnail(
            book.thumbnail,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        LinearProgressIndicator(
            progress = { progressFraction },
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
        Text(
            book.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = AbbayTextStyles.subtitleText,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(progressLabel, style = AbbayTextStyles.progressText, maxLines = 1)
        if (book.hasError) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = stringResource(R.string.error_playing_file),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
