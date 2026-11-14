package com.mobyle.abbay.presentation.readalong

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import java.util.Locale
import kotlin.math.abs

/**
 * A round button beside the language, showing the adjustment once there is one (*+1.5 s*). It opens the adjustment:
 * earlier and later by [com.readalong.ReadAlongSync.STEP_MS], how far the text is moved, the touch-and-hold tip, and reset.
 */
@Composable
internal fun SyncMenu(sync: ReadAlongSyncControls, isHidden: Boolean, isOpen: Boolean, onOpenChange: (Boolean) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val label = stringResource(R.string.read_along_sync)
    val status = syncStatus(sync.offsetMs, locale)
    Box {
        AnimatedVisibility(visible = !isHidden || isOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = label) { onOpenChange(true) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        stateDescription = status
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = CONTROL_FILL_ALPHA))
                        .padding(horizontal = if (sync.offsetMs == 0L) 8.dp else 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    if (sync.offsetMs != 0L) {
                        Text(
                            signedSeconds(sync.offsetMs, locale),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { onOpenChange(false) }) {
            SyncAdjustment(sync, status)
        }
    }
}

/** The menu's content: it stays open while the buttons are tapped, so the text can be watched settling. */
@Composable
private fun SyncAdjustment(sync: ReadAlongSyncControls, status: String) {
    Text(
        stringResource(R.string.read_along_sync),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        val later = stringResource(R.string.read_along_sync_later)
        val earlier = stringResource(R.string.read_along_sync_earlier)
        IconButton(onClick = { sync.onNudge(-1) }, modifier = Modifier.semantics { contentDescription = later }) {
            Icon(Icons.Rounded.Remove, contentDescription = null)
        }
        Text(
            status,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = STATUS_WIDTH).semantics { liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = { sync.onNudge(1) }, modifier = Modifier.semantics { contentDescription = earlier }) {
            Icon(Icons.Rounded.Add, contentDescription = null)
        }
    }
    Text(
        stringResource(R.string.read_along_sync_tip),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.widthIn(max = TIP_WIDTH).padding(horizontal = 16.dp, vertical = 8.dp),
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.read_along_sync_reset)) },
        enabled = sync.offsetMs != 0L,
        onClick = sync.onReset,
    )
}

/** *In sync*, *Text 1.5 s ahead*, *Text 0.5 s behind*. */
@Composable
private fun syncStatus(offsetMs: Long, locale: Locale): String = when {
    offsetMs > 0 -> stringResource(R.string.read_along_sync_ahead, seconds(offsetMs, locale))
    offsetMs < 0 -> stringResource(R.string.read_along_sync_behind, seconds(offsetMs, locale))
    else -> stringResource(R.string.read_along_sync_in_sync)
}

private fun seconds(offsetMs: Long, locale: Locale): String = String.format(locale, "%.1f", abs(offsetMs) / MILLIS_PER_SECOND)

private fun signedSeconds(offsetMs: Long, locale: Locale): String = (if (offsetMs > 0) "+" else "−") + seconds(offsetMs, locale) + "\u00A0s"

private val MIN_TOUCH = 48.dp
private val STATUS_WIDTH = 160.dp
private val TIP_WIDTH = 280.dp
private const val CONTROL_FILL_ALPHA = 0.4f
private const val MILLIS_PER_SECOND = 1_000.0
