package com.mobyle.abbay.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.SimpleBookRow
import com.statistics.GlobalStatistics
import com.statistics.PeriodTotal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private enum class Period { WEEK, MONTH, YEAR }

/** Global statistics; a top book opens its own statistics. */
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel(), onOpenBook: (String) -> Unit, close: () -> Unit) {
    val statistics by viewModel.statistics.collectAsState()
    val books by viewModel.books.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    AbbayScreen(title = stringResource(R.string.statistics_title), onClose = close) {
        val stats = statistics ?: return@AbbayScreen
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        ) {
            if (!isRecording) Note(stringResource(R.string.statistics_disabled))
            if (stats.totalWallTimeMs <= 0L) {
                EmptyMessage(stringResource(R.string.statistics_empty))
                return@Column
            }
            Summary(stats)
            PeriodChart(stats)
            if (stats.topBooks.isNotEmpty()) {
                Column {
                    SectionTitle(stringResource(R.string.statistics_top_books))
                    stats.topBooks.forEach { total ->
                        val book = books[total.bookId] ?: return@forEach
                        SimpleBookRow(book, onClick = { onOpenBook(book.id) }) {
                            Text(listeningTime(total.wallTimeMs), color = AbbayColors.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(stats: GlobalStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
        Figure(
            label = stringResource(R.string.statistics_total),
            value = listeningTime(stats.totalWallTimeMs),
            detail = stringResource(R.string.statistics_book_time, listeningTime(stats.totalBookTimeMs)),
        )
        Figure(
            label = stringResource(R.string.statistics_streak),
            value = stats.streakDays.toString(),
            detail = pluralStringResource(R.plurals.statistics_streak_days, stats.streakDays, stats.streakDays),
        )
        Figure(
            label = stringResource(R.string.statistics_saved),
            value = listeningTime(stats.savedBySpeedMs + stats.savedBySilenceMs),
            detail = stringResource(R.string.statistics_saved_speed, listeningTime(stats.savedBySpeedMs)) + " · " +
                stringResource(R.string.statistics_saved_silence, listeningTime(stats.savedBySilenceMs)),
        )
    }
}

@Composable
private fun PeriodChart(stats: GlobalStatistics) {
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    val locale = Locale.getDefault()
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Period.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { period = option },
                    label = { Text(stringResource(option.label())) },
                    colors = FilterChipDefaults.filterChipColors(labelColor = AbbayColors.content),
                )
            }
        }
        val (bars, labelOf) = when (period) {
            Period.WEEK -> stats.last7Days to { bar: PeriodTotal -> bar.start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) }
            Period.MONTH -> stats.last30Days to { bar: PeriodTotal -> bar.start.format(DateTimeFormatter.ofPattern("d/M", locale)) }
            Period.YEAR -> stats.last12Months to { bar: PeriodTotal -> bar.start.month.getDisplayName(TextStyle.SHORT, locale) }
        }
        ListeningBarChart(bars, labelOf, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

private fun Period.label(): Int = when (this) {
    Period.WEEK -> R.string.statistics_period_week
    Period.MONTH -> R.string.statistics_period_month
    Period.YEAR -> R.string.statistics_period_year
}

@Composable
fun Figure(label: String, value: String, detail: String? = null) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AbbayColors.content)
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = AbbayColors.content,
        modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
    )
}

@Composable
fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AbbayColors.content,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

/** Medium date in the user's locale, for the estimated finish. */
fun mediumDate(date: java.time.LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private const val SECONDARY_ALPHA = 0.75f
