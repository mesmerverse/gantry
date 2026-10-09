package com.mesmerverse.gantry.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesmerverse.gantry.ui.theme.LocalAccentColor

internal enum class TourStep {
    Welcome, OpenDrawer, Drawer, CloseDrawer, Widgets, Gestures, Airlock, Done,
}

/**
 * First-run guided tour. A dimming scrim spotlights the piece of UI each step
 * talks about, and — the important part — the scrim has no pointer handlers,
 * so the launcher underneath stays fully live: the rocket steps advance when
 * the user actually swipes up on (or taps) the rocket, teaching the muscle
 * memory instead of describing it. Replayable from Settings → About → How Gantry works.
 */
@Composable
internal fun TourOverlay(
    drawerOpen: Boolean,
    hasAirlock: Boolean,
    onRequestDrawer: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    val steps = remember(hasAirlock) {
        buildList {
            add(TourStep.Welcome)
            add(TourStep.OpenDrawer)
            add(TourStep.Drawer)
            add(TourStep.CloseDrawer)
            add(TourStep.Widgets)
            add(TourStep.Gestures)
            if (hasAirlock) add(TourStep.Airlock)
            add(TourStep.Done)
        }
    }
    var step by remember { mutableStateOf(TourStep.Welcome) }

    fun advance() {
        val next = steps.getOrNull(steps.indexOf(step) + 1)
        if (next == null) onFinish() else step = next
    }

    // The drawer steps advance on the real drawer state, however the user got
    // there (handle tap, swipe, back button, or the card's fallback button).
    LaunchedEffect(drawerOpen, step) {
        step = when {
            drawerOpen && step == TourStep.OpenDrawer -> TourStep.Drawer
            !drawerOpen && step == TourStep.Drawer -> TourStep.Widgets
            !drawerOpen && step == TourStep.CloseDrawer -> TourStep.Widgets
            else -> step
        }
    }

    val accent = LocalAccentColor.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Offscreen compositing so BlendMode.Clear punches a real hole in the
        // scrim instead of painting black-on-black.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            when (step) {
                // These steps ask the user to look around, not at one spot.
                TourStep.Drawer, TourStep.Gestures -> Unit
                else -> drawRect(Color.Black.copy(alpha = 0.55f))
            }
            when (step) {
                TourStep.OpenDrawer, TourStep.CloseDrawer, TourStep.Airlock -> {
                    // The dock's rocket handle: a 44dp circle inside the pill's
                    // 6dp padding, 32dp above the bottom edge (see Dock()).
                    val center = Offset(size.width / 2f, size.height - 63.dp.toPx())
                    val radius = 40.dp.toPx()
                    drawCircle(Color.Transparent, radius, center, blendMode = BlendMode.Clear)
                    drawCircle(accent, radius, center, style = Stroke(2.dp.toPx()))
                }
                TourStep.Widgets -> {
                    val topLeft = Offset(12.dp.toPx(), 8.dp.toPx())
                    val cutout = Size(size.width - 24.dp.toPx(), 260.dp.toPx())
                    val corner = CornerRadius(24.dp.toPx())
                    drawRoundRect(Color.Transparent, topLeft, cutout, corner, blendMode = BlendMode.Clear)
                    drawRoundRect(accent, topLeft, cutout, corner, style = Stroke(2.dp.toPx()))
                }
                else -> Unit
            }
        }

        val stepIndex = steps.indexOf(step)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                // The widgets spotlight is up top, so its card moves down —
                // clear of the cutout and of the dock.
                .padding(bottom = if (step == TourStep.Widgets) 132.dp else 0.dp),
            contentAlignment = if (step == TourStep.Widgets) {
                Alignment.BottomCenter
            } else {
                Alignment.Center
            },
        ) {
            when (step) {
                TourStep.Welcome -> TourCard(
                    title = "Welcome to Gantry",
                    body = "A one-minute tour of how the launcher works. " +
                        "You can replay it anytime from Settings.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Start", onPrimary = ::advance, onSkip = onFinish,
                )
                TourStep.OpenDrawer -> TourCard(
                    title = "All your apps",
                    body = "Swipe up from the glowing rocket below to open the " +
                        "app list. Swiping up from anywhere else works too.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Open it for me",
                    onPrimary = { onRequestDrawer(true) },
                    onSkip = onFinish,
                )
                TourStep.Drawer -> TourCard(
                    title = "Find anything",
                    body = "Tap the magnifier below to search, jump with the A–Z rail, " +
                        "or filter with the tag chips up top. Long-press any app to pin " +
                        "it to Home, the dock, or the corner, tag it, or rename it.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Next", onPrimary = ::advance, onSkip = onFinish,
                )
                TourStep.CloseDrawer -> TourCard(
                    title = "Back home",
                    body = "Tap the rocket to close the list.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Close it for me",
                    onPrimary = { onRequestDrawer(false) },
                    onSkip = onFinish,
                )
                TourStep.Widgets -> TourCard(
                    title = "Glance and go",
                    body = "Everything up top is tappable: the date opens your calendar, " +
                        "the clock your alarms, the battery your power usage. " +
                        "Notifications gather here too — tap one to open it, " +
                        "swipe it aside to clear it.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Next", onPrimary = ::advance, onSkip = onFinish,
                )
                TourStep.Gestures -> TourCard(
                    title = "Gestures",
                    body = "Swipe left for the camera, right for your phone's settings. " +
                        "Long-press an app or folder to rearrange Home. Gantry's own " +
                        "settings are behind the gear in the dock when the app list " +
                        "is open.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Next", onPrimary = ::advance, onSkip = onFinish,
                )
                TourStep.Airlock -> TourCard(
                    title = "The Airlock",
                    body = "Apps with an amber ring live in the Airlock — a contained " +
                        "space for apps you don't fully trust. Tap one to unlock and " +
                        "launch it; long-press the rocket to seal or open the Airlock.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Next", onPrimary = ::advance, onSkip = onFinish,
                )
                TourStep.Done -> TourCard(
                    title = "Ready for launch",
                    body = "That's the whole tour. When in doubt, long-press something. " +
                        "Replay this anytime from Settings → About → How Gantry works.",
                    stepIndex = stepIndex, stepCount = steps.size,
                    primaryLabel = "Done", onPrimary = ::advance, onSkip = null,
                )
            }
        }
    }
}

@Composable
private fun TourCard(
    title: String,
    body: String,
    stepIndex: Int,
    stepCount: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onSkip: (() -> Unit)?,
) {
    val accent = LocalAccentColor.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF20E0F12),
        modifier = Modifier
            .fillMaxWidth()
            // Absorb taps so the card never launches whatever sits behind it.
            .pointerInput(Unit) { detectTapGestures {} },
    ) {
        CompositionLocalProvider(LocalContentColor provides Color(0xFFECECEC)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xB3ECECEC),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(stepCount) { i ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (i == stepIndex) accent else Color(0x40FFFFFF),
                                        CircleShape,
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (onSkip != null) {
                        TextButton(onClick = onSkip) {
                            Text("Skip", color = Color(0x99ECECEC))
                        }
                    }
                    TextButton(onClick = onPrimary) {
                        Text(primaryLabel, color = accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
