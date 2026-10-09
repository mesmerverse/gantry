package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Bold rocket-capsule silhouette with a porthole cutout — the dock handle's
 * resting state ("tap the rocket to launch"). Shares its form with the app
 * icon so the two read as one mark.
 */
val RocketIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Gantry",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Capsule body with the porthole as a cut-out (even-odd).
        path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2f)
            curveTo(15.1f, 5.6f, 15.9f, 9.2f, 15.9f, 12.6f)
            lineTo(15.9f, 17f)
            lineTo(8.1f, 17f)
            lineTo(8.1f, 12.6f)
            curveTo(8.1f, 9.2f, 8.9f, 5.6f, 12f, 2f)
            close()
            // Porthole
            moveTo(12f, 7.6f)
            arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 12f)
            arcTo(2.2f, 2.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 7.6f)
            close()
        }
        // Fins.
        path(fill = SolidColor(Color.White)) {
            moveTo(8.1f, 13.4f)
            lineTo(4.6f, 18f)
            lineTo(8.1f, 17f)
            close()
            moveTo(15.9f, 13.4f)
            lineTo(19.4f, 18f)
            lineTo(15.9f, 17f)
            close()
        }
    }.build()
}
