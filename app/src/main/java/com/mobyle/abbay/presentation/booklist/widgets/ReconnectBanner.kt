package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton

/** "Folder access lost — reconnect". Icon plus text, never color alone. */
@Composable
fun ReconnectBanner(folderName: String, onReconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Text(
            text = stringResource(R.string.library_folder_access_lost, folderName),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        AbbayTextButton(onClick = onReconnect) {
            Text(stringResource(R.string.library_reconnect), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
