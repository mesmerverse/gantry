package com.mesmerverse.gantry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** Default highlight for airlock/exposure accents (the launcher-icon flame). */
val DefaultAccent = Color(0xFFF2A65A)

/** User-selectable highlight color; see Appearance settings. */
val LocalAccentColor = compositionLocalOf { DefaultAccent }

// Light text for dark wallpapers.
private val LightTextScheme = darkColorScheme(
    primary = Color(0xFFE8EDF2),
    onPrimary = Color(0xFF101418),
    secondary = Color(0xFFB0BEC9),
    background = Color.Transparent,
    surface = Color(0xF2101418),
    onBackground = Color(0xFFE8EDF2),
    onSurface = Color(0xFFE8EDF2),
)

// Dark text for wallpapers whose colors support it (HINT_SUPPORTS_DARK_TEXT).
private val DarkTextScheme = lightColorScheme(
    primary = Color(0xFF15191E),
    onPrimary = Color(0xFFE8EDF2),
    secondary = Color(0xFF48525C),
    background = Color.Transparent,
    surface = Color(0xF2ECEFF2),
    onBackground = Color(0xFF15191E),
    onSurface = Color(0xFF15191E),
)

@Composable
fun GantryTheme(
    darkText: Boolean = false,
    customTextColor: Color? = null,
    fontScale: Float = 1f,
    fontId: String = "space_grotesk",
    accentColor: Color = DefaultAccent,
    content: @Composable () -> Unit,
) {
    val base = if (darkText) DarkTextScheme else LightTextScheme
    val scheme = if (customTextColor != null) {
        base.copy(
            onBackground = customTextColor,
            secondary = customTextColor.copy(alpha = 0.7f),
        )
    } else {
        base
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = remember(fontScale, fontId) {
            gantryTypography(fontScale, fontId)
        },
    ) {
        // The launcher draws directly over the wallpaper (no Surface), so the
        // default content color must be provided explicitly or Text falls back
        // to black regardless of the color scheme.
        CompositionLocalProvider(
            LocalContentColor provides scheme.onBackground,
            LocalAccentColor provides accentColor,
            content = content,
        )
    }
}
