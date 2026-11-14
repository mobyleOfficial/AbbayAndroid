package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R

@Composable
fun AbbayActionDialog(
    title: String,
    body: String,
    actionButtonTitle: String,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    icon: ImageVector = Icons.Default.Delete,
    isDestructive: Boolean = true,
    showCancel: Boolean = true,
) {
    val accent = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    AbbayAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, tint = accent) },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            AbbayTextButton(onClick = onAction, colors = abbayTextButtonColors(isDestructive)) { Text(actionButtonTitle) }
        },
        dismissButton = if (showCancel) {
            { AbbayTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else {
            null
        },
    )
}
