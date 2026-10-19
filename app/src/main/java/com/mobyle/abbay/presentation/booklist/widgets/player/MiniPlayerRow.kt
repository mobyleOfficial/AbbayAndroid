package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.model.Book

/** Collapsed player: title, chapter as a second line, play/pause and a thin progress line. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerRow(
    book: Book,
    position: PlayerPosition,
    playerIcon: PlayingState,
    isScreenLocked: Boolean,
    onPlayButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(book.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.basicMarquee())
                Text(
                    chapterTitle(position.chapter),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            if (isScreenLocked) {
                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.screen_locked), tint = AbbayColors.content)
            } else {
                PlayButton(playerIcon, onClick = onPlayButtonClick)
            }
        }
        LinearProgressIndicator(
            progress = { progressFraction(position.bookPositionMs, book.duration) },
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth().height(2.dp),
        )
    }
}
