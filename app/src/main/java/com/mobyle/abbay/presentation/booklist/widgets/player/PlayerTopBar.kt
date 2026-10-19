package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem

/** Top bar of the expanded player: collapse · title · speed · lock · overflow. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerTopBar(
    title: String,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onSetDefaultSpeed: (Float) -> Unit,
    isScreenLocked: Boolean,
    onCollapse: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onOpenDetails: () -> Unit,
    onMarkFinished: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    extraMenu: PlayerMenuExtras = PlayerMenuExtras(),
    routeButton: @Composable () -> Unit = {},
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    if (showSpeedSheet) {
        SpeedSheet(speed = speed, onSpeedChange = onSpeedChange, onSetDefault = onSetDefaultSpeed, onDismiss = { showSpeedSheet = false })
    }

    TopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = AbbayColors.content, maxLines = 1, modifier = Modifier.clipToBounds().basicMarquee()) },
        navigationIcon = {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.player_collapse), tint = AbbayColors.content)
            }
        },
        actions = {
            routeButton()
            SpeedButton(speed) { showSpeedSheet = true }
            IconButton(onClick = { onLockToggle(!isScreenLocked) }) {
                Icon(
                    imageVector = if (isScreenLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = stringResource(if (isScreenLocked) R.string.player_unlock else R.string.player_lock),
                    tint = AbbayColors.content,
                )
            }
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.player_more), tint = AbbayColors.content)
                }
                PlayerMenu(
                    isOpen = isMenuOpen,
                    onClose = { isMenuOpen = false },
                    entries = listOf(
                        R.string.player_book_details to onOpenDetails,
                        R.string.player_bookmarks to onOpenBookmarks,
                        R.string.player_history to onOpenHistory,
                        R.string.player_text to extraMenu.onOpenText,
                        R.string.player_characters to extraMenu.onOpenCharacters,
                        R.string.player_statistics to extraMenu.onOpenStatistics,
                        R.string.player_open_text to extraMenu.onOpenBookText,
                        R.string.player_mark_finished to onMarkFinished,
                        R.string.settings to onOpenSettings,
                    ),
                )
            }
        },
    )
}

/** Overflow entries of Phases 8–9: characters, statistics, the book's text files and subtitles. */
class PlayerMenuExtras(
    val onOpenCharacters: () -> Unit = {},
    val onOpenStatistics: () -> Unit = {},
    val onOpenBookText: () -> Unit = {},
    val onOpenText: () -> Unit = {},
)

/** A text plus an icon is wider than an IconButton: a padded clickable row keeps it off the title. */
@Composable
private fun SpeedButton(speed: Float, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Text(text = formatSpeed(speed), color = AbbayColors.content)
        Icon(
            Icons.Default.Speed,
            contentDescription = stringResource(R.string.change_speed_content_description),
            tint = AbbayColors.content,
        )
    }
}

@Composable
private fun PlayerMenu(isOpen: Boolean, onClose: () -> Unit, entries: List<Pair<Int, () -> Unit>>) {
    AbbayDropdownMenu(expanded = isOpen, onDismissRequest = onClose) {
        entries.forEach { (labelResource, action) ->
            AbbayMenuItem(
                text = stringResource(labelResource),
                onClick = {
                    onClose()
                    action()
                },
            )
        }
    }
}
