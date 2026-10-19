package com.mobyle.abbay.presentation.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.library.FolderEntry
import com.library.FolderLocation
import com.library.FolderTree
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.SimpleBookRow
import com.model.Book
import com.model.LibraryRoot

/** Roots, collections and book folders as a tree with a breadcrumb. Back goes one level up. */
@Composable
fun FolderScreen(viewModel: FolderViewModel = hiltViewModel(), onPlay: (Book) -> Unit, close: () -> Unit) {
    val location by viewModel.location.collectAsState()
    val level by viewModel.level.collectAsState()
    val roots by viewModel.roots.collectAsState()

    BackHandler { if (!viewModel.goUp()) close() }

    AbbayScreen(title = stringResource(R.string.folders_title), onClose = close) {
        Column(modifier = Modifier.fillMaxSize()) {
            Breadcrumb(location, roots, viewModel::open)
            HorizontalDivider(color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA))
            val current = level ?: return@Column
            if (current.folders.isEmpty() && current.books.isEmpty()) {
                EmptyMessage(stringResource(R.string.folders_empty))
                return@Column
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(current.folders, key = { "folder:${it.rootId}:${it.path}:${it.name}" }) { folder ->
                    FolderRow(folder) { viewModel.open(locationOf(folder, location)) }
                }
                items(current.books, key = { "book:${it.id}" }) { book -> SimpleBookRow(book, onClick = { onPlay(book) }) }
            }
        }
    }
}

private fun locationOf(folder: FolderEntry, current: FolderLocation): FolderLocation = when {
    folder.rootId == null -> FolderLocation(isSingleFiles = true)
    current.isTop -> FolderLocation(rootId = folder.rootId)
    else -> FolderLocation(folder.rootId, folder.path)
}

@Composable
private fun Breadcrumb(location: FolderLocation, roots: List<LibraryRoot>, onOpen: (FolderLocation) -> Unit) {
    val crumbs = buildList {
        add(stringResource(R.string.folders_breadcrumb_top) to FolderLocation.TOP)
        if (location.isSingleFiles) add(stringResource(R.string.folders_single_files) to location)
        location.rootId?.let { rootId ->
            add((roots.firstOrNull { it.id == rootId }?.displayName.orEmpty()) to location.upTo(0))
            location.segments.forEachIndexed { index, segment -> add(segment to location.upTo(index + 1)) }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
    ) {
        crumbs.forEachIndexed { index, (label, target) ->
            val isLast = index == crumbs.lastIndex
            Text(
                label,
                color = if (isLast) AbbayColors.content else MaterialTheme.colorScheme.tertiary,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .then(if (isLast) Modifier.semantics { heading() } else Modifier.clickable { onOpen(target) })
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            )
            if (!isLast) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AbbayColors.content)
        }
    }
}

@Composable
private fun FolderRow(folder: FolderEntry, onClick: () -> Unit) {
    val name = if (folder.rootId == null &&
        folder.name == FolderTree.SINGLE_FILES
    ) {
        stringResource(R.string.folders_single_files)
    } else {
        folder.name
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            if (folder.rootId == null) Icons.Default.AudioFile else Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = AbbayColors.content)
            Text(
                pluralStringResource(R.plurals.folders_book_count, folder.bookCount, folder.bookCount),
                style = MaterialTheme.typography.bodySmall,
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AbbayColors.content)
    }
}

private const val SECONDARY_ALPHA = 0.7f
private const val DIVIDER_ALPHA = 0.2f
