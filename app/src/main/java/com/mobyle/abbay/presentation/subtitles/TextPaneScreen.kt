package com.mobyle.abbay.presentation.subtitles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.subtitles.SubtitleLine

/**
 * Subtitles or lyrics of the playing track: the current line stands out,
 * the others are dimmed; tapping a line seeks to it. The list follows playback except for 5 s after the user scrolls.
 */
@Composable
fun TextPaneScreen(viewModel: TextPaneViewModel = hiltViewModel(), close: () -> Unit) {
    val state by viewModel.state.collectAsState()

    AbbayScreen(title = stringResource(R.string.text_title), onClose = close) {
        when (val current = state) {
            TextPaneViewModel.State.Loading -> Unit
            TextPaneViewModel.State.NoText -> EmptyMessage(stringResource(R.string.text_none))
            is TextPaneViewModel.State.Text -> when {
                current.subtitles.needsCharset -> CharsetChoice(current.subtitles.fileName, viewModel::chooseCharset)
                current.subtitles.lines.isEmpty() -> EmptyMessage(stringResource(R.string.text_none))
                else -> Lines(current.subtitles.lines, current.currentIndex, viewModel::seekTo)
            }
        }
    }
}

@Composable
private fun Lines(lines: List<SubtitleLine>, currentIndex: Int, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val lastUserScrollAt = remember { mutableLongStateOf(0L) }
    val userScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) lastUserScrollAt.longValue = System.currentTimeMillis()
                return Offset.Zero
            }
        }
    }
    FollowPlayback(listState, currentIndex) { System.currentTimeMillis() - lastUserScrollAt.longValue >= USER_SCROLL_PAUSE_MS }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 120.dp),
        modifier = Modifier.fillMaxSize().nestedScroll(userScroll),
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentIndex
            Text(
                line.text,
                style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = AbbayColors.content.copy(alpha = if (isCurrent) 1f else DIMMED_ALPHA),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = stringResource(R.string.text_go_to_line)) { onSeek(line.startMs) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}

/** Keeps the current line in view unless the user scrolled in the last 5 s. */
@Composable
private fun FollowPlayback(listState: LazyListState, currentIndex: Int, isFollowing: () -> Boolean) {
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && isFollowing()) listState.animateScrollToItem(currentIndex, scrollOffset = -CENTER_OFFSET_PX)
    }
}

@Composable
private fun CharsetChoice(fileName: String, onChoose: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.text_choose_charset, fileName),
            color = AbbayColors.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn {
            itemsIndexed(TextPaneViewModel.CHARSETS) { _, charset ->
                Text(
                    charset,
                    color = AbbayColors.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onChoose(charset) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

private const val USER_SCROLL_PAUSE_MS = 5_000L
private const val DIMMED_ALPHA = 0.5f
private const val CENTER_OFFSET_PX = 300
