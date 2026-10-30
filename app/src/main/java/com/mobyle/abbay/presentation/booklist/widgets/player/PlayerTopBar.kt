package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.CircleIconButton

@Composable
fun PlayerTopBar(
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

    Row(
        modifier = modifier.heightIn(min = 56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(R.string.player_collapse),
            onClick = onCollapse,
        )
        Spacer(modifier = Modifier.weight(1f))
        routeButton()
        IconButton(onClick = { onLockToggle(!isScreenLocked) }) {
            Icon(
                imageVector = if (isScreenLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = stringResource(if (isScreenLocked) R.string.player_unlock else R.string.player_lock),
                tint = AbbayColors.content,
            )
        }
        Box {
            IconButton(onClick = { isMenuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.player_more), tint = AbbayColors.content)
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
    }
}

class PlayerMenuExtras(
    val onOpenCharacters: () -> Unit = {},
    val onOpenStatistics: () -> Unit = {},
    val onOpenBookText: () -> Unit = {},
    val onOpenText: () -> Unit = {},
)

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
