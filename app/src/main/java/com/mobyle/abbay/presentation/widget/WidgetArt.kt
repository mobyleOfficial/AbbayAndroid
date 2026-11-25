package com.mobyle.abbay.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.CoverTint
import com.mobyle.abbay.presentation.common.theme.LightOnColor
import com.mobyle.abbay.presentation.common.theme.LightOnSurfaceVariant
import com.mobyle.abbay.presentation.common.theme.LightSurfaceColor
import com.mobyle.abbay.presentation.common.theme.LightTertiaryColor
import com.mobyle.abbay.presentation.common.theme.PrimaryColor
import com.mobyle.abbay.presentation.common.theme.TertiaryColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
/** The cover arrives already cropped to a square with rounded corners, since cornerRadius needs Android 12. */
@Composable
internal fun Cover(cover: Bitmap?, size: Dp, modifier: GlanceModifier = GlanceModifier) {
    Image(
        provider = cover?.let(::ImageProvider) ?: ImageProvider(R.drawable.widget_cover_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

/** Colours follow the in-app player: the cover's average colour, darkened for white text or washed over the light surface. */
internal data class WidgetPalette(
    val background: Color,
    val content: Color,
    val contentVariant: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val shaded: Boolean,
) {
    companion object {
        fun of(dark: Boolean, coverAverage: Color?): WidgetPalette =
            if (dark) {
                val background = coverAverage?.let(CoverTint::darkenedForWhiteText) ?: PrimaryColor
                WidgetPalette(
                    background = background,
                    content = Color.White,
                    contentVariant = Color.White.copy(alpha = DARK_SECONDARY_TEXT_ALPHA),
                    accent = TertiaryColor,
                    onAccent = Color.Black,
                    track = lerp(background, Color.White, DARK_TRACK_FRACTION),
                    shaded = coverAverage != null,
                )
            } else {
                val background = coverAverage?.let { CoverTint.washedOver(it, LightSurfaceColor, LightOnColor) } ?: LightSurfaceColor
                WidgetPalette(
                    background = background,
                    content = LightOnColor,
                    contentVariant = LightOnSurfaceVariant,
                    accent = LightTertiaryColor,
                    onAccent = Color.White,
                    track = lerp(background, LightOnColor, LIGHT_TRACK_FRACTION),
                    shaded = false,
                )
            }
    }
}

internal class CoverArt(val bitmap: Bitmap, val average: Color?)

internal suspend fun loadCover(context: Context, coverUri: String?): CoverArt? = withContext(Dispatchers.IO) {
    coverUri ?: return@withContext null
    runCatching {
        val uri = Uri.parse(coverUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = generateSequence(1) { it * 2 }.first { minOf(bounds.outWidth, bounds.outHeight) / (it * 2) < COVER_PX }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val square = centerSquare(decoded)
        val average = CoverTint.averageOf(Bitmap.createScaledBitmap(square, AVERAGE_SAMPLE, AVERAGE_SAMPLE, true))
        CoverArt(rounded(square), average)
    }.getOrNull()
}

internal fun centerSquare(source: Bitmap): Bitmap {
    val side = minOf(source.width, source.height)
    val cropped = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    return if (side > COVER_PX) Bitmap.createScaledBitmap(cropped, COVER_PX, COVER_PX, true) else cropped
}

internal fun rounded(square: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(square.width, square.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
    val radius = square.width * COVER_CORNER_RATIO
    Canvas(output).drawRoundRect(RectF(0f, 0f, square.width.toFloat(), square.height.toFloat()), radius, radius, paint)
    return output
}

private const val DARK_SECONDARY_TEXT_ALPHA = 0.72f
private const val DARK_TRACK_FRACTION = 0.22f
private const val LIGHT_TRACK_FRACTION = 0.14f
