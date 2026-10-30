package com.mobyle.abbay.presentation.librivox

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.theme.rememberCoverTint
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.model.LibriVoxDownload

@Composable
fun LibriVoxBookScreen(
    viewModel: LibriVoxBookViewModel = hiltViewModel(),
    onListen: (bookId: String) -> Unit,
    close: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val libraryBookId by viewModel.libraryBookId.collectAsState()
    val download by viewModel.download.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()
    val cellularPromptBytes by viewModel.cellularPromptBytes.collectAsState()
    val listState = rememberLazyListState()
    val isActionScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > ACTION_ITEM_INDEX } }
    val scaffoldState = rememberScaffoldState()
    val addFailedMessage = stringResource(R.string.librivox_add_failed)
    LaunchedEffect(Unit) { viewModel.addFailed.collect { scaffoldState.snackbarHostState.showSnackbar(addFailedMessage) } }

    val action = PrimaryAction(
        libraryBookId = libraryBookId,
        isEnabled = state is LibriVoxBookState.Loaded,
        isBusy = isStarting,
        onDownload = viewModel::download,
        onListen = onListen,
    )

    AbbayScreen(
        title = stringResource(R.string.librivox_title),
        onClose = close,
        scaffoldState = scaffoldState,
        actions = { if (isActionScrolledAway && action.isEnabled) CompactActionButton(action) },
    ) {
        BookPage(state, action, download, listState, viewModel)
    }

    cellularPromptBytes?.let { bytes -> CellularDownloadDialog(bytes, onAnswer = viewModel::answerCellularPrompt) }
}

@Composable
private fun BookPage(
    state: LibriVoxBookState,
    action: PrimaryAction,
    download: LibriVoxDownload?,
    listState: LazyListState,
    viewModel: LibriVoxBookViewModel,
) {
    val book = state.book
    val tint = rememberCoverTint(book.coverUrl)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to tint, HERO_FADE to MaterialTheme.colorScheme.primary)),
    ) {
        item { Hero(book, isPlaceholder = state !is LibriVoxBookState.Loaded) }
        item { ActionButton(action, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) }
        download?.let { current -> item { DownloadSummary(current, onRetry = viewModel::retryFailedChapters) } }
        when (state) {
            is LibriVoxBookState.Failed -> item { FailedDetails(onRetry = viewModel::load) }
            is LibriVoxBookState.Placeholder -> placeholderContent()
            is LibriVoxBookState.Loaded -> loadedContent(state, download, onRetry = viewModel::retryFailedChapters)
        }
        item { Credit() }
    }
}

private fun LazyListScope.placeholderContent() {
    item { DescriptionPlaceholder() }
    item { SectionHeading() }
    items(PLACEHOLDER_ROWS) { ChapterPlaceholderRow() }
}

private fun LazyListScope.loadedContent(state: LibriVoxBookState.Loaded, download: LibriVoxDownload?, onRetry: () -> Unit) {
    item { if (state.isAwaitingTitles) DescriptionPlaceholder() else Description(state.book.description) }
    item { SectionHeading() }
    itemsIndexed(state.book.sections) { index, section ->
        ChapterRow(
            index = index,
            section = section,
            download = download?.chapters?.getOrNull(index),
            isTitleLoading = state.isAwaitingTitles,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun Credit() {
    Text(
        stringResource(R.string.librivox_credit),
        style = MaterialTheme.typography.bodySmall,
        color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    )
}

@Composable
private fun CellularDownloadDialog(bytes: Long, onAnswer: (useMobileData: Boolean?) -> Unit) {
    AbbayAlertDialog(
        onDismissRequest = { onAnswer(null) },
        icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
        title = { Text(stringResource(R.string.librivox_cellular_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                stringResource(R.string.librivox_cellular_body, Formatter.formatShortFileSize(LocalContext.current, bytes)),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            AbbayTextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.librivox_cellular_continue)) }
        },
        dismissButton = {
            AbbayTextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.librivox_cellular_wait)) }
        },
    )
}

internal class PrimaryAction(
    val libraryBookId: String?,
    val isEnabled: Boolean,
    val isBusy: Boolean,
    val onDownload: () -> Unit,
    val onListen: (String) -> Unit,
) {
    val isInLibrary: Boolean get() = libraryBookId != null

    fun perform() {
        val bookId = libraryBookId
        if (bookId != null) onListen(bookId) else onDownload()
    }
}

private const val ACTION_ITEM_INDEX = 1
private const val PLACEHOLDER_ROWS = 6
private const val HERO_FADE = 0.35f
internal const val SECONDARY_ALPHA = 0.75f
internal const val PERCENT = 100
