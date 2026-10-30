package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.library.LibraryTab
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton

@Composable
fun LibraryTabEmptyState(
    tab: LibraryTab,
    visibleTabs: List<LibraryTab>,
    isScanning: Boolean,
    onShowTab: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (isScanning) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(ICON_SIZE.dp))
            EmptyStateTexts(R.string.library_scanning_title, R.string.library_scanning_body)
            return@Column
        }
        val content = tab.emptyStateContent()
        Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(ICON_SIZE.dp))
        EmptyStateTexts(content.titleResource, content.bodyResource)
        content.action?.takeIf { (_, target) -> target in visibleTabs }?.let { (labelResource, target) ->
            AbbayTextButton(onClick = { onShowTab(target) }) { Text(stringResource(labelResource)) }
        }
    }
}

@Composable
private fun EmptyStateTexts(titleResource: Int, bodyResource: Int) {
    Text(
        stringResource(titleResource),
        style = MaterialTheme.typography.titleMedium,
        color = AbbayColors.content,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(bodyResource),
        style = MaterialTheme.typography.bodyMedium,
        color = AbbayColors.content,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = BODY_MAX_WIDTH.dp),
    )
}

private class EmptyStateContent(
    val icon: ImageVector,
    val titleResource: Int,
    val bodyResource: Int,
    val action: Pair<Int, LibraryTab>? = null,
)

private fun LibraryTab.emptyStateContent(): EmptyStateContent = when (this) {
    LibraryTab.IN_PROGRESS -> EmptyStateContent(
        Icons.Outlined.Headphones,
        R.string.library_empty_in_progress_title,
        R.string.library_empty_in_progress_body,
        R.string.library_empty_show_new to LibraryTab.NEW,
    )
    LibraryTab.NEW -> EmptyStateContent(
        Icons.Outlined.AutoStories,
        R.string.library_empty_new_title,
        R.string.library_empty_new_body,
        R.string.library_empty_show_in_progress to LibraryTab.IN_PROGRESS,
    )
    LibraryTab.FINISHED -> EmptyStateContent(
        Icons.Outlined.TaskAlt,
        R.string.library_empty_finished_title,
        R.string.library_empty_finished_body,
        R.string.library_empty_show_in_progress to LibraryTab.IN_PROGRESS,
    )
    LibraryTab.ALL -> EmptyStateContent(
        Icons.AutoMirrored.Outlined.LibraryBooks,
        R.string.library_empty_all_title,
        R.string.library_empty_all_body,
    )
}

private const val ICON_SIZE = 64
private const val BODY_MAX_WIDTH = 320
