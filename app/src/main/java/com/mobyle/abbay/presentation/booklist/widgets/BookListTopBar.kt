package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.SVGIcon

@Composable
fun BookListTopBar(openFolderSelector: () -> Unit, openFileSelector: () -> Unit) {
    val context = LocalContext.current

    TopAppBar(title = {
        Text(
            getString(context, R.string.book_list_top_bar_title),
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }, actions = {
        IconButton(onClick = openFolderSelector) {
            SVGIcon(
                R.drawable.folder_plus,
                getString(context, R.string.book_list_top_bar_folder_button_description)
            )
        }
        IconButton(onClick = openFileSelector) {
            SVGIcon(
                R.drawable.file_plus,
                getString(context, R.string.book_list_top_bar_file_button_description)
            )
        }
    })
}