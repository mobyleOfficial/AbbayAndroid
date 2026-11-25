package com.mobyle.abbay.presentation.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mobyle.abbay.R
import com.player.NowPlaying
import com.settings.ThemeMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The three widgets share one responsive layout and differ only in their default size, so a resized widget always
 * shows what fits: a compact cover and play button, a single row with the book and controls, or the full player.
 */
abstract class PlayerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    // A Glance session keeps its composition alive between updates, so everything shown is observed inside it; reading
    // it once here would leave a running session showing stale state (play instead of pause, an old chapter).
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = NowPlayingStore(context)
        val themeModes = themeModes(context)
        val initialNowPlaying = store.read()
        val initialCover = loadCover(context, initialNowPlaying?.coverUri)
        val initialThemeMode = themeModes.first()
        provideContent {
            val nowPlaying by remember { store.changes() }.collectAsState(initialNowPlaying)
            val themeMode by remember { themeModes }.collectAsState(initialThemeMode)
            val coverUri = nowPlaying?.coverUri
            val cover by produceState(initialCover.takeIf { coverUri == initialNowPlaying?.coverUri }, coverUri) {
                value = loadCover(context, coverUri)
            }
            val palette = WidgetPalette.of(isDark(context, themeMode), cover?.average)
            PlayerWidgetContent(nowPlaying, cover?.bitmap, palette)
        }
    }

    private fun themeModes(context: Context): Flow<ThemeMode> = runCatching {
        EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            .settingsRepository().observeSettings().map { it.themeMode }.distinctUntilChanged()
    }.getOrDefault(flowOf(ThemeMode.DARK))

    /** Follows the theme chosen in the app (dark by default), like the in-app player. */
    private fun isDark(context: Context, themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.SYSTEM ->
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
}

class MiniPlayerWidget : PlayerWidget()

class StandardPlayerWidget : PlayerWidget()

class LargePlayerWidget : PlayerWidget()

@Composable
internal fun PlayerWidgetContent(nowPlaying: NowPlaying?, cover: Bitmap?, palette: WidgetPalette) {
    val size = LocalSize.current
    WidgetSurface(palette) {
        when {
            nowPlaying == null -> EmptyState(palette, size)
            size.height >= TALL_MIN_HEIGHT -> TallLayout(nowPlaying, cover, palette, size)
            size.width >= ROW_MIN_WIDTH -> RowLayout(nowPlaying, cover, palette, size)
            else -> CompactLayout(nowPlaying, cover, palette, size)
        }
    }
}

@Composable
internal fun WidgetSurface(palette: WidgetPalette, content: @Composable () -> Unit) {
    Box(modifier = GlanceModifier.fillMaxSize().appWidgetBackground()) {
        Image(
            provider = ImageProvider(R.drawable.widget_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(ColorProvider(palette.background)),
            modifier = GlanceModifier.fillMaxSize(),
        )
        if (palette.shaded) {
            Image(
                provider = ImageProvider(R.drawable.widget_background_shade),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.fillMaxSize().padding(PADDING)) { content() }
    }
}

/** 2×1: cover, play button and, when there is room below, the chapter progress. */
@Composable
internal fun CompactLayout(playing: NowPlaying, cover: Bitmap?, palette: WidgetPalette, size: DpSize) {
    val showProgress = size.height >= COMPACT_PROGRESS_MIN_HEIGHT
    val coverSize = (size.height - PADDING * 2 - if (showProgress) PROGRESS_BLOCK else 0.dp).coerceIn(36.dp, 64.dp)
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Cover(cover, coverSize, modifier = GlanceModifier.clickable(openAppAction()))
            Spacer(GlanceModifier.defaultWeight())
            PlayPauseButton(playing, palette, PLAY_SIZE)
        }
        if (showProgress) {
            Spacer(GlanceModifier.height(PROGRESS_GAP))
            ProgressBar(playing.chapterProgress, palette, size.width - PADDING * 2)
        }
    }
}

