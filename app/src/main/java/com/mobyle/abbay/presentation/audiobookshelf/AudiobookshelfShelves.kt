package com.mobyle.abbay.presentation.audiobookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSort

/** All books or series: what a library shows before a search. */
@Composable
internal fun ShelfTabs(selected: AudiobookshelfTab, onSelect: (AudiobookshelfTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .selectableGroup(),
    ) {
        AudiobookshelfTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
            ) {
                Text(
                    stringResource(if (tab == AudiobookshelfTab.ALL) R.string.audiobookshelf_all else R.string.audiobookshelf_series),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = AbbayColors.content.copy(alpha = if (isSelected) 1f else SECONDARY_ALPHA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(INDICATOR_HEIGHT)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.tertiary else AbbayColors.content.copy(alpha = DIVIDER_ALPHA)
                        ),
                )
            }
        }
    }
}

/**
 * Covers in a grid that asks for its next page as it nears its end, with [header] above them and, below, whether
 * more are loading or failed to.
 */
@Composable
internal fun <T> PagedGrid(
    items: List<T>,
    key: (T) -> String,
    more: MoreItems,
    onLoadMore: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    cell: @Composable (T) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val isNearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(isNearEnd, more, items.size) {
        if (isNearEnd && more == MoreItems.AVAILABLE) onLoadMore()
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_WIDTH),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        header?.let { content -> item(span = { GridItemSpan(maxLineSpan) }) { content() } }
        items(items, key = key) { cell(it) }
        if (more == MoreItems.LOADING || more == MoreItems.FAILED) {
            item(span = { GridItemSpan(maxLineSpan) }) { MoreFooter(isFailed = more == MoreItems.FAILED, onRetry = onLoadMore) }
        }
    }
}

@Composable
private fun MoreFooter(isFailed: Boolean, onRetry: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        if (isFailed) {
            AbbayTextButton(onClick = onRetry) { Text(stringResource(R.string.librivox_retry)) }
        } else {
            val loading = stringResource(R.string.librivox_loading)
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = loading },
            )
        }
    }
}

/** How the books are listed: newest first, by title or by author. */
@Composable
internal fun SortPicker(selected: AudiobookshelfSort, onSelect: (AudiobookshelfSort) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(
            stringResource(R.string.audiobookshelf_sort_label),
            style = MaterialTheme.typography.bodyMedium,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
        )
        Box {
            AbbayTextButton(onClick = { isExpanded = true }) {
                Text(stringResource(labelOf(selected)))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            AbbayDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                AudiobookshelfSort.entries.forEach { sort ->
                    AbbayMenuItem(
                        text = stringResource(labelOf(sort)),
                        selected = sort == selected,
                        onClick = {
                            isExpanded = false
                            onSelect(sort)
                        },
                    )
                }
            }
        }
    }
}

private fun labelOf(sort: AudiobookshelfSort): Int = when (sort) {
    AudiobookshelfSort.RECENTLY_ADDED -> R.string.audiobookshelf_recent
    AudiobookshelfSort.TITLE -> R.string.audiobookshelf_sort_title
    AudiobookshelfSort.AUTHOR -> R.string.audiobookshelf_sort_author
}

@Composable
internal fun SeriesCell(series: AudiobookshelfSeries, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookThumbnail(
            series.coverUrl.ifEmpty { null },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = COVER_RADIUS,
            shadow = true,
        )
        Text(
            series.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AbbayColors.content,
            minLines = TITLE_LINES,
            maxLines = TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            pluralStringResource(R.plurals.audiobookshelf_series_book_count, series.bookCount, series.bookCount),
            style = MaterialTheme.typography.bodySmall,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            maxLines = 1,
        )
    }
}

private val GRID_CELL_MIN_WIDTH = 112.dp
private val COVER_RADIUS = 10.dp
private val INDICATOR_HEIGHT = 2.dp
private const val TITLE_LINES = 2
private const val SECONDARY_ALPHA = 0.75f
private const val DIVIDER_ALPHA = 0.12f
private const val PREFETCH_DISTANCE = 6
