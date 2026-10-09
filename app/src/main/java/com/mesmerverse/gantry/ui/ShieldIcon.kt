package com.mesmerverse.gantry.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * VPN mark for the header when the connected VPN isn't Tailscale (which
 * keeps its mesh): a shield outline with a keyhole. Single colour so Icon's
 * tint carries the state, same weight as [TailnetIcon].
 */
val ShieldIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Shield",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M12,2.8 L19.2,5.6 L19.2,11 C19.2,15.8 16.2,19.6 12,21.2 " +
                    "C7.8,19.6 4.8,15.8 4.8,11 L4.8,5.6 Z",
            ),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineCap = StrokeCap.Round,
        )
        // Keyhole: a round head over a tapered stem.
        addPath(
            pathData = addPathNodes(
                "M12,8 A2.1,2.1 0 1,1 11.99,8 Z M11,11.6 L13,11.6 L13.5,15.4 L10.5,15.4 Z",
            ),
            fill = SolidColor(Color.White),
        )
    }.build()
}
