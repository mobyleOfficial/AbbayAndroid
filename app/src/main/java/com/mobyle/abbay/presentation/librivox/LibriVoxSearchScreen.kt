package com.mobyle.abbay.presentation.librivox

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.common.widgets.abbayTextFieldColors
import com.model.LibriVoxBook

@Composable
fun LibriVoxSearchScreen(
    viewModel: LibriVoxSearchViewModel = hiltViewModel(),
    onOpenBook: (LibriVoxBook) -> Unit,
    close: () -> Unit,
) {
    val query by viewModel.query.collectAsState()
    val state by viewModel.state.collectAsState()
    val libraryBookIds by viewModel.libraryBookIds.collectAsState()
    val focusManager = LocalFocusManager.current

    AbbayScreen(title = stringResource(R.string.librivox_title), onClose = close) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.librivox_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AbbayColors.content) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = AbbayColors.content
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = abbayTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val current = state) {
                LibriVoxSearchState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                }
                LibriVoxSearchState.Offline -> StateMessage(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.librivox_offline_title),
                    body = stringResource(R.string.librivox_offline_body),
                    onRetry = viewModel::retry,
                )
                LibriVoxSearchState.Failed -> StateMessage(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.librivox_failed_title),
                    body = stringResource(R.string.librivox_failed_body),
                    onRetry = viewModel::retry,
                )
                LibriVoxSearchState.NoResults -> StateMessage(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.librivox_no_results_title),
                    body = stringResource(R.string.librivox_no_results_body),
                )
                is LibriVoxSearchState.Popular -> PopularGrid(current.books, onOpenBook)
                is LibriVoxSearchState.Results -> ResultsList(current.books, libraryBookIds.keys, onOpenBook)
            }
        }
    }
}

@Composable
private fun PopularGrid(books: List<LibriVoxBook>, onOpenBook: (LibriVoxBook) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_WIDTH),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.librivox_popular),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AbbayColors.content,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
        }
        items(books, key = { it.id }) { book -> PopularCell(book, onClick = { onOpenBook(book) }) }
    }
}

@Composable
private fun PopularCell(book: LibriVoxBook, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookThumbnail(book.coverUrl, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = COVER_RADIUS, shadow = true)
        Text(
            book.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AbbayColors.content,
            minLines = TITLE_LINES,
            maxLines = TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            book.authors,
            style = MaterialTheme.typography.bodySmall,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ResultsList(books: List<LibriVoxBook>, libraryIds: Set<String>, onOpenBook: (LibriVoxBook) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(books, key = { it.id }) { book ->
            LibriVoxResultRow(book, isInLibrary = book.id in libraryIds, onClick = { onOpenBook(book) })
            HorizontalDivider(
                color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA),
                modifier = Modifier.padding(start = 16.dp + ROW_COVER + 16.dp),
            )
        }
    }
}

@Composable
private fun LibriVoxResultRow(book: LibriVoxBook, isInLibrary: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BookThumbnail(book.coverUrl, modifier = Modifier.size(ROW_COVER))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                book.title,
                style = MaterialTheme.typography.bodyLarge,
                color = AbbayColors.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                book.authors,
                style = MaterialTheme.typography.bodySmall,
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            stringResource(if (isInLibrary) R.string.librivox_in_library else R.string.librivox_get),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isInLibrary) AbbayColors.content else MaterialTheme.colorScheme.tertiary,
            maxLines = 1,
            modifier = Modifier
                .clip(CircleShape)
                .background(AbbayColors.content.copy(alpha = PILL_ALPHA))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun StateMessage(icon: ImageVector, title: String, body: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = AbbayColors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        onRetry?.let { AbbayTextButton(onClick = it) { Text(stringResource(R.string.librivox_retry)) } }
    }
}

private val GRID_CELL_MIN_WIDTH = 112.dp
private val COVER_RADIUS = 10.dp
private val ROW_COVER = 56.dp
private const val TITLE_LINES = 2
private const val DIVIDER_ALPHA = 0.08f
private const val PILL_ALPHA = 0.1f
