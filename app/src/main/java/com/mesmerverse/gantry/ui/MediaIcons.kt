package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// The compose material-icons-core set has PlayArrow but no pause/jump
// glyphs; these keep us off the heavyweight icons-extended artifact.

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

val PauseIcon: ImageVector by lazy {
    icon("Pause") {
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 5f); lineTo(10f, 5f); lineTo(10f, 19f); lineTo(7f, 19f); close()
            moveTo(14f, 5f); lineTo(17f, 5f); lineTo(17f, 19f); lineTo(14f, 19f); close()
        }
    }
}

// Jump rings: a ¾ circle with an arrow across the gap (rewind counter-
// clockwise, forward clockwise). The widget overlays the jump seconds in
// the ring's open center.

val ReplayIcon: ImageVector by lazy {
    icon("Replay") {
        addPath(
            pathData = addPathNodes(
                "M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4" +
                    "c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z"
            ),
            fill = SolidColor(Color.White),
        )
    }
}

val ForwardIcon: ImageVector by lazy {
    icon("Forward") {
        addPath(
            pathData = addPathNodes(
                "M12 5V1l5 5-5 5V7c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6h2" +
                    "c0 4.42-3.58 8-8 8s-8-3.58-8-8 3.58-8 8-8z"
            ),
            fill = SolidColor(Color.White),
        )
    }
}
