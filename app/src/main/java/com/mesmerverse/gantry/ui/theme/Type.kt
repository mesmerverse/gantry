package com.mesmerverse.gantry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mesmerverse.gantry.R

// All bundled fonts are OFL variable fonts (see licenses/) — nothing is
// fetched at runtime.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFamily(resId: Int): FontFamily = FontFamily(
    listOf(
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
    ).map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    }
)

/** Selectable fonts: id -> family (null = system default). */
val GantryFonts: Map<String, FontFamily?> by lazy {
    mapOf(
        "space_grotesk" to variableFamily(R.font.space_grotesk),
        "inter" to variableFamily(R.font.inter),
        "manrope" to variableFamily(R.font.manrope),
        "nunito" to variableFamily(R.font.nunito),
        "outfit" to variableFamily(R.font.outfit),
        "rubik" to variableFamily(R.font.rubik),
        "sora" to variableFamily(R.font.sora),
        "work_sans" to variableFamily(R.font.work_sans),
        "jetbrains_mono" to variableFamily(R.font.jetbrains_mono),
        "system" to null,
    )
}

val FontOptions = listOf(
    "space_grotesk" to "Space Grotesk",
    "inter" to "Inter",
    "manrope" to "Manrope",
    "nunito" to "Nunito",
    "outfit" to "Outfit",
    "rubik" to "Rubik",
    "sora" to "Sora",
    "work_sans" to "Work Sans",
    "jetbrains_mono" to "JetBrains Mono",
    "system" to "System",
)

/** Typography scaled by the user's font-size setting and font choice. */
fun gantryTypography(scale: Float = 1f, fontId: String = "space_grotesk"): Typography {
    val family = GantryFonts[fontId] ?: GantryFonts.getValue("space_grotesk")
    val defaults = Typography()
    fun TextStyle.adapt() = copy(fontFamily = family, fontSize = fontSize * scale)
    return Typography(
        // The big clock.
        displayLarge = TextStyle(
            fontFamily = family,
            fontSize = (72f * scale).sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        ),
        // Date / weather row above the clock.
        titleMedium = TextStyle(
            fontFamily = family,
            fontSize = (16f * scale).sp,
            fontWeight = FontWeight.Medium,
        ),
        // Status row (battery, ticker, security) below the clock.
        labelLarge = TextStyle(
            fontFamily = family,
            fontSize = (14f * scale).sp,
            fontWeight = FontWeight.Medium,
        ),
        // App list entries.
        bodyLarge = TextStyle(
            fontFamily = family,
            fontSize = (18f * scale).sp,
            fontWeight = FontWeight.Normal,
        ),
        // Dialog and sheet supporting styles.
        headlineSmall = defaults.headlineSmall.adapt(),
        titleLarge = defaults.titleLarge.adapt(),
        bodyMedium = defaults.bodyMedium.adapt(),
        bodySmall = defaults.bodySmall.adapt(),
        labelMedium = defaults.labelMedium.adapt(),
        labelSmall = defaults.labelSmall.adapt(),
    )
}
