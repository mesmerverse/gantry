package com.mesmerverse.gantry.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

// Full-colour glyphs lit like the app icon: gold bodies, glass, dark ink.
// Drawn with their own colours — render with Icon(tint = Color.Unspecified)
// or Image, never a tint.

private val Ink = Color(0xFF14181F)
private val Burnt = Color(0xFFB85A1F)
private val Lamp = Color(0xFFFFF4C2)

/** Vertical gold, light at [top] to deep at [bottom]. */
private fun goldV(top: Float, bottom: Float) = Brush.linearGradient(
    0f to Color(0xFFF9C07A), 1f to Color(0xFFD2702C),
    start = Offset(12f, top), end = Offset(12f, bottom),
)

/** Diagonal glass, bright top-left to cool bottom-right. */
private fun glass(from: Offset, to: Offset) = Brush.linearGradient(
    0f to Color(0xFFE4F5FF), 1f to Color(0xFF6FA3C9),
    start = from, end = to,
)

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
    "M${cx - rx},$cy A$rx,$ry 0 1,0 ${cx + rx},$cy A$rx,$ry 0 1,0 ${cx - rx},$cy Z"

private fun circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

private fun ImageVector.Builder.shape(d: String, fill: Brush, alpha: Float = 1f) {
    addPath(pathData = addPathNodes(d), fill = fill, fillAlpha = alpha)
}

private fun ImageVector.Builder.shape(d: String, fill: Color, alpha: Float = 1f) =
    shape(d, SolidColor(fill), alpha)

/**
 * Dock handle rocket: gold body with a side sheen, glass porthole in a dark
 * rim, burnt-orange fins and base band, exhaust flame below.
 */
val RocketLitIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RocketLit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Flame fades out as it trails away.
        shape(
            "M9.6,17 C10,20.2 11,22.6 12,23.4 C13,22.6 14,20.2 14.4,17 Z",
            Brush.linearGradient(
                0f to Color(0xFFFFE3A3), 0.55f to Color(0xFFF2A65A), 1f to Color(0x00E5484D),
                start = Offset(12f, 17f), end = Offset(12f, 23.4f),
            ),
        )
        shape("M8.1,12.9 L4.3,18.2 L8.1,17.2 Z M15.9,12.9 L19.7,18.2 L15.9,17.2 Z", Burnt)
        shape(
            "M12,1.6 C15.2,5.3 16,9 16,12.6 L16,17.2 L8,17.2 L8,12.6 C8,9 8.8,5.3 12,1.6 Z",
            goldV(1.6f, 17.2f),
        )
        shape(
            "M12,2.7 C10.1,5.2 9.3,8.6 9.3,12.6 L9.3,16.3 L10.4,16.3 L10.4,12.6 " +
                "C10.4,9 11,5.9 12,3.9 Z",
            Color.White, alpha = 0.38f,
        )
        shape("M8,15.5 L16,15.5 L16,17.2 L8,17.2 Z", Burnt)
        shape(circle(12f, 9.8f, 2.65f), Ink)
        shape(circle(12f, 9.8f, 1.75f), glass(Offset(10.25f, 8.05f), Offset(13.75f, 11.55f)))
        shape(circle(11.35f, 9.15f, 0.55f), Color.White, alpha = 0.9f)
    }.build()
}

/**
 * Corner satellite, tilted as if orbiting the dock: gold body with a sheen
 * and burnt-orange band, blue solar panels with cell lines, a glass dish on
 * a mast, and a lit antenna tip.
 */
val SatelliteLitIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SatelliteLit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        group(name = "orbit", rotate = -28f, pivotX = 12f, pivotY = 12f) {
            // Struts from body to panels.
            shape("M7.6,11.4 L9.6,11.4 L9.6,12.6 L7.6,12.6 Z M14.4,11.4 L16.4,11.4 L16.4,12.6 L14.4,12.6 Z", Ink, alpha = 0.7f)
            val panel = Brush.linearGradient(
                0f to Color(0xFF7FB2DA), 1f to Color(0xFF2F5D8A),
                start = Offset(12f, 9.2f), end = Offset(12f, 14.8f),
            )
            shape(roundedRect(1.2f, 9.2f, 6.6f, 5.6f, 0.7f), panel)
            shape(roundedRect(16.2f, 9.2f, 6.6f, 5.6f, 0.7f), panel)
            // Cell lines, then a lit top edge.
            shape(
                "M3.3,9.2 h0.5 v5.6 h-0.5 Z M5.4,9.2 h0.5 v5.6 h-0.5 Z M1.2,11.8 h6.6 v0.45 H1.2 Z " +
                    "M18.3,9.2 h0.5 v5.6 h-0.5 Z M20.4,9.2 h0.5 v5.6 h-0.5 Z M16.2,11.8 h6.6 v0.45 h-6.6 Z",
                Ink, alpha = 0.35f,
            )
            shape("M1.2,9.2 h6.6 v0.6 H1.2 Z M16.2,9.2 h6.6 v0.6 h-6.6 Z", Color.White, alpha = 0.35f)
            // Body.
            shape(roundedRect(9.4f, 8.6f, 5.2f, 7.4f, 1.1f), goldV(8.6f, 16f))
            shape("M10,9.3 h0.9 v6 h-0.9 Z", Color.White, alpha = 0.38f)
            shape("M9.4,14.4 h5.2 v1.6 H9.4 Z", Burnt)
            // Dish on its mast, antenna with a lit tip.
            shape("M11.6,6.9 h0.8 v1.8 h-0.8 Z", Ink, alpha = 0.75f)
            shape("M9.3,6.9 A2.7,1.6 0 0,1 14.7,6.9 Z", glass(Offset(9.3f, 5.3f), Offset(14.7f, 6.9f)))
            shape("M11.75,3.6 h0.5 v3.1 h-0.5 Z", Ink, alpha = 0.75f)
            shape(circle(12f, 3.3f, 1.3f), Lamp, alpha = 0.35f)
            shape(circle(12f, 3.3f, 0.75f), Lamp)
        }
    }.build()
}

/**
 * Home-folder glyph: two shipping containers stacked like cargo on a pad,
 * in the folder's [color], with ribbed walls, lit top rails, and shaded
 * bottom rails. Built per colour, so cache it (remember(color)).
 */
fun containerStackIcon(color: Color): ImageVector =
    ImageVector.Builder(
        name = "ContainerStack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        val shade = Color.Black
        // Upper container, set back to the right.
        shape(roundedRect(6f, 3.6f, 15.5f, 7.6f, 1.1f), color)
        shape(ribs(9f, 5.4f, 4.2f), shade, alpha = 0.24f)
        shape("M6,3.6 L21.5,3.6 L21.5,4.8 L6,4.8 Z", Color.White, alpha = 0.35f)
        shape("M6,10 L21.5,10 L21.5,11.2 L6,11.2 Z", shade, alpha = 0.38f)
        // Lower container, forward and to the left.
        shape(roundedRect(2.5f, 12.4f, 15.5f, 7.6f, 1.1f), color)
        shape("M2.5,12.4 L18,12.4 L18,13.6 L2.5,13.6 Z", Color.White, alpha = 0.35f)
        shape(ribs(5.5f, 14.2f, 4.2f), shade, alpha = 0.24f)
        shape("M2.5,18.8 L18,18.8 L18,20 L2.5,20 Z", shade, alpha = 0.38f)
    }.build()

/** Four 1-wide corrugation ribs, 2.6 apart, starting at [x]. */
private fun ribs(x: Float, y: Float, h: Float) = (0 until 4).joinToString(" ") { i ->
    val left = x + i * 2.6f
    "M$left,$y L${left + 1f},$y L${left + 1f},${y + h} L$left,${y + h} Z"
}

private fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r},$y L${x + w - r},$y A$r,$r 0 0,1 ${x + w},${y + r} " +
        "L${x + w},${y + h - r} A$r,$r 0 0,1 ${x + w - r},${y + h} " +
        "L${x + r},${y + h} A$r,$r 0 0,1 $x,${y + h - r} " +
        "L$x,${y + r} A$r,$r 0 0,1 ${x + r},$y Z"
