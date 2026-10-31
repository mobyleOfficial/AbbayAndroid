package com.mobyle.abbay.presentation.audiobookshelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.librivox.ResultsList
import com.mobyle.abbay.presentation.librivox.StateMessage
import com.model.CatalogSeries
import com.model.LibriVoxBook

/** The books of one series, in their order in it. */
@Composable
fun AudiobookshelfSeriesScreen(
    viewModel: AudiobookshelfSeriesViewModel = hiltViewModel(),
    onOpenBook: (LibriVoxBook) -> Unit,
    close: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val libraryBookIds by viewModel.libraryBookIds.collectAsState()
    AbbayScreen(title = stringResource(R.string.audiobookshelf_title), onClose = close) {
        when (val current = state) {
            AudiobookshelfSeriesState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            }
            AudiobookshelfSeriesState.Offline -> StateMessage(
                icon = Icons.Outlined.CloudOff,
                title = stringResource(R.string.librivox_offline_title),
                body = stringResource(R.string.audiobookshelf_offline_body),
                onRetry = viewModel::load,
            )
            AudiobookshelfSeriesState.Failed -> StateMessage(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.audiobookshelf_failed_title),
                body = stringResource(R.string.librivox_failed_body),
                onRetry = viewModel::load,
            )
            AudiobookshelfSeriesState.Empty -> StateMessage(
                icon = Icons.Outlined.LibraryBooks,
                title = stringResource(R.string.audiobookshelf_empty_series_title),
                body = stringResource(R.string.audiobookshelf_empty_series_body),
                onRetry = viewModel::load,
            )
            is AudiobookshelfSeriesState.Books -> ResultsList(
                books = current.books,
                libraryIds = libraryBookIds.keys,
                onOpenBook = onOpenBook,
                overline = { book -> book.series?.let { sequenceLabel(it) } },
                header = { SeriesHeader(name = viewModel.name, bookCount = current.books.size) },
            )
        }
    }
}

/** The series' full name, however long, above its books. */
@Composable
private fun SeriesHeader(name: String, bookCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = AbbayColors.content,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            pluralStringResource(R.plurals.audiobookshelf_series_book_count, bookCount, bookCount),
            style = MaterialTheme.typography.bodyMedium,
            color = AbbayColors.content.copy(alpha = HEADER_SECONDARY_ALPHA),
        )
    }
}

private const val HEADER_SECONDARY_ALPHA = 0.75f

/** "Book 2", or null when the series does not number it. */
@Composable
internal fun sequenceLabel(series: CatalogSeries): String? =
    series.sequence?.takeIf(String::isNotBlank)?.let { stringResource(R.string.audiobookshelf_series_sequence, it) }
