package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.utils.debounceClick
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Chapter

/** Current chapter title and "Chapter n of N"; tapping it shows or hides the chapter list. */
@Composable
fun ChapterRow(chapter: Chapter?, chapterCount: Int, isExpanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .debounceClick { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // TalkBack announces a new chapter politely, never the running time.
        Column(
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (chapter?.title?.isNotBlank() == true) {
                Text(
                    text = chapter.title,
                    style = AbbayTextStyles.chapterTitle,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }
            if (chapter != null) {
                Text(
                    text = stringResource(R.string.player_chapter_of, chapter.index + 1, chapterCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(if (isExpanded) R.string.hide_chapters else R.string.show_chapters),
            tint = AbbayColors.content,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** The book's chapters, current one highlighted; selecting one seeks to its start. */
@Composable
fun ChapterList(chapters: List<Chapter>, currentIndex: Int, onSelect: (Chapter) -> Unit) {
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
    ) {
        itemsIndexed(chapters) { index, chapter ->
            val isSelected = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { selected = isSelected }
                    .debounceClick { onSelect(chapter) }
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = AbbayTextStyles.buttonTextMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onTertiary else AbbayColors.content
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapterTitle(chapter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    )
                    Text(
                        text = (chapter.endMs - chapter.startMs).toHHMMSS(),
                        style = MaterialTheme.typography.bodySmall,
                        // Full color: at 70 % the light theme's green fell to 3.2:1 (WCAG AA needs 4.5:1).
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}
