package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.SVGIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListTopBar(
    openFolderSelector: () -> Unit,
    openFileSelector: () -> Unit,
    openSettings: () -> Unit,
) {
    val context = LocalContext.current

    TopAppBar(
        title = {
            Text(
                getString(context, R.string.book_list_top_bar_title),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        actions = {
//            IconButton(onClick = openFolderSelector) {
//                SVGIcon(
//                    path = R.drawable.folder_plus,
//                    description = getString(
//                        context,
//                        R.string.book_list_top_bar_folder_button_description
//                    )
//                )
//            }
            IconButton(onClick = openFileSelector) {
                SVGIcon(
                    path = R.drawable.file_plus,
                    description = getString(
                        context,
                        R.string.book_list_top_bar_file_button_description
                    )
                )
            }
            IconButton(onClick = openSettings) {
                Icon(
                    Icons.Filled.Settings,
                    "menu",
                    tint = Color.White,
                )
            }
        })
}