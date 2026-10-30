package com.mobyle.abbay.presentation.librivox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.model.ChapterDownload
import com.model.ChapterDownloadStatus
import com.model.LibriVoxSection
import java.util.Locale

@Composable
internal fun ChapterRow(index: Int, section: LibriVoxSection, download: ChapterDownload?, isTitleLoading: Boolean, onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 6.dp),
    ) {
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.bodySmall,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                section.title,
                style = MaterialTheme.typography.bodyMedium,
                color = AbbayColors.content.copy(alpha = if (isTitleLoading) LOADING_TITLE_ALPHA else 1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (section.durationMs > 0) {
                Text(
                    chapterDuration(section.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                )
            }
        }
        download?.let { ChapterStatus(it, onRetry) }
    }
}

@Composable
private fun ChapterStatus(download: ChapterDownload, onRetry: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
        when (download.status) {
            ChapterDownloadStatus.DOWNLOADED -> Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.librivox_chapter_downloaded),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            ChapterDownloadStatus.DOWNLOADING -> {
                val description = stringResource(R.string.librivox_chapter_downloading, (download.fraction * PERCENT).toInt())
                CircularProgressIndicator(
                    progress = { download.fraction.coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = AbbayColors.content.copy(alpha = TRACK_ALPHA),
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics { contentDescription = description },
                )
            }
            ChapterDownloadStatus.WAITING -> Icon(
                Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.librivox_chapter_waiting),
                tint = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                modifier = Modifier.size(20.dp),
            )
            ChapterDownloadStatus.FAILED -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.librivox_retry), onClick = onRetry),
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.librivox_chapter_failed),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
internal fun ChapterPlaceholderRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clearAndSetSemantics { },
    ) {
        Spacer(Modifier.width(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            PlaceholderBar(width = CHAPTER_PLACEHOLDER_WIDTH)
            PlaceholderBar(width = DURATION_PLACEHOLDER_WIDTH)
        }
    }
}

@Composable
internal fun FailedDetails(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        Text(
            stringResource(R.string.librivox_details_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = AbbayColors.content,
            textAlign = TextAlign.Center,
        )
        AbbayTextButton(onClick = onRetry) {
            Text(stringResource(R.string.librivox_retry))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PlaceholderBar(width: Dp? = null) {
    Box(
        modifier = Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AbbayColors.content.copy(alpha = PLACEHOLDER_ALPHA)),
    )
}

internal fun chapterDuration(durationMs: Long): String {
    val totalSeconds = durationMs / MS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

private const val LOADING_TITLE_ALPHA = 0.5f
private const val PLACEHOLDER_ALPHA = 0.1f
private const val TRACK_ALPHA = 0.15f
private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private val CHAPTER_PLACEHOLDER_WIDTH = 220.dp
private val DURATION_PLACEHOLDER_WIDTH = 48.dp
