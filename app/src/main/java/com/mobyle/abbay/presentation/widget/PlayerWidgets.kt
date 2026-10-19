package com.mobyle.abbay.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mobyle.abbay.R
import com.player.NowPlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The three widgets. They draw the last published [NowPlaying] (stored on
 * disk), so they keep working while the app is not running; buttons send media keys to the playback service.
 */
class MiniPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nowPlaying = NowPlayingStore(context).read()
        val cover = loadCover(context, nowPlaying?.coverUri)
        provideContent {
            WidgetFrame(nowPlaying) { playing ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
                    Cover(cover, size = COVER_SMALL)
                    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight()) { PlayPauseButton(playing) }
                }
            }
        }
    }
}

class StandardPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nowPlaying = NowPlayingStore(context).read()
        val cover = loadCover(context, nowPlaying?.coverUri)
        provideContent { WidgetFrame(nowPlaying) { playing -> StandardRow(playing, cover) } }
    }
}

class LargePlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nowPlaying = NowPlayingStore(context).read()
        val cover = loadCover(context, nowPlaying?.coverUri)
        provideContent {
            WidgetFrame(nowPlaying) { playing ->
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    StandardRow(playing, cover)
                    LinearProgressIndicator(
                        progress = playing.chapterProgress,
                        color = ColorProvider(ACCENT),
                        backgroundColor = ColorProvider(TRACK),
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight()) {
                            IconButton(
                                icon = R.drawable.widget_sleep,
                                description = LocalContext.current.getString(
                                    if (playing.isSleepTimerRunning) R.string.widget_sleep_timer_off else R.string.widget_sleep_timer_on,
                                ),
                                action = actionStartService(
                                    WidgetCommands.serviceAction(LocalContext.current, WidgetCommands.ACTION_TOGGLE_SLEEP_TIMER)
                                ),
                                tint = if (playing.isSleepTimerRunning) ACCENT else Color.White,
                            )
                        }
                        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.defaultWeight()) {
                            IconButton(
                                icon = R.drawable.widget_bookmark,
                                description = LocalContext.current.getString(R.string.widget_add_bookmark),
                                action = actionStartService(
                                    WidgetCommands.serviceAction(LocalContext.current, WidgetCommands.ACTION_ADD_BOOKMARK)
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardRow(playing: NowPlaying, cover: Bitmap?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Cover(cover, size = COVER_SMALL)
        Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 8.dp)) {
            Text(
                playing.bookTitle,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            if (playing.chapterTitle.isNotBlank()) {
                Text(playing.chapterTitle, maxLines = 1, style = TextStyle(color = ColorProvider(SECONDARY_TEXT), fontSize = 12.sp))
            }
        }
        val context = LocalContext.current
        IconButton(R.drawable.widget_back, context.getString(R.string.widget_skip_back), mediaKey(context, KeyEvent.KEYCODE_MEDIA_REWIND))
        PlayPauseButton(playing)
        IconButton(
            R.drawable.widget_forward,
            context.getString(R.string.widget_skip_forward),
            mediaKey(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        )
    }
}

/** Background, padding, and the empty state that opens the app when nothing was played yet. */
@Composable
private fun WidgetFrame(nowPlaying: NowPlaying?, content: @Composable (NowPlaying) -> Unit) {
    val context = LocalContext.current
    val modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_background)).cornerRadius(16.dp).padding(8.dp)
    if (nowPlaying == null) {
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.let { base ->
                open?.let { base.clickable(actionStartActivity(it)) }
                    ?: base
            }
        ) {
            Text(context.getString(R.string.widget_empty), style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp))
        }
    } else {
        Box(modifier = modifier) { content(nowPlaying) }
    }
}

@Composable
private fun PlayPauseButton(playing: NowPlaying) {
    val context = LocalContext.current
    IconButton(
        icon = if (playing.isPlaying) R.drawable.widget_pause else R.drawable.widget_play,
        description = context.getString(if (playing.isPlaying) R.string.widget_pause else R.string.widget_play),
        action = mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
    )
}

/** 48 dp touch target with the icon centered. */
@Composable
private fun IconButton(icon: Int, description: String, action: Action, tint: Color = Color.White) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp).clickable(action).semantics { contentDescription = description },
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(tint)),
            modifier = GlanceModifier.size(28.dp),
        )
    }
}

@Composable
private fun Cover(cover: Bitmap?, size: androidx.compose.ui.unit.Dp) {
    Image(
        provider = cover?.let(::ImageProvider) ?: ImageProvider(R.drawable.widget_cover_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = GlanceModifier.size(size).cornerRadius(8.dp),
    )
}

/** Foreground-service start: allowed from a widget click, and it works with the app closed. */
private fun mediaKey(context: Context, keyCode: Int): Action =
    actionStartService(WidgetCommands.mediaKey(context, keyCode), isForegroundService = true)

/** The cover, downsampled to at most [COVER_PX] pixels so the widget's bitmap stays small. */
private suspend fun loadCover(context: Context, coverUri: String?): Bitmap? = withContext(Dispatchers.IO) {
    coverUri ?: return@withContext null
    runCatching {
        val uri = Uri.parse(coverUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = generateSequence(1) { it * 2 }.first { maxOf(bounds.outWidth, bounds.outHeight) / it <= COVER_PX }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize =
                        sample
                }
            )
        }
    }.getOrNull()
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

private val COVER_SMALL = 56.dp
private const val COVER_PX = 256
private val ACCENT = Color(0xFF1ED760)
private val TRACK = Color(0xFF45454C)
private val SECONDARY_TEXT = Color(0xFFBDBDC2)
