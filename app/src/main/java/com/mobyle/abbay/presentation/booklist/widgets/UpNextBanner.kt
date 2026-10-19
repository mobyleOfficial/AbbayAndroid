package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.player.UpNext

/** The countdown to the next queued book, above the library and the player. */
@Composable
fun UpNextBanner(upNext: UpNext?, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    // Keeps the last value while the banner slides out.
    var shown by remember { mutableStateOf(upNext) }
    if (upNext != null) shown = upNext

    AnimatedVisibility(
        visible = upNext != null,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier.statusBarsPadding().padding(12.dp),
    ) {
        val current = shown ?: return@AnimatedVisibility
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                Text(
                    stringResource(R.string.up_next_message, current.title, current.secondsLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                AbbayTextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}
