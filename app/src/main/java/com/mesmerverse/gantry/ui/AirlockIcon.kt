package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hatch-wheel glyph for the airlock (Android private space repurposed as a
 * containment area for tracking-heavy apps).
 */
val AirlockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Airlock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
        ) {
            // Rim
            moveTo(12f, 3.8f)
            arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 20.2f)
            arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3.8f)
            // Hub
            moveTo(12f, 9.4f)
            arcTo(2.6f, 2.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 14.6f)
            arcTo(2.6f, 2.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 9.4f)
            // Diagonal wheel spokes (hub to rim)
            moveTo(13.85f, 10.15f); lineTo(17.8f, 6.2f)
            moveTo(10.15f, 10.15f); lineTo(6.2f, 6.2f)
            moveTo(13.85f, 13.85f); lineTo(17.8f, 17.8f)
            moveTo(10.15f, 13.85f); lineTo(6.2f, 17.8f)
        }
    }.build()
}
