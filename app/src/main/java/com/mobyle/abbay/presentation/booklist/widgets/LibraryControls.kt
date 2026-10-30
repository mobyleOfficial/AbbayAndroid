package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.library.LibraryLayout
import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.model.ScanStatus

/** Tabs, sort menu, layout menu and scan progress of the library. */
@Composable
fun LibraryControls(
    tab: LibraryTab,
    tabs: List<LibraryTab>,
    sortOrder: LibrarySortOrder,
    sortAscending: Boolean,
    layout: LibraryLayout,
    scanStatus: ScanStatus,
    onTabSelected: (LibraryTab) -> Unit,
    onSortOrderSelected: (LibrarySortOrder) -> Unit,
    onLayoutSelected: (LibraryLayout) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryTabPills(tab = tab, tabs = tabs, onTabSelected = onTabSelected, modifier = Modifier.weight(1f))
        SortMenu(sortOrder, sortAscending, onSortOrderSelected)
        LayoutMenu(layout, onLayoutSelected)
    }

    if (scanStatus.isScanning) {
        val description = stringResource(R.string.library_scanning)
        val fraction = if (scanStatus.totalBooks > 0) scanStatus.processedBooks.toFloat() / scanStatus.totalBooks else 0f
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
        )
    }
}

/** The tabs as capsules (AbbayiOS's look): the selected one filled, the others on a faint fill. */
@Composable
private fun LibraryTabPills(
    tab: LibraryTab,
    tabs: List<LibraryTab>,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectableGroup()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { libraryTab ->
            val isSelected = libraryTab == tab
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onTabSelected(libraryTab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(libraryTab.labelResource()),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else AbbayColors.content,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) AbbayColors.content else AbbayColors.content.copy(alpha = UNSELECTED_FILL_ALPHA))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private const val UNSELECTED_FILL_ALPHA = 0.08f

@Composable
private fun SortMenu(sortOrder: LibrarySortOrder, sortAscending: Boolean, onSortOrderSelected: (LibrarySortOrder) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.library_sort), tint = AbbayColors.content)
        }
        AbbayDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            LibrarySortOrder.entries.forEach { order ->
                val isCurrent = order == sortOrder
                AbbayMenuItem(
                    text = stringResource(order.labelResource()),
                    selected = isCurrent,
                    // The current order shows its direction instead of a check mark.
                    trailingIcon = if (isCurrent) {
                        { tint ->
                            Icon(
                                imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = stringResource(
                                    if (sortAscending) R.string.library_sort_ascending else R.string.library_sort_descending
                                ),
                                tint = tint,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        isExpanded = false
                        onSortOrderSelected(order)
                    }
                )
            }
        }
    }
}

@Composable
private fun LayoutMenu(layout: LibraryLayout, onLayoutSelected: (LibraryLayout) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(layout.icon(), contentDescription = stringResource(R.string.library_layout), tint = AbbayColors.content)
        }
        AbbayDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            LibraryLayout.entries.forEach { option ->
                AbbayMenuItem(
                    text = stringResource(option.labelResource()),
                    selected = option == layout,
                    leadingIcon = option.icon(),
                    onClick = {
                        isExpanded = false
                        onLayoutSelected(option)
                    }
                )
            }
        }
    }
}

internal fun LibraryTab.labelResource() = when (this) {
    LibraryTab.IN_PROGRESS -> R.string.library_tab_in_progress
    LibraryTab.NEW -> R.string.library_tab_new
    LibraryTab.FINISHED -> R.string.library_tab_finished
    LibraryTab.ALL -> R.string.library_tab_all
}

private fun LibrarySortOrder.labelResource() = when (this) {
    LibrarySortOrder.TITLE -> R.string.library_sort_title
    LibrarySortOrder.AUTHOR -> R.string.library_sort_author
    LibrarySortOrder.RECENTLY_PLAYED -> R.string.library_sort_recently_played
    LibrarySortOrder.RECENTLY_ADDED -> R.string.library_sort_recently_added
    LibrarySortOrder.PROGRESS -> R.string.library_sort_progress
    LibrarySortOrder.DURATION -> R.string.library_sort_duration
}

internal fun LibraryLayout.labelResource() = when (this) {
    LibraryLayout.LIST -> R.string.library_layout_list
    LibraryLayout.GRID -> R.string.library_layout_grid
    LibraryLayout.COMPACT -> R.string.library_layout_compact
}

private fun LibraryLayout.icon() = when (this) {
    LibraryLayout.LIST -> Icons.AutoMirrored.Filled.ViewList
    LibraryLayout.GRID -> Icons.Default.GridView
    LibraryLayout.COMPACT -> Icons.Default.ViewHeadline
}
