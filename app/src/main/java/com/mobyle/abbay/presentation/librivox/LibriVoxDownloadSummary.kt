package com.mobyle.abbay.presentation.librivox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.model.LibriVoxDownload
import com.model.LibriVoxDownloadMode

@Composable
internal fun DownloadSummary(download: LibriVoxDownload, onPause: () -> Unit, onResume: () -> Unit) {
    val percent = (download.fraction * PERCENT).toInt()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
    ) {
        when {
            download.isComplete -> StatusText(Icons.Outlined.CheckCircle, stringResource(R.string.librivox_downloaded))
            download.mode == LibriVoxDownloadMode.OFF -> StatusWithAction(
                Icons.Outlined.CloudQueue,
                stringResource(R.string.librivox_streaming_only),
                actionLabel = stringResource(R.string.librivox_download),
                onAction = onResume,
            )
            download.mode == LibriVoxDownloadMode.PAUSED -> {
                StatusWithAction(
                    Icons.Outlined.PauseCircle,
                    stringResource(R.string.librivox_paused, percent),
                    actionLabel = stringResource(R.string.librivox_resume),
                    onAction = onResume,
                )
                ProgressCapsule(fraction = download.fraction, height = 4.dp, fill = AbbayColors.content.copy(alpha = SECONDARY_ALPHA))
            }
            download.hasFailures -> StatusWithAction(
                Icons.Outlined.ErrorOutline,
                stringResource(R.string.librivox_download_failed),
                actionLabel = stringResource(R.string.librivox_retry),
                onAction = onResume,
            )
            else -> {
                StatusWithAction(
                    Icons.Outlined.Downloading,
                    stringResource(R.string.librivox_downloading, percent),
                    actionLabel = stringResource(R.string.librivox_pause),
                    onAction = onPause,
                )
                ProgressCapsule(fraction = download.fraction, height = 4.dp)
                Text(
                    stringResource(R.string.librivox_listen_while_downloading),
                    style = MaterialTheme.typography.bodySmall,
                    color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun StatusWithAction(icon: ImageVector, text: String, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { StatusText(icon, text) }
        AbbayTextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun StatusText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AbbayColors.content)
    }
}
