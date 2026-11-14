package com.mobyle.abbay.presentation.walkthrough

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class WalkthroughPage(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val highlights: List<Highlight> = emptyList(),
)

private class Highlight(val icon: ImageVector, @StringRes val text: Int)

private val pages = listOf(
    WalkthroughPage(
        icon = Icons.Rounded.Headphones,
        title = R.string.walkthrough_welcome_title,
        body = R.string.walkthrough_welcome_body,
        highlights = listOf(
            Highlight(Icons.Rounded.LockOpen, R.string.walkthrough_welcome_no_account),
            Highlight(Icons.Rounded.WifiOff, R.string.walkthrough_welcome_offline),
        ),
    ),
    WalkthroughPage(
        icon = Icons.Rounded.FolderOpen,
        title = R.string.walkthrough_library_title,
        body = R.string.walkthrough_library_body,
        highlights = listOf(
            Highlight(Icons.Rounded.CreateNewFolder, R.string.walkthrough_library_folders),
            Highlight(Icons.Rounded.Public, R.string.walkthrough_library_catalogs),
        ),
    ),
    WalkthroughPage(
        icon = Icons.Rounded.Speed,
        title = R.string.walkthrough_listen_title,
        body = R.string.walkthrough_listen_body,
        highlights = listOf(
            Highlight(Icons.Rounded.Bedtime, R.string.walkthrough_listen_sleep),
            Highlight(Icons.Rounded.Bookmark, R.string.walkthrough_listen_bookmarks),
            Highlight(Icons.Rounded.Equalizer, R.string.walkthrough_listen_effects),
        ),
    ),
    WalkthroughPage(
        icon = Icons.Rounded.FormatQuote,
        title = R.string.walkthrough_more_title,
        body = R.string.walkthrough_more_body,
        highlights = listOf(
            Highlight(Icons.Rounded.FormatQuote, R.string.walkthrough_more_read_along),
            Highlight(Icons.Rounded.InsertChart, R.string.walkthrough_more_statistics),
            Highlight(Icons.Rounded.DirectionsCar, R.string.walkthrough_more_car),
        ),
    ),
    WalkthroughPage(
        icon = Icons.Rounded.CreateNewFolder,
        title = R.string.walkthrough_start_title,
        body = R.string.walkthrough_start_body,
    ),
)

/**
 * Shown over the whole app while the library says the tour is due. When it is due at launch it is already there in the
 * first frame, with no entrance; replayed from Settings it fades in, and either way it fades out onto the library.
 */
@Composable
fun WalkthroughOverlay(viewModel: BooksListViewModel) {
    val isShown by viewModel.showWalkthrough.collectAsState()
    val hasLibrary by viewModel.hasSelectedFolder.collectAsState()
    AnimatedVisibility(visible = isShown, enter = fadeIn(tween(OVERLAY_FADE_MS)), exit = fadeOut(tween(OVERLAY_FADE_MS))) {
        Walkthrough(hasLibrary = hasLibrary, onFolderSelected = viewModel::addFolder, onFinish = viewModel::dismissWalkthrough)
    }
}

