package com.mobyle.abbay.presentation.common.theme

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor

object CoverTint {
    private const val SAMPLE_SIDE = 16
    private const val MINIMUM_CONTRAST = 4.5f
    private const val DARKENING_STEP = 0.9f
    private const val MAX_DARKENING_STEPS = 40
    private const val LIGHT_WASH = 0.18f
    private const val LIGHT_WASH_STEP = 0.03f
    private const val CACHE_SIZE = 128
    private const val HSV_COMPONENTS = 3

    private val averages = LruCache<String, Int>(CACHE_SIZE)

    fun cachedAverage(thumbnail: String?): Color? = thumbnail?.let { averages.get(it) }?.let { Color(it) }

    suspend fun average(context: Context, thumbnail: String): Color? = cachedAverage(thumbnail) ?: run {
        val request = ImageRequest.Builder(context)
            .data(thumbnail)
            .size(SAMPLE_SIDE)
            .allowHardware(false)
            .build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
        val average = drawable?.let {
            withContext(Dispatchers.Default) { averageOf(it.toBitmap(SAMPLE_SIDE, SAMPLE_SIDE, Bitmap.Config.ARGB_8888)) }
        }
        average?.also { averages.put(thumbnail, it.toArgb()) }
    }

    private fun averageOf(bitmap: Bitmap): Color? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var red = 0L
        var green = 0L
        var blue = 0L
        var alpha = 0L
        pixels.forEach { pixel ->
            val pixelAlpha = AndroidColor.alpha(pixel)
            red += AndroidColor.red(pixel) * pixelAlpha
            green += AndroidColor.green(pixel) * pixelAlpha
            blue += AndroidColor.blue(pixel) * pixelAlpha
            alpha += pixelAlpha
        }
        if (alpha == 0L) return null
        return Color(red = (red / alpha).toInt(), green = (green / alpha).toInt(), blue = (blue / alpha).toInt())
    }

    fun darkenedForWhiteText(color: Color): Color {
        val hsv = FloatArray(HSV_COMPONENTS)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        var candidate = color
        var steps = 0
        while (contrast(Color.White, candidate) < MINIMUM_CONTRAST && steps < MAX_DARKENING_STEPS) {
            hsv[2] *= DARKENING_STEP
            candidate = Color(AndroidColor.HSVToColor(hsv))
            steps++
        }
        return candidate
    }

    fun washedOver(color: Color, surface: Color, content: Color): Color {
        var fraction = LIGHT_WASH
        var candidate = lerp(surface, color, fraction)
        while (contrast(content, candidate) < MINIMUM_CONTRAST && fraction > 0f) {
            fraction = (fraction - LIGHT_WASH_STEP).coerceAtLeast(0f)
            candidate = lerp(surface, color, fraction)
        }
        return candidate
    }

    fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private const val LUMINANCE_OFFSET = 0.05f
}

val isDarkTheme: Boolean
    @Composable get() = MaterialTheme.colorScheme.surface.luminance() < HALF_LUMINANCE

private const val HALF_LUMINANCE = 0.5f

@Composable
fun rememberCoverTint(thumbnail: String?): Color {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isDarkTheme
    var average by remember(thumbnail) { mutableStateOf(CoverTint.cachedAverage(thumbnail)) }
    LaunchedEffect(thumbnail) {
        if (average == null && thumbnail != null) average = CoverTint.average(context, thumbnail)
    }
    val target = average?.let { cover ->
        if (isDark) CoverTint.darkenedForWhiteText(cover) else CoverTint.washedOver(cover, colorScheme.surface, colorScheme.onSurface)
    } ?: colorScheme.surface
    val tint by animateColorAsState(target, label = "coverTint")
    return tint
}

@Composable
fun Modifier.coverTintBackground(tint: Color, gradientStrength: Float = 1f): Modifier {
    val shade = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.surface
    val strength = gradientStrength.coerceIn(0f, 1f)
    return drawBehind {
        drawRect(tint)
        if (strength > 0f) {
            drawRect(
                Brush.verticalGradient(
                    0f to shade.copy(alpha = 0f),
                    GRADIENT_MIDDLE to shade.copy(alpha = GRADIENT_MIDDLE_ALPHA * strength),
                    1f to shade.copy(alpha = GRADIENT_END_ALPHA * strength),
                ),
            )
        }
    }
}

private const val GRADIENT_MIDDLE = 0.45f
private const val GRADIENT_MIDDLE_ALPHA = 0.45f
private const val GRADIENT_END_ALPHA = 0.82f
