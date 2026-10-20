package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.library.LibraryTab
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.labelResource
import com.settings.AppSettings

/** Settings → Library tabs: which tabs the library shows and in what order. */
@Composable
internal fun LibraryTabSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    val orderedTabs = LibraryTab.ordered(settings.libraryTabOrder)
    val visibleTabs = settings.visibleLibraryTabs
    SectionHeader(R.string.settings_library_tabs)
    orderedTabs.forEachIndexed { index, tab ->
        val label = stringResource(tab.labelResource())
        val isShown = tab in visibleTabs
        SettingItem(text = label) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoveTabButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    description = stringResource(R.string.settings_tab_move_up, label),
                    enabled = index > 0,
                ) { change { it.copy(libraryTabOrder = orderedTabs.swapped(index, index - 1)) } }
                MoveTabButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    description = stringResource(R.string.settings_tab_move_down, label),
                    enabled = index < orderedTabs.lastIndex,
                ) { change { it.copy(libraryTabOrder = orderedTabs.swapped(index, index + 1)) } }
                // The last shown tab cannot be hidden: the library always has one.
                SettingSwitch(
                    checked = isShown,
                    enabled = !isShown || visibleTabs.size > 1,
                    onCheckedChange = { show ->
                        change { current ->
                            if (show) {
                                current.copy(hiddenLibraryTabs = current.hiddenLibraryTabs - tab)
                            } else {
                                // A hidden default tab goes back to Automatic.
                                current.copy(
                                    hiddenLibraryTabs = current.hiddenLibraryTabs + tab,
                                    libraryTab = current.libraryTab.takeIf { it != tab },
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MoveTabButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        val tint = MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = description, tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA))
    }
}

private fun List<LibraryTab>.swapped(from: Int, to: Int): List<LibraryTab> =
    toMutableList().apply { this[from] = this[to].also { this[to] = this[from] } }

private const val DISABLED_ALPHA = 0.38f
