package com.mobyle.abbay.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.statistics.PeriodTotal

@Composable
fun ListeningBarChart(bars: List<PeriodTotal>, labelOf: (PeriodTotal) -> String, modifier: Modifier = Modifier) {
    var selected by remember(bars) { mutableStateOf<Int?>(null) }
    val barColor = MaterialTheme.colorScheme.tertiary
    val baselineColor = AbbayColors.content.copy(alpha = BASELINE_ALPHA)
    val maximum = bars.maxOfOrNull { it.wallTimeMs }?.coerceAtLeast(1L) ?: 1L
    val values = bars.map { listeningTime(it.wallTimeMs) }
    val labels = bars.map(labelOf)
    val description = labels.zip(values).joinToString(", ") { (label, value) -> "$label $value" }
    val selectedText = selected?.let { stringResource(R.string.statistics_bar, labels[it], values[it]) }

    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Text(
            selectedText ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = AbbayColors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        if (bars.isEmpty()) return@detectTapGestures
                        val index = (offset.x / (size.width / bars.size)).toInt().coerceIn(0, bars.lastIndex)
                        selected = if (selected == index) null else index
                    }
                },
        ) {
            drawBars(bars, maximum, selected, barColor, baselineColor)
        }
        AxisLabels(labels)
    }
}

private fun DrawScope.drawBars(bars: List<PeriodTotal>, maximum: Long, selected: Int?, barColor: Color, baselineColor: Color) {
    if (bars.isEmpty()) return
    val slot = size.width / bars.size
    val gap = GAP.toPx().coerceAtMost(slot / 2)
    val radius = CORNER.toPx()
    bars.forEachIndexed { index, bar ->
        val height = size.height * bar.wallTimeMs / maximum
        if (height <= 0f) return@forEachIndexed
        val left = index * slot + gap / 2
        val width = slot - gap
        val top = size.height - height
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = left,
                    top = top,
                    right = left + width,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(radius.coerceAtMost(width / 2)),
                    topRightCornerRadius = CornerRadius(radius.coerceAtMost(width / 2)),
                ),
            )
        }
        drawPath(path, color = if (selected == null || selected == index) barColor else barColor.copy(alpha = DIMMED_ALPHA))
    }
    drawRect(baselineColor, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
    selected?.let { index ->
        drawRect(
            baselineColor,
            topLeft = Offset(index * slot, 0f),
            size = Size(slot, size.height),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    if (labels.isEmpty()) return
    val shown = if (labels.size <= SHOW_ALL_LABELS) labels else listOf(labels.first(), labels[labels.size / 2], labels.last())
    Row(modifier = Modifier.fillMaxWidth()) {
        shown.forEach { label ->
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = AbbayColors.content.copy(alpha = LABEL_ALPHA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val CHART_HEIGHT = 160.dp
private val GAP = 2.dp
private val CORNER = 4.dp
private const val SHOW_ALL_LABELS = 7
private const val BASELINE_ALPHA = 0.3f
private const val DIMMED_ALPHA = 0.45f
private const val LABEL_ALPHA = 0.75f
