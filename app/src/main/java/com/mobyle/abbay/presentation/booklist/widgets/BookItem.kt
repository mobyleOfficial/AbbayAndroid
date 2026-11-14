package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.mobyle.abbay.presentation.common.widgets.StreamingBadge
import com.mobyle.abbay.presentation.utils.debounceClick
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book

@Composable
fun BookItem(
    book: Book,
    progressLabel: String,
    isPlaying: Boolean,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(ROW_RADIUS))
            .background(if (book.hasError) ErrorRowColor else Color.Transparent)
            .debounceClick(onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            BookThumbnail(book.thumbnail)
            if (isStreamingOnly(book)) StreamingBadge(size = 20.dp, inset = 3.dp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BookTitle(book, isPlaying, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bookStatusLine(book),
                    style = statusStyle(),
                    color = secondaryContent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$progressLabel / ${book.duration.toHHMMSS()}",
                    style = timeStyle(),
                    color = secondaryContent(),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            val fraction = bookStartedFraction(book)
            ProgressCapsule(fraction = fraction ?: 0f, modifier = Modifier.alpha(if (fraction == null) 0f else 1f))
        }
    }
}

@Composable
internal fun BookTitle(book: Book, isPlaying: Boolean, maxLines: Int, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (isPlaying) {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = stringResource(R.string.book_now_playing),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 4.dp).size(16.dp),
            )
        }
        Text(
            book.name,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            color = if (isPlaying) MaterialTheme.colorScheme.tertiary else AbbayColors.content,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (book.hasError) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = stringResource(R.string.error_playing_file),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp).size(18.dp),
            )
        }
    }
}

@Composable
internal fun secondaryContent(): Color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA)

@Composable
internal fun statusStyle() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)

@Composable
internal fun timeStyle() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)

internal val ErrorRowColor = Color(0xff1f1113)

private val ROW_RADIUS = 12.dp
private const val SECONDARY_ALPHA = 0.75f
