package com.mobyle.abbay.presentation.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.model.CompanionDocument

/** Lists the book's text files; the chosen one opens in another app with a read grant. */
@Composable
fun CompanionDocumentsDialog(viewModel: CompanionDocumentsViewModel) {
    val bookId by viewModel.bookId.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    bookId ?: return

    AbbayAlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(stringResource(R.string.documents_title)) },
        text = {
            val list = documents
            when {
                list == null -> CircularProgressIndicator()
                list.isEmpty() -> Text(stringResource(R.string.documents_none))
                else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    list.forEach { document ->
                        DocumentRow(document) {
                            if (open(context, document)) viewModel.opened(document)
                        }
                    }
                }
            }
        },
        confirmButton = { AbbayTextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DocumentRow(document: CompanionDocument, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(document.name, style = MaterialTheme.typography.bodyLarge)
            if (document.isLastOpened) Text(stringResource(R.string.documents_last_opened), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** ACTION_VIEW with a read grant; false when no app can show the file. */
private fun open(context: Context, document: CompanionDocument): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(document.uri), mimeTypeOf(document.name))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.documents_no_app, Toast.LENGTH_SHORT).show()
        false
    }
}

private fun mimeTypeOf(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "epub" -> "application/epub+zip"
    "fb2" -> "application/x-fictionbook+xml"
    else -> "text/plain"
}
