package com.mobyle.abbay.presentation.readalong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R

/**
 * Before the text first shows: where it comes from. The book's EPUB, when it sits in the audiobook's folder; otherwise
 * text generated from the audio, which gets words wrong.
 */
@Composable
fun ReadAlongIntroDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.FormatQuote, contentDescription = null) },
        title = { Text(stringResource(R.string.read_along_intro_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.read_along_intro_epub))
                Text(stringResource(R.string.read_along_intro_generated))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.read_along_intro_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.read_along_intro_dismiss)) } },
    )
}
