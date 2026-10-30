package com.mobyle.abbay.presentation.librivox

import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.statistics.listeningTime
import com.model.LibriVoxBook
import java.util.Locale

@Composable
internal fun Hero(book: LibriVoxBook, isPlaceholder: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp),
    ) {
        BookThumbnail(book.coverUrl, modifier = Modifier.size(HERO_COVER), shadow = true)
        Spacer(Modifier.height(10.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = AbbayColors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (book.authors.isNotBlank()) {
            Text(
                book.authors,
                style = MaterialTheme.typography.bodyMedium,
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                textAlign = TextAlign.Center,
            )
        }
        if (isPlaceholder) {
            PlaceholderBar(width = METADATA_PLACEHOLDER_WIDTH)
        } else {
            Text(
                metadataLine(book),
                style = MaterialTheme.typography.bodySmall,
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun metadataLine(book: LibriVoxBook): String {
    val context = LocalContext.current
    return listOfNotNull(
        book.durationMs.takeIf { it > 0 }?.let { listeningTime(it) },
        pluralStringResource(R.plurals.librivox_chapter_count, book.sections.size, book.sections.size),
        book.totalBytes.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) },
        languageName(book.language, Locale.getDefault()),
    ).joinToString(" · ")
}

private fun languageName(code: String, locale: Locale): String? {
    if (code.isBlank()) return null
    val name = Locale.forLanguageTag(code).getDisplayLanguage(locale)
    return name.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }?.replaceFirstChar { it.titlecase(locale) }
}

@Composable
internal fun ActionButton(action: PrimaryAction, modifier: Modifier = Modifier) {
    val label = stringResource(if (action.isInLibrary) R.string.librivox_listen else R.string.librivox_download)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = if (action.isEnabled) 1f else DISABLED_ALPHA))
            .clickable(enabled = action.isEnabled && !action.isBusy, role = Role.Button, onClick = action::perform)
            .semantics { if (action.isBusy) contentDescription = label },
    ) {
        if (action.isBusy) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Icon(
                if (action.isInLibrary) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary,
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
internal fun CompactActionButton(action: PrimaryAction) {
    AbbayTextButton(onClick = action::perform, enabled = !action.isBusy) {
        Icon(if (action.isInLibrary) Icons.Rounded.PlayArrow else Icons.Rounded.Download, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(if (action.isInLibrary) R.string.librivox_listen else R.string.librivox_download))
    }
}

@Composable
internal fun Description(text: String) {
    if (text.isBlank()) return
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .animateContentSize(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = AbbayColors.content.copy(alpha = DESCRIPTION_ALPHA),
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        AbbayTextButton(onClick = { isExpanded = !isExpanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(stringResource(if (isExpanded) R.string.librivox_less else R.string.librivox_more))
        }
    }
}

@Composable
internal fun DescriptionPlaceholder() {
    val loading = stringResource(R.string.librivox_loading)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = loading },
    ) {
        PlaceholderBar()
        PlaceholderBar()
        PlaceholderBar(width = DESCRIPTION_LAST_LINE_WIDTH)
    }
}

@Composable
internal fun SectionHeading() {
    Text(
        stringResource(R.string.librivox_chapters_heading),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = AbbayColors.content,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

private const val COLLAPSED_DESCRIPTION_LINES = 3
private const val DESCRIPTION_ALPHA = 0.85f
private const val DISABLED_ALPHA = 0.4f
private val HERO_COVER = 168.dp
private val METADATA_PLACEHOLDER_WIDTH = 180.dp
private val DESCRIPTION_LAST_LINE_WIDTH = 200.dp
