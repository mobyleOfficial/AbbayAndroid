package com.mobyle.abbay.presentation.librivox

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.theme.rememberCoverTint
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.model.LibriVoxDownload
import com.model.LibriVoxDownloadMode

@Composable
fun LibriVoxBookScreen(
    viewModel: LibriVoxBookViewModel = hiltViewModel(),
    onListen: (bookId: String) -> Unit,
    close: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val libraryBookId by viewModel.libraryBookId.collectAsState()
    val download by viewModel.download.collectAsState()
    val starting by viewModel.starting.collectAsState()
    val cellularPromptBytes by viewModel.cellularPromptBytes.collectAsState()
    var isConfirmingRemoval by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isActionScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > ACTION_ITEM_INDEX } }
    val scaffoldState = rememberScaffoldState()
    val addFailedMessage = stringResource(R.string.librivox_add_failed)
    LaunchedEffect(Unit) { viewModel.addFailed.collect { scaffoldState.snackbarHostState.showSnackbar(addFailedMessage) } }
    LaunchedEffect(Unit) { viewModel.listen.collect(onListen) }

    val action = PrimaryAction(
        libraryBookId = libraryBookId,
        isEnabled = state is LibriVoxBookState.Loaded,
        starting = starting,
        onDownload = viewModel::download,
        onListen = onListen,
        onStream = viewModel::listenWithoutDownloading,
    )

    AbbayScreen(
        title = stringResource(R.string.librivox_title),
        onClose = close,
        scaffoldState = scaffoldState,
        actions = {
            if (isActionScrolledAway && action.isEnabled) CompactActionButton(action)
            download?.takeIf { it.isInProgress || it.hasDownloadedChapters }?.let { current ->
                DownloadMenu(isComplete = current.isComplete, onRemove = { isConfirmingRemoval = true })
            }
        },
    ) {
        BookPage(state, action, download, listState, viewModel)
    }

    cellularPromptBytes?.let { bytes -> CellularDownloadDialog(bytes, onAnswer = viewModel::answerCellularPrompt) }
    if (isConfirmingRemoval) {
        AbbayActionDialog(
            title = stringResource(R.string.librivox_remove_download_title),
            body = stringResource(R.string.librivox_remove_download_body),
            actionButtonTitle = stringResource(R.string.librivox_remove_download_confirm),
            onDismiss = { isConfirmingRemoval = false },
            onAction = {
                isConfirmingRemoval = false
                viewModel.removeDownload()
            },
        )
    }
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
        item {
            ActionButton(
                action,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (action.isInLibrary) 16.dp else 8.dp),
            )
        }
        if (!action.isInLibrary) item { StreamButton(action) }
        download?.let { current ->
            item { DownloadSummary(current, onPause = viewModel::pauseDownload, onResume = viewModel::resumeDownload) }
        }
        when (state) {
            is LibriVoxBookState.Failed -> item { FailedDetails(onRetry = viewModel::load) }
            is LibriVoxBookState.Placeholder -> placeholderContent()
            is LibriVoxBookState.Loaded -> loadedContent(
                state,
                download?.takeUnless { it.mode == LibriVoxDownloadMode.OFF },
                onRetry = viewModel::resumeDownload,
            )
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
private fun StreamButton(action: PrimaryAction) {
    val label = stringResource(R.string.librivox_listen_without_download)
    val isStreaming = action.starting == LibriVoxAddMode.STREAM
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(AbbayColors.content.copy(alpha = STREAM_BUTTON_ALPHA))
            .clickable(enabled = action.isEnabled && action.isIdle, role = Role.Button, onClick = action.onStream)
            .semantics { if (isStreaming) contentDescription = label },
    ) {
        if (isStreaming) {
            CircularProgressIndicator(color = AbbayColors.content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Outlined.Podcasts, contentDescription = null, tint = AbbayColors.content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = AbbayColors.content.copy(alpha = if (action.isEnabled) 1f else SECONDARY_ALPHA)
            )
        }
    }
}

@Composable
private fun DownloadMenu(isComplete: Boolean, onRemove: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { isExpanded = true }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.librivox_more_options), tint = AbbayColors.content)
    }
    AbbayDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        AbbayMenuItem(
            text = stringResource(if (isComplete) R.string.librivox_remove_download else R.string.librivox_cancel_download),
            leadingIcon = Icons.Outlined.Delete,
            onClick = {
                isExpanded = false
                onRemove()
            },
        )
    }
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
    val starting: LibriVoxAddMode?,
    val onDownload: () -> Unit,
    val onListen: (String) -> Unit,
    val onStream: () -> Unit,
) {
    val isInLibrary: Boolean get() = libraryBookId != null

    val isBusy: Boolean get() = starting == LibriVoxAddMode.DOWNLOAD

    val isIdle: Boolean get() = starting == null

    fun perform() {
        val bookId = libraryBookId
        if (bookId != null) onListen(bookId) else onDownload()
    }
}

private const val ACTION_ITEM_INDEX = 1
private const val STREAM_BUTTON_ALPHA = 0.12f
private const val PLACEHOLDER_ROWS = 6
private const val HERO_FADE = 0.35f
internal const val SECONDARY_ALPHA = 0.75f
internal const val PERCENT = 100
