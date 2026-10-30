package com.mobyle.abbay.presentation.common.theme

import androidx.compose.ui.graphics.Color

// Dark theme colors (your app seems to use dark theme primarily)
val PrimaryColor = Color(0xFF222227) // Dark background
val SecondaryColor = Color(0xFFFFFFFF) // White text/icons
val TertiaryColor = Color(0xFF0BDA51) // Green accent
val SurfaceColor = Color(0xFF353539) // Slightly lighter dark surface

// The rest of AbbayiOS's palette (ColorConstants.plist), so both apps share one look.
val Background2Color = Color(0xFF1B1B1F) // Deepest dark: placeholder covers, text on the accent
val LightSilverColor = Color(0xFFD9D9D9) // Secondary text on dark
val SpanishGrayColor = Color(0xFF9B9B9B) // Placeholder glyphs

// Additional colors for better contrast and Android 15 compatibility
val OnPrimaryColor = Color(0xFFFFFFFF) // White on primary
val OnSecondaryColor = Color(0xFF000000) // Black on secondary
val OnTertiaryColor = Color(0xFF000000) // Black on tertiary
val OnSurfaceColor = Color(0xFFFFFFFF) // White on surface
val OnBackgroundColor = Color(0xFFFFFFFF) // White on background

// Error colors
// Lighter than the original #CF6679 (3.4:1 on the dark surface): 5.2:1 on surface, 6.7:1 on the background (WCAG AA).
val ErrorColor = Color(0xFFF28B96)
val OnErrorColor = Color(0xFF000000)

/**
 * Light theme: off-white neutrals instead of pure white, graphite text instead of
 * near-black (11.8:1 instead of 17:1), and a calmer green that still meets WCAG AA (≥ 4.6:1) on every light container.
 */
val LightBackgroundColor = Color(0xFFEDEEEA)
val LightSurfaceColor = Color(0xFFF6F7F3)
val LightTertiaryColor = Color(0xFF2B6C3E)
val LightOnColor = Color(0xFF2B2E2C)
val LightErrorColor = Color(0xFFB3443A)

/** The light theme's other Material roles, so no default (purple) Material color shows through. */
val LightSurfaceContainerLowest = Color(0xFFFAFBF8)
val LightSurfaceContainerLow = Color(0xFFF3F4F0)
val LightSurfaceContainer = Color(0xFFEEEFEA)
val LightSurfaceContainerHigh = Color(0xFFE8E9E4)
val LightSurfaceContainerHighest = Color(0xFFE3E5E0)
val LightOnSurfaceVariant = Color(0xFF555955)
val LightOutline = Color(0xFF767A75)
val LightOutlineVariant = Color(0xFFC6C9C3)
val LightAccentContainer = Color(0xFFD3E8D7)
val LightOnAccentContainer = Color(0xFF173A22)
val LightErrorContainer = Color(0xFFF6DEDA)
val LightOnErrorContainer = Color(0xFF5C1A14)
val LightInverseSurface = Color(0xFF2F322F)
val LightInverseOnSurface = Color(0xFFF0F1EC)
val LightInverseAccent = Color(0xFF8FD49F)

/** Black theme: pure black for OLED screens. */
val BlackColor = Color(0xFF000000)
