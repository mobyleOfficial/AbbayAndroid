package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbaysDropdownItem
import com.mobyle.abbay.presentation.common.widgets.SVGIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListTopBar(
    openFolderSelector: (() -> Unit),
    openFileSelector: () -> Unit,
    openSettings: () -> Unit,
    onRefresh: () -> Unit,
    hasSelectedFolder: Boolean = false,
    isContentEnabled: Boolean = true,
    isRefreshing: Boolean = false
) {
    val context = LocalContext.current
    var showAddMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                getString(context, R.string.book_list_top_bar_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        actions = {
            if (isContentEnabled) {
                IconButton(
                    onClick = onRefresh,
                    enabled = hasSelectedFolder && !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.background(Color.Transparent),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = if (hasSelectedFolder) 1f else 0.5f)
                        )
                    }
                }

                if (!hasSelectedFolder) {
                    IconButton(
                        onClick = { showAddMenu = true },
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
                    ) {
                        AbbaysDropdownItem(
                            text = "Add Folder",
                            imageUrl = R.drawable.folder_plus,
                            onClick = {
                                showAddMenu = false
                                openFolderSelector()
                            }
                        )

                        AbbaysDropdownItem(
                            text = "Add File",
                            imageUrl = R.drawable.file_plus,
                            onClick = {
                                showAddMenu = false
                                openFileSelector()
                            }
                        )
                    }
                } else {
                    IconButton(onClick = openFileSelector) {
                        SVGIcon(
                            path = R.drawable.file_plus,
                            description = getString(
                                context,
                                R.string.book_list_top_bar_file_button_description
                            )
                        )
                    }
                }

                IconButton(onClick = openSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        "menu",
                        tint = Color.White,
                    )
                }
            }
        }
    )
}