/** 4×1: cover, book and chapter, skip and play controls, with the chapter progress underneath. */
@Composable
internal fun RowLayout(playing: NowPlaying, cover: Bitmap?, palette: WidgetPalette, size: DpSize) {
    val showProgress = size.height >= ROW_PROGRESS_MIN_HEIGHT
    val showSkips = size.width >= ROW_SKIPS_MIN_WIDTH
    val coverSize = (size.height - PADDING * 2 - if (showProgress) PROGRESS_BLOCK else 0.dp).coerceIn(36.dp, 56.dp)
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.defaultWeight().clickable(openAppAction()),
            ) {
                Cover(cover, coverSize)
                Spacer(GlanceModifier.width(10.dp))
                BookTitles(playing, palette, titleSize = 14.sp, titleLines = 2, modifier = GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.width(4.dp))
            if (showSkips) SkipBackButton(palette, ROW_SKIP_SIZE)
            PlayPauseButton(playing, palette, ROW_PLAY_SIZE)
            if (showSkips) SkipForwardButton(palette, ROW_SKIP_SIZE)
        }
        if (showProgress) {
            Spacer(GlanceModifier.height(PROGRESS_GAP))
            ProgressBar(playing.chapterProgress, palette, size.width - PADDING * 2)
        }
    }
}

/** 4×2 and up: large cover and titles, chapter progress, and a full row of controls. */
@Composable
internal fun TallLayout(playing: NowPlaying, cover: Bitmap?, palette: WidgetPalette, size: DpSize) {
    // The cover takes the height left over by the progress and controls, so the book sits right above its progress.
    val spareHeight = size.height - PADDING * 2 - TALL_INFO_GAP - PROGRESS_HEIGHT - TALL_CONTROLS_GAP - PLAY_SIZE_LARGE
    val coverSize = spareHeight.coerceIn(56.dp, size.width * TALL_COVER_MAX_WIDTH_RATIO)
    val showExtras = size.width >= TALL_EXTRAS_MIN_WIDTH
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().clickable(openAppAction()),
        ) {
            Cover(cover, coverSize)
            Spacer(GlanceModifier.width(14.dp))
            BookTitles(playing, palette, titleSize = 17.sp, titleLines = 2, modifier = GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(TALL_INFO_GAP))
        ProgressBar(playing.chapterProgress, palette, size.width - PADDING * 2)
        Spacer(GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.height(TALL_CONTROLS_GAP))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            if (showExtras) Slot { SleepTimerButton(playing, palette) }
            Slot { SkipBackButton(palette) }
            Slot { PlayPauseButton(playing, palette, PLAY_SIZE_LARGE) }
            Slot { SkipForwardButton(palette) }
            if (showExtras) Slot { BookmarkButton(palette) }
        }
    }
}

@Composable
internal fun EmptyState(palette: WidgetPalette, size: DpSize) {
    val context = LocalContext.current
    val wide = size.width >= ROW_MIN_WIDTH
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.fillMaxSize().clickable(openAppAction())) {
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Cover(cover = null, size = (size.height - PADDING * 2).coerceIn(36.dp, 56.dp))
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(context.getString(R.string.app_name), maxLines = 1, style = titleStyle(palette, 15.sp))
                    Text(context.getString(R.string.widget_empty), maxLines = 2, style = subtitleStyle(palette))
                }
            }
        } else {
            Text(
                context.getString(R.string.widget_empty),
                maxLines = 3,
                style = subtitleStyle(palette).copy(color = ColorProvider(palette.content), textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
internal fun BookTitles(playing: NowPlaying, palette: WidgetPalette, titleSize: TextUnit, titleLines: Int, modifier: GlanceModifier) {
    Column(modifier = modifier) {
        Text(playing.bookTitle, maxLines = titleLines, style = titleStyle(palette, titleSize))
        if (playing.chapterTitle.isNotBlank()) {
            Text(playing.chapterTitle, maxLines = 1, style = subtitleStyle(palette))
        }
    }
}

internal fun titleStyle(palette: WidgetPalette, fontSize: TextUnit) =
    TextStyle(color = ColorProvider(palette.content), fontSize = fontSize, fontWeight = FontWeight.Bold)

internal fun subtitleStyle(palette: WidgetPalette) = TextStyle(color = ColorProvider(palette.contentVariant), fontSize = 13.sp)

@Composable
internal fun openAppAction(): Action {
    val context = LocalContext.current
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: android.content.Intent().setPackage(context.packageName)
    return actionStartActivity(intent)
}

class MiniPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniPlayerWidget()
}

class StandardPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StandardPlayerWidget()
}

class LargePlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargePlayerWidget()
}
