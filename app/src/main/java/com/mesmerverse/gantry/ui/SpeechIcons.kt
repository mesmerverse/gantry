package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Custom speaker glyphs — the material-icons-core set has no volume icon, and
// Gantry bundles its own vectors rather than pull the extended icon library
// (see [AirlockIcon]). Icon() recolors these via its tint, so the stroke color
// here is only a placeholder.

private fun speakerBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
)

private fun ImageVector.Builder.speakerCone() {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(4f, 9f)
        lineTo(7.5f, 9f)
        lineTo(11.5f, 5f)
        lineTo(11.5f, 19f)
        lineTo(7.5f, 15f)
        lineTo(4f, 15f)
        close()
    }
}

/** Speaker with sound waves — read-aloud armed. */
val SpeakerOnIcon: ImageVector by lazy {
    speakerBuilder("SpeakerOn").apply {
        speakerCone()
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Inner wave
            moveTo(15f, 9.5f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 14.5f)
            // Outer wave
            moveTo(17.5f, 7f)
            arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.5f, 17f)
        }
    }.build()
}

/** Speaker with a slash — read-aloud off. */
val SpeakerOffIcon: ImageVector by lazy {
    speakerBuilder("SpeakerOff").apply {
        speakerCone()
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(15f, 9f)
            lineTo(20f, 15f)
            moveTo(20f, 9f)
            lineTo(15f, 15f)
        }
    }.build()
}
