package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton

/** Shown when the locked player is touched; a double tap unlocks it (existing AbbayAndroid behavior). */
@Composable
fun ScreenLockedAlert(onDismissRequest: () -> Unit) {
    AbbayAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    stringResource(R.string.screen_locked),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.screen_locked_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.screen_locked_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            // A TextButton keeps the 48 dp touch target.
            AbbayTextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dismiss), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}
