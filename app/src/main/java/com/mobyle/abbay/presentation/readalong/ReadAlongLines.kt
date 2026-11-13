package com.mobyle.abbay.presentation.readalong

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.TertiaryColor
import com.readalong.ReadAlongLine

/**
 * The transcript with the current line kept in the middle: white and semibold, the spoken word in green; lines read
 * already are faint, upcoming ones a little less. The top and bottom edges fade out. Lazy, so a whole book costs only
 * the rows on screen. A tap on a line plays from its start; touch and hold says it is the one being read (the sync
 * adjustment lines the text up with it).
 */
@Suppress("LongParameterList")
@Composable
internal fun Lines(
    lines: List<ReadAlongLine>,
    current: Int,
    isSpoken: Boolean,
    word: Int?,
    style: ReadAlongStyle,
    onLineTap: (ReadAlongLine) -> Unit,
    onLineHold: (ReadAlongLine) -> Unit,
    onScroll: () -> Unit,
    animates: Boolean,
) {
    val listState = rememberLazyListState()
    val dragInteraction = remember { MutableInteractionSource() }
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) { if (isDragged) onScroll() }
    var hasCentered by remember { mutableStateOf(false) }
    LaunchedEffect(current) {
        listState.center(current, animate = hasCentered && animates)
        hasCentered = true
    }
    val hints = LineHints(stringResource(R.string.read_along_line_hint), stringResource(R.string.read_along_line_sync_hint))
    val textStyle = if (style == ReadAlongStyle.SQUARE) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = maxHeight / 2),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(KeepsScrollInside)
                .then(style.maxLineWidth?.let { Modifier.widthIn(max = it) } ?: Modifier)
                .edgeFade(),
            userScrollEnabled = true,
        ) {
            items(count = lines.size, key = { index -> index }) { index ->
                val isCurrent = isSpoken && index == current
                val alpha = when {
                    isCurrent -> 1f
                    index < current -> READ_ALPHA
                    else -> UPCOMING_ALPHA
                }
                val line = lines[index]
                LineText(
                    line, isCurrent, if (isCurrent) word else null, alpha, textStyle, style, hints, dragInteraction,
                    onTap = { onLineTap(line) },
                    onHold = { onLineHold(line) },
                )
            }
        }
    }
}

/**
 * What the lines don't scroll stays with them: dragged past their first or last line, the drag would otherwise reach
 * the player's sheet and collapse it.
 */
private object KeepsScrollInside : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongParameterList")
@Composable
private fun LineText(
    line: ReadAlongLine,
    isCurrent: Boolean,
    word: Int?,
    alpha: Float,
    textStyle: TextStyle,
    style: ReadAlongStyle,
    hints: LineHints,
    interactionSource: MutableInteractionSource,
    onTap: () -> Unit,
    onHold: () -> Unit,
) {
    Text(
        text = buildAnnotatedString {
            line.words.forEachIndexed { wordIndex, item ->
                if (wordIndex > 0) append(' ')
                if (wordIndex == word) withStyle(SpanStyle(color = TertiaryColor)) { append(item.text) } else append(item.text)
            }
        },
        style = textStyle,
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        color = Color.White.copy(alpha = alpha),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = hints.tap,
                onLongClickLabel = hints.hold,
                onLongClick = onHold,
                onClick = onTap,
            )
            .semantics { selected = isCurrent }
            // The gap between lines is part of each line's touch target, so every line is at least 48 dp tall.
            .padding(horizontal = style.lineMargin, vertical = style.lineSpacing / 2),
    )
}

/** What TalkBack says a line's tap and its touch and hold do. */
private class LineHints(val tap: String, val hold: String)

/** Brings line [index] to the middle: smoothly when it is on screen, else at once. */
private suspend fun LazyListState.center(index: Int, animate: Boolean) {
    fun delta(): Float? {
        val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        return (info.offset + info.size / 2 - viewportCenter).toFloat()
    }
    val visible = delta()
    if (visible != null && animate) {
        animateScrollBy(visible, tween(SCROLL_DURATION_MS))
        return
    }
    if (visible == null) scrollToItem(index)
    delta()?.let { scrollBy(it) }
}

private fun Modifier.edgeFade(): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            FADE_TOP to Color.Black,
            FADE_BOTTOM to Color.Black,
            // Gone by the bar along the bottom, which sits over the last lines.
            FADE_GONE to Color.Transparent,
            1f to Color.Transparent,
        ),
        blendMode = BlendMode.DstIn,
    )
}

private const val READ_ALPHA = 0.4f
private const val UPCOMING_ALPHA = 0.6f
private const val FADE_TOP = 0.2f
private const val FADE_BOTTOM = 0.68f
private const val FADE_GONE = 0.88f
private const val SCROLL_DURATION_MS = 350
