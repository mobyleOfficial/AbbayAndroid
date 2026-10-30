package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.model.Book

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
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(start = 4.dp, end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AbbayColors.content,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                val subtitle = listOf(chapterTitle(position.chapter), timeLeftLabel(book.duration - position.bookPositionMs, book.speed))
                    .filter { it.isNotBlank() }
                    .joinToString(SEPARATOR)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AbbayColors.content.copy(alpha = SUBTITLE_ALPHA),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            if (isScreenLocked) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.screen_locked),
                    tint = AbbayColors.content,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                PlayButton(playerIcon, onClick = onPlayButtonClick)
            }
        }
        ProgressCapsule(
            fraction = progressFraction(position.bookPositionMs, book.duration),
            height = 2.dp,
            fill = AbbayColors.content,
            modifier = Modifier.align(Alignment.BottomCenter).padding(end = 8.dp),
        )
    }
}

private const val SEPARATOR = " · "
private const val SUBTITLE_ALPHA = 0.75f
