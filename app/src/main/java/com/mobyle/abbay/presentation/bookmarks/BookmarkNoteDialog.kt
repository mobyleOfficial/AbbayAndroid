package com.mobyle.abbay.presentation.bookmarks

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.model.Bookmark

/** Note of a bookmark, up to [Bookmark.MAX_NOTE_LENGTH] characters. */
@Composable
fun BookmarkNoteDialog(initialNote: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable { mutableStateOf(initialNote) }
    AbbayAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bookmark_edit_note)) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(Bookmark.MAX_NOTE_LENGTH) },
                label = { Text(stringResource(R.string.bookmark_note)) },
                supportingText = { Text(stringResource(R.string.bookmark_note_counter, note.length, Bookmark.MAX_NOTE_LENGTH)) },
                modifier = Modifier.heightIn(min = 120.dp),
            )
        },
        confirmButton = { AbbayTextButton(onClick = { onSave(note) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { AbbayTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
