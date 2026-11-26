package com.mobyle.abbay.presentation.bookdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.model.BookNameSource

/** Where a folder book's title comes from, with the folder it would be named after. */
@Composable
internal fun NameSourceRow(selected: BookNameSource, folderName: String, hasOwnTitle: Boolean, onSelect: (BookNameSource) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { isMenuOpen = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.details_name_source), modifier = Modifier.weight(1f))
                Text(
                    stringResource(selected.labelResource()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            AbbayDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                BookNameSource.entries.forEach { source ->
                    AbbayMenuItem(
                        text = stringResource(source.labelResource()),
                        selected = source == selected,
                        onClick = {
                            isMenuOpen = false
                            onSelect(source)
                        },
                    )
                }
            }
        }
        val hint = if (hasOwnTitle) {
            stringResource(R.string.details_name_source_own_title)
        } else {
            stringResource(R.string.details_name_source_folder, folderName)
        }
        Text(hint, style = MaterialTheme.typography.bodySmall)
    }
}

private fun BookNameSource.labelResource(): Int = when (this) {
    BookNameSource.APP_DEFAULT -> R.string.name_source_app_default
    BookNameSource.FOLDER -> R.string.name_source_folder
    BookNameSource.METADATA -> R.string.name_source_metadata
}
