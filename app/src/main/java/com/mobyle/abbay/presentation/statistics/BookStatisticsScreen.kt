package com.mobyle.abbay.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import java.util.Locale

@Composable
fun BookStatisticsScreen(viewModel: BookStatisticsViewModel = hiltViewModel(), onOpenAll: (() -> Unit)?, close: () -> Unit) {
    val book by viewModel.book.collectAsState()
    val statistics by viewModel.statistics.collectAsState()

    AbbayScreen(title = stringResource(R.string.book_statistics_title), onClose = close) {
        val stats = statistics ?: return@AbbayScreen
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            book?.let { Text(it.name, style = MaterialTheme.typography.titleLarge, color = AbbayColors.content) }
            Figure(stringResource(R.string.book_statistics_listened), listeningTime(stats.totalWallTimeMs))
            Figure(
                stringResource(R.string.book_statistics_sessions),
                stats.sessions.toString(),
                stringResource(R.string.book_statistics_sessions_hint),
            )
            Figure(stringResource(R.string.book_statistics_average_speed), String.format(Locale.getDefault(), "%.2f×", stats.averageSpeed))
            Figure(
                stringResource(R.string.book_statistics_finish),
                stats.estimatedFinish?.let(::mediumDate) ?: "—",
                stringResource(R.string.book_statistics_finish_unknown).takeIf { stats.estimatedFinish == null },
            )
            onOpenAll?.let { open ->
                AbbayTextButton(onClick = open) {
                    Text(stringResource(R.string.statistics_all), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