/**
 * The tour a new listener sees once, over everything. The last page offers to pick the audiobook folder right away;
 * skipping, finishing or picking all mark it seen. It can be replayed from Settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Walkthrough(hasLibrary: Boolean, onFolderSelected: (treeUri: String) -> Unit, onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            onFolderSelected(it.toString())
            onFinish()
        }
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 16.dp),
    ) {
        SkipBar(isShown = !isLastPage, onSkip = onFinish)
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            PageContent(page = pages[index], isVisible = pagerState.currentPage == index)
        }
        PageIndicator(count = pages.size, current = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(24.dp))
        val action = when {
            isLastPage && !hasLibrary -> WalkthroughAction.CHOOSE_FOLDER
            isLastPage -> WalkthroughAction.FINISH
            else -> WalkthroughAction.NEXT
        }
        WalkthroughActions(
            action = action,
            onAction = {
                when (action) {
                    WalkthroughAction.CHOOSE_FOLDER -> folderPicker.launch(null)
                    WalkthroughAction.FINISH -> onFinish()
                    WalkthroughAction.NEXT -> scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            onLater = onFinish,
        )
    }
}

private enum class WalkthroughAction(@StringRes val label: Int) {
    NEXT(R.string.walkthrough_next),
    FINISH(R.string.walkthrough_get_started),
    CHOOSE_FOLDER(R.string.walkthrough_choose_folder),
}

@Composable
private fun SkipBar(isShown: Boolean, onSkip: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(visible = isShown, enter = fadeIn(), exit = fadeOut()) {
            AbbayTextButton(onClick = onSkip) { Text(stringResource(R.string.walkthrough_skip)) }
        }
    }
}

@Composable
private fun WalkthroughActions(action: WalkthroughAction, onAction: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().heightIn(min = 52.dp),
        ) {
            AnimatedContent(targetState = action.label, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "walkthroughAction") {
                Text(stringResource(it), style = MaterialTheme.typography.titleMedium)
            }
        }
        LaterButton(isShown = action == WalkthroughAction.CHOOSE_FOLDER, onClick = onLater)
    }
}

// Keeps its height when hidden, so the main button does not jump on the last page.
@Composable
private fun LaterButton(isShown: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = isShown, enter = fadeIn(), exit = fadeOut()) {
            AbbayTextButton(onClick = onClick) { Text(stringResource(R.string.walkthrough_later)) }
        }
    }
}

@Composable
private fun PageContent(page: WalkthroughPage, isVisible: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulsingIcon(page.icon)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            color = AbbayColors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = AbbayColors.content.copy(alpha = BODY_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        if (page.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                page.highlights.forEachIndexed { index, highlight ->
                    StaggeredHighlight(highlight, order = index, isVisible = isVisible)
                }
            }
        }
    }
}

/** Each highlight rises into place one after another whenever its page comes into view. */
@Composable
private fun StaggeredHighlight(highlight: Highlight, order: Int, isVisible: Boolean) {
    var isShown by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(HIGHLIGHT_DELAY_MS + order * HIGHLIGHT_STAGGER_MS)
            isShown = true
        } else {
            isShown = false
        }
    }
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        AnimatedVisibility(
            visible = isShown,
            enter = fadeIn(tween(HIGHLIGHT_ENTER_MS)) + slideInVertically(tween(HIGHLIGHT_ENTER_MS)) { height -> height / 2 },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AbbayColors.content.copy(alpha = HIGHLIGHT_BACKGROUND_ALPHA))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(highlight.icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(stringResource(highlight.text), style = MaterialTheme.typography.bodyMedium, color = AbbayColors.content)
            }
        }
    }
}

@Composable
private fun PulsingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "walkthroughPulse")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MS, easing = LinearEasing)),
        label = "ripple",
    )
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = ICON_BREATH_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val accent = MaterialTheme.colorScheme.tertiary
    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        listOf(0f, 0.5f).forEach { offset ->
            val progress = (ripple + offset) % 1f
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        val scale = 1f + progress * RIPPLE_GROWTH
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - progress) * RIPPLE_ALPHA
                    }
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                }
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val isCurrent = index == current
            val width by animateDpAsState(if (isCurrent) 24.dp else 8.dp, label = "indicatorWidth")
            val color by animateColorAsState(
                if (isCurrent) MaterialTheme.colorScheme.tertiary else AbbayColors.content.copy(alpha = INDICATOR_IDLE_ALPHA),
                label = "indicatorColor",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private const val BODY_ALPHA = 0.75f
private const val HIGHLIGHT_BACKGROUND_ALPHA = 0.06f
private const val INDICATOR_IDLE_ALPHA = 0.25f
private const val RIPPLE_ALPHA = 0.35f
private const val RIPPLE_GROWTH = 0.6f
private const val ICON_BREATH_SCALE = 1.06f
private const val PULSE_MS = 2400
private const val HIGHLIGHT_DELAY_MS = 150L
private const val HIGHLIGHT_STAGGER_MS = 120L
private const val HIGHLIGHT_ENTER_MS = 350
private const val OVERLAY_FADE_MS = 300
