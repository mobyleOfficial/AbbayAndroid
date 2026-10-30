package com.mobyle.abbay.presentation.bookmarks

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.SnackbarResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Bookmark
import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel = hiltViewModel(), onJump: (Long) -> Unit, close: () -> Unit) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val details by viewModel.details.collectAsState()
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Bookmark?>(null) }
    val deletedMessage = stringResource(R.string.bookmark_deleted)
    val undoLabel = stringResource(R.string.undo)

    editing?.let { bookmark ->
        BookmarkNoteDialog(
            initialNote = bookmark.note,
            onSave = { note ->
                viewModel.saveNote(bookmark.id, note)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    val delete: (Bookmark) -> Unit = { bookmark ->
        viewModel.delete(bookmark)
        scope.launch {
            val result = scaffoldState.snackbarHostState.showSnackbar(deletedMessage, undoLabel)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(bookmark)
        }
    }

    AbbayScreen(
        title = stringResource(R.string.bookmarks_title),
        onClose = close,
        scaffoldState = scaffoldState,
        actions = {
            val list = bookmarks.orEmpty()
            if (list.isNotEmpty()) {
                IconButton(onClick = { shareBookmarks(context, details?.title.orEmpty(), list) }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.bookmarks_share), tint = AbbayColors.content)
                }
            }
        },
    ) {
        val list = bookmarks ?: return@AbbayScreen
        if (list.isEmpty()) {
            EmptyMessage(stringResource(R.string.bookmarks_empty))
            return@AbbayScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { bookmark ->
                SwipeToDelete(onDelete = { delete(bookmark) }) {
                    BookmarkRow(bookmark, onJump = {
                        onJump(bookmark.bookPositionMs)
                    }, onEdit = { editing = bookmark }, onDelete = { delete(bookmark) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onJump: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val position = bookmark.bookPositionMs.toHHMMSS()
    val deleteLabel = stringResource(R.string.bookmark_delete)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = stringResource(R.string.bookmark_go_to, position), onClick = onJump)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    }
                )
            }
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(position, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            if (bookmark.chapterTitle.isNotBlank()) {
                Text(
                    bookmark.chapterTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AbbayColors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (bookmark.note.isNotBlank()) {
                Text(
                    bookmark.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AbbayColors.content,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.bookmark_edit_note), tint = AbbayColors.content)
        }
    }
}

@Composable
fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = AbbayColors.content)
    }
}

private fun shareBookmarks(context: Context, bookTitle: String, bookmarks: List<Bookmark>) {
    val title = context.getString(R.string.bookmarks_share_title, bookTitle)
    val text = BookmarkExport.toText(title, bookmarks)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.bookmarks_share)))
}
