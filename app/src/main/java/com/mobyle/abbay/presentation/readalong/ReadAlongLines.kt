package com.mobyle.abbay.presentation.readalong

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.readalong.ReadAlongLine

/**
 * The transcript, the current line kept in the middle. The current line is white and bold, with the spoken word in
 * the accent colour; lines already read are faint, upcoming ones a little less. A tap on a line plays from it.
 */
@Composable
internal fun ReadAlongLines(
    lines: List<ReadAlongLine>,
    current: Int,
    word: Int?,
    isSpoken: Boolean,
    style: ReadAlongStyle,
    onSeek: (Long) -> Unit,
    onInteract: () -> Unit,
) {
    val listState = rememberLazyListState()
    val seekLabel = stringResource(R.string.read_along_go_to_line)
    FollowPlayback(listState, current)

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = VERTICAL_PADDING.dp),
        modifier = Modifier
            .fillMaxSize()
            .fadeEdges()
            .pointerInput(Unit) { detectTapGestures { onInteract() } },
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = isSpoken && index == current
            Text(
                text = highlighted(line, if (isCurrent) word else null),
                style = lineStyle(style, if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                color = Color.White.copy(alpha = alphaOf(index, current, isCurrent)),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TOUCH_TARGET.dp)
                    .clickable(role = Role.Button, onClickLabel = seekLabel) {
                        onInteract()
                        onSeek(line.startMs)
                    }
                    .padding(horizontal = style.horizontalPadding.dp, vertical = style.spacing.dp / 2),
            )
        }
    }
}

private fun alphaOf(index: Int, current: Int, isCurrent: Boolean): Float = when {
    isCurrent -> 1f
    index < current -> READ_ALPHA
    else -> UPCOMING_ALPHA
}

@Composable
private fun lineStyle(style: ReadAlongStyle, weight: FontWeight): TextStyle = when (style) {
    ReadAlongStyle.SQUARE -> MaterialTheme.typography.titleMedium.copy(fontWeight = weight)
    ReadAlongStyle.FULL_SCREEN -> MaterialTheme.typography.titleLarge.copy(fontWeight = weight)
}

/** The line's words, the spoken one in the accent colour. */
@Composable
private fun highlighted(line: ReadAlongLine, word: Int?): AnnotatedString {
    val accent = MaterialTheme.colorScheme.tertiary
    return remember(line, word, accent) {
        buildAnnotatedString {
            line.words.forEachIndexed { index, item ->
                if (index > 0) append(' ')
                if (index == word) {
                    withStyle(SpanStyle(color = accent)) { append(item.text) }
                } else {
                    append(item.text)
                }
            }
        }
    }
}

@Composable
private fun FollowPlayback(listState: LazyListState, current: Int) {
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(current, scrollOffset = -CENTER_OFFSET_PX)
    }
}

/** Fades the top and bottom edges, so the text doesn't stop at a hard line. */
private fun Modifier.fadeEdges(): Modifier = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            FADE_FRACTION to Color.Black,
            1f - FADE_FRACTION to Color.Black,
            1f to Color.Transparent,
        ),
        blendMode = BlendMode.DstIn,
    )
}
