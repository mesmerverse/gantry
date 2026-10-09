package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mesmerverse.gantry.weather.WeatherCondition

// Monochrome weather glyphs so the status row stays one uniform tinted system
// instead of a lone full-color emoji. Bundled like the other custom vectors
// (see [SpeechIcons]); Icon() applies the tint, so the placeholder color here
// doesn't matter.

private fun weatherBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
)

/** Appends a filled circle subpath (two half-arcs). */
private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, 2 * r, 0f)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, -2 * r, 0f)
    close()
}

/** A filled cloud silhouette (rounded lobes over a slab), optionally shifted. */
private fun androidx.compose.ui.graphics.vector.PathBuilder.cloudShape(dx: Float = 0f, dy: Float = 0f) {
    // base slab
    moveTo(5.5f + dx, 17.5f + dy)
    lineTo(18.5f + dx, 17.5f + dy)
    lineTo(18.5f + dx, 13.5f + dy)
    lineTo(5.5f + dx, 13.5f + dy)
    close()
    circle(8.5f + dx, 13.3f + dy, 3f)
    circle(12.5f + dx, 10.8f + dy, 4.2f)
    circle(15.8f + dx, 13.3f + dy, 3.3f)
}

private fun ImageVector.Builder.filledCloud(dx: Float = 0f, dy: Float = 0f) {
    path(fill = SolidColor(Color.White)) { cloudShape(dx, dy) }
}

private fun ImageVector.Builder.strokeLines(
    segments: List<FloatArray>, width: Float = 2f,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
    ) {
        segments.forEach { (x0, y0, x1, y1) ->
            moveTo(x0, y0); lineTo(x1, y1)
        }
    }
}

private val Clear: ImageVector by lazy {
    weatherBuilder("WeatherClear").apply {
        path(fill = SolidColor(Color.White)) { circle(12f, 12f, 4.2f) }
        strokeLines(
            listOf(
                floatArrayOf(12f, 3f, 12f, 5f), floatArrayOf(12f, 19f, 12f, 21f),
                floatArrayOf(3f, 12f, 5f, 12f), floatArrayOf(19f, 12f, 21f, 12f),
                floatArrayOf(5.6f, 5.6f, 7f, 7f), floatArrayOf(17f, 17f, 18.4f, 18.4f),
                floatArrayOf(18.4f, 5.6f, 17f, 7f), floatArrayOf(7f, 17f, 5.6f, 18.4f),
            ),
        )
    }.build()
}

private val Cloudy: ImageVector by lazy {
    weatherBuilder("WeatherCloudy").apply { filledCloud() }.build()
}

private val PartlyCloudy: ImageVector by lazy {
    weatherBuilder("WeatherPartly").apply {
        path(fill = SolidColor(Color.White)) { circle(8.5f, 7.5f, 3f) }
        strokeLines(
            listOf(
                floatArrayOf(8.5f, 1.8f, 8.5f, 3f), floatArrayOf(2.8f, 7.5f, 4f, 7.5f),
                floatArrayOf(4.3f, 3.3f, 5.2f, 4.2f), floatArrayOf(12.7f, 3.3f, 11.8f, 4.2f),
            ),
        )
        filledCloud(dx = 1.5f, dy = 2.2f)
    }.build()
}

private val Fog: ImageVector by lazy {
    weatherBuilder("WeatherFog").apply {
        strokeLines(
            listOf(
                floatArrayOf(4f, 7f, 20f, 7f), floatArrayOf(4f, 11f, 20f, 11f),
                floatArrayOf(6f, 15f, 18f, 15f), floatArrayOf(4f, 19f, 16f, 19f),
            ),
        )
    }.build()
}

private fun ImageVector.Builder.cloudWithLines(segments: List<FloatArray>): ImageVector.Builder {
    filledCloud(dy = -1.5f)
    strokeLines(segments, width = 2f)
    return this
}

private val Drizzle: ImageVector by lazy {
    weatherBuilder("WeatherDrizzle").apply {
        cloudWithLines(
            listOf(floatArrayOf(9f, 18f, 9f, 20f), floatArrayOf(14f, 18f, 14f, 20f)),
        )
    }.build()
}

private val Rain: ImageVector by lazy {
    weatherBuilder("WeatherRain").apply {
        cloudWithLines(
            listOf(
                floatArrayOf(8.5f, 17.5f, 7.5f, 21f),
                floatArrayOf(12f, 17.5f, 11f, 21f),
                floatArrayOf(15.5f, 17.5f, 14.5f, 21f),
            ),
        )
    }.build()
}

private val Snow: ImageVector by lazy {
    weatherBuilder("WeatherSnow").apply {
        filledCloud(dy = -1.5f)
        path(fill = SolidColor(Color.White)) {
            circle(9f, 19.5f, 1f); circle(12f, 21f, 1f); circle(15f, 19.5f, 1f)
        }
    }.build()
}

private val Thunder: ImageVector by lazy {
    weatherBuilder("WeatherThunder").apply {
        filledCloud(dy = -1.5f)
        path(fill = SolidColor(Color.White)) {
            moveTo(12.5f, 16.5f)
            lineTo(9.5f, 20.5f)
            lineTo(11.6f, 20.5f)
            lineTo(10.5f, 23.5f)
            lineTo(14.5f, 19f)
            lineTo(12.4f, 19f)
            lineTo(13.8f, 16.5f)
            close()
        }
    }.build()
}

/** Glyph for a [WeatherCondition]; Unknown falls back to a plain cloud. */
fun weatherIcon(condition: WeatherCondition): ImageVector = when (condition) {
    WeatherCondition.Clear -> Clear
    WeatherCondition.PartlyCloudy -> PartlyCloudy
    WeatherCondition.Cloudy -> Cloudy
    WeatherCondition.Fog -> Fog
    WeatherCondition.Drizzle -> Drizzle
    WeatherCondition.Rain -> Rain
    WeatherCondition.Snow -> Snow
    WeatherCondition.Thunder -> Thunder
    WeatherCondition.Unknown -> Cloudy
}
