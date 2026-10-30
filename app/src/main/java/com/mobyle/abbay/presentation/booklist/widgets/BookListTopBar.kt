package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListTopBar(
    openFolderSelector: (() -> Unit),
    openFileSelector: () -> Unit,
    openSettings: () -> Unit,
    isContentEnabled: Boolean = true,
    more: LibraryMenu = LibraryMenu(),
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
        title = {
            Text(
                getString(context, R.string.book_list_top_bar_title),
                style = AbbayTextStyles.bookTitleLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = AbbayColors.content
            )
        },
        actions = {
            if (isContentEnabled) {
                IconButton(onClick = more.onSearch) {
                    Icon(Icons.Rounded.Search, stringResource(R.string.search_title), tint = AbbayColors.content)
                }

                AddContentButton(
                    openFolderSelector = openFolderSelector,
                    openFileSelector = openFileSelector,
                    openLibriVox = more.onLibriVox
                )

                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.library_more), tint = AbbayColors.content)
                }
                AbbayDropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    listOf(
                        R.string.queue_title to more.onQueue,
                        R.string.folders_title to more.onFolders,
                        R.string.statistics_title to more.onStatistics,
                        R.string.settings to openSettings,
                    ).forEach { (label, action) ->
                        AbbayMenuItem(
                            text = stringResource(label),
                            onClick = {
                                showMoreMenu = false
                                action()
                            },
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AddContentButton(openFolderSelector: () -> Unit, openFileSelector: () -> Unit, openLibriVox: () -> Unit) {
    var showAddMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { showAddMenu = true }) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.add_content_description),
            tint = AbbayColors.content
        )
    }
    AbbayDropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
        AbbayMenuItem(
            text = stringResource(R.string.add_folder),
            leadingIconResource = R.drawable.folder_plus,
            onClick = {
                showAddMenu = false
                openFolderSelector()
            }
        )

        AbbayMenuItem(
            text = stringResource(R.string.add_file),
            leadingIconResource = R.drawable.file_plus,
            onClick = {
                showAddMenu = false
                openFileSelector()
            }
        )

        AbbayMenuItem(
            text = stringResource(R.string.librivox_add_menu),
            leadingIcon = Icons.Outlined.Public,
            onClick = {
                showAddMenu = false
                openLibriVox()
            }
        )
    }
}

class LibraryMenu(
    val onSearch: () -> Unit = {},
    val onLibriVox: () -> Unit = {},
    val onQueue: () -> Unit = {},
    val onFolders: () -> Unit = {},
    val onStatistics: () -> Unit = {},
)
