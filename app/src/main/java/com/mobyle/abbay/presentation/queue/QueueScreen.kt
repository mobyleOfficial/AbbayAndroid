package com.mobyle.abbay.presentation.queue

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.SimpleBookRow
import com.model.Book
import kotlin.math.roundToInt

@Composable
fun QueueScreen(viewModel: QueueViewModel = hiltViewModel(), onPlay: (Book) -> Unit, close: () -> Unit) {
    val queue by viewModel.queue.collectAsState()

    AbbayScreen(title = stringResource(R.string.queue_title), onClose = close) {
        val books = queue ?: return@AbbayScreen
        if (books.isEmpty()) {
            EmptyMessage(stringResource(R.string.queue_empty))
            return@AbbayScreen
        }
        ReorderableQueue(books, onPlay = onPlay, onMove = viewModel::move, onRemove = viewModel::remove)
    }
}

@Composable
private fun ReorderableQueue(books: List<Book>, onPlay: (Book) -> Unit, onMove: (Book, Int) -> Unit, onRemove: (Book) -> Unit) {
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val moveUp = stringResource(R.string.queue_move_up)
    val moveDown = stringResource(R.string.queue_move_down)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
            val isDragged = draggedId == book.id
            SimpleBookRow(
                book = book,
                onClick = { onPlay(book) },
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffset else 0f
                        shadowElevation = if (isDragged) DRAG_ELEVATION else 0f
                    }
                    .semantics {
                        customActions = listOfNotNull(
                            CustomAccessibilityAction(moveUp) { onMove(book, index - 1).let { true } }.takeIf { index > 0 },
                            CustomAccessibilityAction(moveDown) { onMove(book, index + 1).let { true } }.takeIf { index < books.lastIndex },
                        )
                    },
            ) {
                DragHandle(
                    key = "${book.id}:$index:${books.size}",
                    onStart = {
                        draggedId = book.id
                        dragOffset = 0f
                    },
                    onDrag = { amount -> dragOffset += amount },
                    onEnd = { isCanceled ->
                        val targetIndex = (index + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, books.lastIndex)
                        if (!isCanceled && targetIndex != index) onMove(book, targetIndex)
                        draggedId = null
                        dragOffset = 0f
                    },
                )
                IconButton(onClick = { onRemove(book) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.queue_remove, book.name),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle(key: String, onStart: () -> Unit, onDrag: (Float) -> Unit, onEnd: (Boolean) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .pointerInput(key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount.y)
                    },
                    onDragEnd = { onEnd(false) },
                    onDragCancel = { onEnd(true) },
                )
            },
    ) {
        Icon(Icons.Default.DragHandle, contentDescription = stringResource(R.string.queue_drag), tint = AbbayColors.content)
    }
}

private val ROW_HEIGHT = 72.dp
private const val DRAG_ELEVATION = 12f
