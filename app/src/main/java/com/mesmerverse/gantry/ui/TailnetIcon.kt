package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Tailnet mark for the header: three nodes meshed together. Single colour so
 * Icon's tint carries the health state.
 */
val TailnetIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Tailnet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Links between the nodes.
        addPath(
            pathData = addPathNodes("M12,6 L5.5,17.5 M12,6 L18.5,17.5 M5.5,17.5 L18.5,17.5"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        )
        // Nodes.
        listOf(12f to 6f, 5.5f to 17.5f, 18.5f to 17.5f).forEach { (x, y) ->
            addPath(
                pathData = addPathNodes(
                    "M${x - 3f},$y A3,3 0 1,0 ${x + 3f},$y A3,3 0 1,0 ${x - 3f},$y Z",
                ),
                fill = SolidColor(Color.White),
            )
        }
    }.build()
}
