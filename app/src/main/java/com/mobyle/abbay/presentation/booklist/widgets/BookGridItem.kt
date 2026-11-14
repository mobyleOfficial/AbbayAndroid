package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.mobyle.abbay.presentation.common.widgets.StreamingBadge
import com.mobyle.abbay.presentation.utils.debounceClick
import com.model.Book

@Composable
fun BookGridItem(book: Book, progressLabel: String, isPlaying: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    val titleHeight = with(LocalDensity.current) { (TITLE_LINE_HEIGHT_SP * TITLE_LINES * fontScale).dp }
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(CELL_RADIUS))
            .background(if (book.hasError) ErrorRowColor else Color.Transparent)
            .debounceClick(onLongClick = onLongClick, onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            BookThumbnail(
                book.thumbnail,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cornerRadius = COVER_RADIUS,
                shadow = true,
            )
            if (isStreamingOnly(book)) StreamingBadge(size = 26.dp, inset = 6.dp)
        }
        BookTitle(book, isPlaying, maxLines = TITLE_LINES, modifier = Modifier.padding(top = 4.dp).heightIn(min = titleHeight))
        Text(bookStatusLine(book), style = statusStyle(), color = secondaryContent(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(progressLabel, style = timeStyle(), color = secondaryContent(), maxLines = 1)
        val fraction = bookStartedFraction(book)
        ProgressCapsule(fraction = fraction ?: 0f, modifier = Modifier.alpha(if (fraction == null) 0f else 1f))
    }
}

private val CELL_RADIUS = 12.dp
private val COVER_RADIUS = 10.dp
private const val TITLE_LINES = 2
private const val TITLE_LINE_HEIGHT_SP = 20
