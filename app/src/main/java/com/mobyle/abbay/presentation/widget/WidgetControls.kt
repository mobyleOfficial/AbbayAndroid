package com.mobyle.abbay.presentation.widget

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.mobyle.abbay.R
import com.player.NowPlaying
@Composable
internal fun ProgressBar(progress: Float, palette: WidgetPalette, width: Dp) {
    Box(modifier = GlanceModifier.fillMaxWidth().height(PROGRESS_HEIGHT)) {
        Pill(palette.track, GlanceModifier.fillMaxSize())
        val filled = width * progress.coerceIn(0f, 1f)
        if (filled > 0.dp) Pill(palette.accent, GlanceModifier.width(maxOf(filled, PROGRESS_HEIGHT)).fillMaxHeight())
    }
}

@Composable
internal fun Pill(color: Color, modifier: GlanceModifier) {
    Image(
        provider = ImageProvider(R.drawable.widget_pill),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = modifier,
    )
}

@Composable
internal fun RowScope.Slot(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight()) { content() }
}

@Composable
internal fun PlayPauseButton(playing: NowPlaying, palette: WidgetPalette, size: Dp) {
    val context = LocalContext.current
    IconButton(
        icon = if (playing.isPlaying) R.drawable.widget_pause else R.drawable.widget_play,
        description = context.getString(if (playing.isPlaying) R.string.widget_pause else R.string.widget_play),
        action = mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
        tint = palette.onAccent,
        background = palette.accent,
        size = size,
        iconSize = size * PLAY_ICON_RATIO,
    )
}

@Composable
internal fun SkipBackButton(palette: WidgetPalette, size: Dp = BUTTON_SIZE) {
    val context = LocalContext.current
    IconButton(
        icon = R.drawable.widget_back,
        description = context.getString(R.string.widget_skip_back),
        action = mediaKey(context, KeyEvent.KEYCODE_MEDIA_REWIND),
        tint = palette.content,
        size = size,
        iconSize = minOf(ICON_SIZE, size * SKIP_ICON_RATIO),
    )
}

@Composable
internal fun SkipForwardButton(palette: WidgetPalette, size: Dp = BUTTON_SIZE) {
    val context = LocalContext.current
    IconButton(
        icon = R.drawable.widget_forward,
        description = context.getString(R.string.widget_skip_forward),
        action = mediaKey(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD),
        tint = palette.content,
        size = size,
        iconSize = minOf(ICON_SIZE, size * SKIP_ICON_RATIO),
    )
}

@Composable
internal fun SleepTimerButton(playing: NowPlaying, palette: WidgetPalette) {
    val context = LocalContext.current
    val running = playing.isSleepTimerRunning
    IconButton(
        icon = R.drawable.widget_sleep,
        description = context.getString(if (running) R.string.widget_sleep_timer_off else R.string.widget_sleep_timer_on),
        action = actionStartService(WidgetCommands.serviceAction(context, WidgetCommands.ACTION_TOGGLE_SLEEP_TIMER)),
        tint = if (running) palette.accent else palette.contentVariant,
        background = if (running) palette.track else null,
        iconSize = SECONDARY_ICON_SIZE,
    )
}

@Composable
internal fun BookmarkButton(palette: WidgetPalette) {
    val context = LocalContext.current
    IconButton(
        icon = R.drawable.widget_bookmark,
        description = context.getString(R.string.widget_add_bookmark),
        action = actionStartService(WidgetCommands.serviceAction(context, WidgetCommands.ACTION_ADD_BOOKMARK)),
        tint = palette.contentVariant,
        iconSize = SECONDARY_ICON_SIZE,
    )
}

@Composable
internal fun IconButton(
    icon: Int,
    description: String,
    action: Action,
    tint: Color,
    background: Color? = null,
    size: Dp = BUTTON_SIZE,
    iconSize: Dp = ICON_SIZE,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(size).clickable(action).semantics { contentDescription = description },
    ) {
        if (background != null) {
            Image(
                provider = ImageProvider(R.drawable.widget_circle),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(ColorProvider(background)),
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(tint)),
            modifier = GlanceModifier.size(iconSize),
        )
    }
}

internal fun mediaKey(context: Context, keyCode: Int): Action =
    actionStartService(WidgetCommands.mediaKey(context, keyCode), isForegroundService = true)
