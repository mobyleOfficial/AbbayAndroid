package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.model.Book

@Composable
internal fun PillsRow(book: Book, position: PlayerPosition, chapters: ChaptersToggle, actions: PlayerActions) {
    var showSpeedSheet by remember { mutableStateOf(false) }
    if (showSpeedSheet) {
        SpeedSheet(
            speed = book.speed,
            onSpeedChange = actions.onSpeedChange,
            onSetDefault = actions.onSetDefaultSpeed,
            onDismiss = { showSpeedSheet = false },
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val isNormalSpeed = book.speed == NORMAL_SPEED
        PlayerPill(
            text = formatSpeed(book.speed),
            description = stringResource(R.string.change_speed_content_description),
            icon = Icons.Rounded.Speed,
            color = if (isNormalSpeed) AbbayColors.content else MaterialTheme.colorScheme.tertiary,
            onClick = { showSpeedSheet = true },
        )
        Spacer(modifier = Modifier.weight(1f))
        if (position.chapterCount > 1) {
            PlayerPill(
                text = stringResource(R.string.player_chapters),
                description = stringResource(if (chapters.isOpen) R.string.player_close_chapters else R.string.player_chapters),
                icon = if (chapters.isOpen) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.List,
                color = AbbayColors.content,
                onClick = chapters.onToggle,
            )
        }
    }
}

@Composable
private fun PlayerPill(text: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(shape)
                .background(AbbayColors.content.copy(alpha = PILL_FILL_ALPHA))
                .border(0.5.dp, AbbayColors.content.copy(alpha = PILL_EDGE_ALPHA), shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_FIGURES),
                color = color,
                maxLines = 1,
            )
        }
    }
}

private const val PILL_FILL_ALPHA = 0.12f
private const val PILL_EDGE_ALPHA = 0.15f
private const val NORMAL_SPEED = 1f
