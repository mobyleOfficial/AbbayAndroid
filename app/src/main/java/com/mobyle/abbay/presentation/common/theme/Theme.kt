package com.mobyle.abbay.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.settings.ThemeMode

/** The AbbayAndroid palette. The UI paints its backgrounds with `primary` and its content with `onSurface`. */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = TertiaryColor,
    background = PrimaryColor,
    surface = SurfaceColor,
    onPrimary = OnPrimaryColor,
    onSecondary = OnSecondaryColor,
    onTertiary = OnTertiaryColor,
    onBackground = OnBackgroundColor,
    onSurface = OnSurfaceColor,
    error = ErrorColor,
    onError = OnErrorColor,
)

private val BlackColorScheme = DarkColorScheme.copy(primary = BlackColor, background = BlackColor, surface = BlackColor)

private val LightColorScheme = lightColorScheme(
    primary = LightBackgroundColor,
    secondary = LightOnColor,
    tertiary = LightTertiaryColor,
    background = LightBackgroundColor,
    surface = LightSurfaceColor,
    onPrimary = LightOnColor,
    onSecondary = LightSurfaceColor,
    onTertiary = LightSurfaceColor,
    onBackground = LightOnColor,
    onSurface = LightOnColor,
    error = LightErrorColor,
    onError = LightSurfaceColor,
    surfaceVariant = LightSurfaceContainerHighest,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightBackgroundColor,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceBright = LightSurfaceColor,
    surfaceDim = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    primaryContainer = LightSurfaceContainerHigh,
    onPrimaryContainer = LightOnColor,
    secondaryContainer = LightAccentContainer,
    onSecondaryContainer = LightOnAccentContainer,
    tertiaryContainer = LightAccentContainer,
    onTertiaryContainer = LightOnAccentContainer,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInverseAccent,
)

/**
 * UI-09. Dynamic colors are mapped onto the roles this UI uses: wallpaper background
 * as `primary`, wallpaper accent as `tertiary`.
 */
@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
    val baseScheme = when {
        themeMode == ThemeMode.BLACK -> BlackColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val wallpaper = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        wallpaper.asAbbayRoles(isBlack = themeMode == ThemeMode.BLACK)
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Older Android versions color the bars themselves; on 15+ the app draws behind them (MainActivity).
            // Transparent: MainActivity paints the strip behind it, in the app bars' color or the open player's.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.primary.toArgb()
            // No system scrim over the navigation bar: the app's own background shows through, like the status bar.
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AbbayTypography(
            primaryTextColor = colorScheme.onSurface,
            secondaryTextColor = colorScheme.onSurface.copy(alpha = 0.7f),
        ),
        content = content,
    )
}

private fun ColorScheme.asAbbayRoles(isBlack: Boolean): ColorScheme {
    val backgroundColor = if (isBlack) BlackColor else background
    return copy(
        primary = backgroundColor,
        onPrimary = onBackground,
        background = backgroundColor,
        surface = if (isBlack) BlackColor else surfaceContainer,
        tertiary = primary,
        onTertiary = onPrimary,
        secondary = onSurface,
        onSecondary = surface,
    )
}
