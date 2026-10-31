package com.mobyle.abbay.presentation.audiobookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.abbayTextFieldColors
import com.mobyle.abbay.presentation.librivox.PopularCell
import com.mobyle.abbay.presentation.librivox.ResultsList
import com.mobyle.abbay.presentation.librivox.StateMessage
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSeries
import com.model.CatalogSeries
import com.model.LibriVoxBook

@Composable
fun AudiobookshelfScreen(
    viewModel: AudiobookshelfViewModel = hiltViewModel(),
    signInViewModel: AudiobookshelfSignInViewModel = hiltViewModel(),
    onOpenBook: (LibriVoxBook) -> Unit,
    onOpenSeries: (CatalogSeries) -> Unit,
    close: () -> Unit,
) {
    val account by signInViewModel.account.collectAsState()
    val signedIn = account?.takeIf(AudiobookshelfAccount::isSignedIn)

    AbbayScreen(
        title = stringResource(R.string.audiobookshelf_title),
        onClose = close,
        actions = { signedIn?.let { AccountMenu(it, onSignOut = signInViewModel::signOut) } },
    ) {
        if (signedIn == null) SignInForm(account, signInViewModel) else Browse(viewModel, onOpenBook, onOpenSeries)
    }
}

@Composable
private fun AccountMenu(account: AudiobookshelfAccount, onSignOut: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { isExpanded = true }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.librivox_more_options), tint = AbbayColors.content)
    }
    AbbayDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        Text(
            stringResource(R.string.audiobookshelf_signed_in_as, account.username, hostOf(account.serverUrl)),
            style = MaterialTheme.typography.bodySmall,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 280.dp),
        )
        AbbayMenuItem(
            text = stringResource(R.string.audiobookshelf_sign_out),
            leadingIcon = Icons.AutoMirrored.Outlined.Logout,
            onClick = {
                isExpanded = false
                onSignOut()
            },
        )
    }
}

@Composable
private fun Browse(viewModel: AudiobookshelfViewModel, onOpenBook: (LibriVoxBook) -> Unit, onOpenSeries: (CatalogSeries) -> Unit) {
    val query by viewModel.query.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val state by viewModel.state.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val selectedLibraryId by viewModel.selectedLibraryId.collectAsState()
    val libraryBookIds by viewModel.libraryBookIds.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.audiobookshelf_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AbbayColors.content) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear), tint = AbbayColors.content)
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
        if (libraries.size > 1) LibraryPicker(libraries, selectedLibraryId, onSelect = viewModel::selectLibrary)
        if (query.isBlank()) ShelfTabs(tab, onSelect = viewModel::selectTab)
        BrowseContent(
            state = state,
            emptyShelf = { EmptyShelf(tab, isQueryBlank = query.isBlank(), onRetry = viewModel::retry) },
            libraryIds = libraryBookIds.keys,
            onOpenBook = onOpenBook,
            onOpenSeries = { series ->
                selectedLibraryId?.let { libraryId -> onOpenSeries(CatalogSeries(series.id, series.name, sequence = null, libraryId)) }
            },
            onRetry = viewModel::retry,
            onLoadMore = viewModel::loadMore,
            booksHeader = { SortPicker(sort, onSelect = viewModel::selectSort) },
        )
    }
}

@Composable
private fun BrowseContent(
    state: AudiobookshelfBrowseState,
    emptyShelf: @Composable () -> Unit,
    libraryIds: Set<String>,
    onOpenBook: (LibriVoxBook) -> Unit,
    onOpenSeries: (AudiobookshelfSeries) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    booksHeader: @Composable () -> Unit,
) {
    when (val current = state) {
        AudiobookshelfBrowseState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
        }
        AudiobookshelfBrowseState.Offline -> StateMessage(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.librivox_offline_title),
            body = stringResource(R.string.audiobookshelf_offline_body),
            onRetry = onRetry,
        )
        AudiobookshelfBrowseState.Failed -> StateMessage(
            icon = Icons.Outlined.ErrorOutline,
            title = stringResource(R.string.audiobookshelf_failed_title),
            body = stringResource(R.string.librivox_failed_body),
            onRetry = onRetry,
        )
        AudiobookshelfBrowseState.NoLibraries -> StateMessage(
            icon = Icons.Outlined.LibraryBooks,
            title = stringResource(R.string.audiobookshelf_no_libraries_title),
            body = stringResource(R.string.audiobookshelf_no_libraries_body),
            onRetry = onRetry,
        )
        AudiobookshelfBrowseState.NoResults -> emptyShelf()
        is AudiobookshelfBrowseState.Books -> PagedGrid(current.books, key = {
            it.id
        }, current.more, onLoadMore, header = booksHeader) { book ->
            PopularCell(book, onClick = { onOpenBook(book) })
        }
        is AudiobookshelfBrowseState.Results -> ResultsList(current.books, libraryIds, onOpenBook)
        is AudiobookshelfBrowseState.Series -> PagedGrid(current.series, key = { it.id }, current.more, onLoadMore) { series ->
            SeriesCell(series, onClick = { onOpenSeries(series) })
        }
    }
}

/** Nothing to show: an empty library, no series in it, or no match for the search. */
@Composable
private fun EmptyShelf(tab: AudiobookshelfTab, isQueryBlank: Boolean, onRetry: () -> Unit) {
    when {
        !isQueryBlank -> StateMessage(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.librivox_no_results_title),
            body = stringResource(R.string.audiobookshelf_no_results_body),
        )
        tab == AudiobookshelfTab.SERIES -> StateMessage(
            icon = Icons.Outlined.LibraryBooks,
            title = stringResource(R.string.audiobookshelf_no_series_title),
            body = stringResource(R.string.audiobookshelf_no_series_body),
            onRetry = onRetry,
        )
        else -> StateMessage(
            icon = Icons.Outlined.LibraryBooks,
            title = stringResource(R.string.audiobookshelf_empty_library_title),
            body = stringResource(R.string.audiobookshelf_empty_library_body),
            onRetry = onRetry,
        )
    }
}

@Composable
private fun LibraryPicker(libraries: List<AudiobookshelfLibrary>, selectedId: String?, onSelect: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        items(libraries, key = { it.id }) { library ->
            val isSelected = library.id == selectedId
            Text(
                library.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onTertiary else AbbayColors.content,
                maxLines = 1,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.tertiary else AbbayColors.content.copy(alpha = PILL_ALPHA))
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(library.id) })
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

internal fun hostOf(serverUrl: String): String = serverUrl.toUri().let { uri ->
    listOfNotNull(uri.host, uri.port.takeIf { it >= 0 }?.toString()).joinToString(":").ifEmpty { serverUrl }
}

private const val SECONDARY_ALPHA = 0.75f
private const val PILL_ALPHA = 0.1f
