package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.model.Book
import com.model.BookState

/** Long-press actions of a library item. */
@Composable
fun BookActionsMenu(
    book: Book?,
    onDismiss: () -> Unit,
    onPlay: (Book) -> Unit,
    onMark: (Book, BookState) -> Unit,
    onRemove: (Book) -> Unit,
    onEditDetails: (Book) -> Unit,
    extras: BookMenuExtras = BookMenuExtras(),
) {
    AbbayDropdownMenu(expanded = book != null, onDismissRequest = onDismiss) {
        book ?: return@AbbayDropdownMenu
        BookAction(R.string.book_action_play, onDismiss) { onPlay(book) }
        BookAction(R.string.book_action_play_next, onDismiss) { extras.onQueue(book, true) }
        BookAction(R.string.book_action_add_to_queue, onDismiss) { extras.onQueue(book, false) }
        if (book.state != BookState.NEW) {
            BookAction(R.string.book_action_mark_new, onDismiss) { onMark(book, BookState.NEW) }
        }
        if (book.state != BookState.FINISHED) {
            BookAction(R.string.book_action_mark_finished, onDismiss) { onMark(book, BookState.FINISHED) }
        }
        BookAction(R.string.book_action_edit_details, onDismiss) { onEditDetails(book) }
        BookAction(R.string.book_action_open_text, onDismiss) { extras.onOpenText(book) }
        BookAction(R.string.book_action_show_in_folder, onDismiss) { extras.onShowInFolder(book) }
        BookAction(R.string.book_action_remove, onDismiss) { onRemove(book) }
    }
}

/** Queue, companion documents and the folder view. */
class BookMenuExtras(
    val onQueue: (Book, playNext: Boolean) -> Unit = { _, _ -> },
    val onOpenText: (Book) -> Unit = {},
    val onShowInFolder: (Book) -> Unit = {},
)

@Composable
private fun BookAction(labelResource: Int, onDismiss: () -> Unit, onSelected: () -> Unit) {
    AbbayMenuItem(
        text = stringResource(labelResource),
        onClick = {
            onDismiss()
            onSelected()
        }
    )
}
