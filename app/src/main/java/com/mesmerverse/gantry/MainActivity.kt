package com.mesmerverse.gantry

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import com.mesmerverse.gantry.media.MediaNotificationListener
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mesmerverse.gantry.home.HomeScreen
import com.mesmerverse.gantry.home.HomeViewModel
import com.mesmerverse.gantry.settings.TextMode
import com.mesmerverse.gantry.ui.theme.DefaultAccent
import com.mesmerverse.gantry.ui.theme.GantryTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // A HOME app is killed and restarted often; the system can leave the
        // notification listener bound to a dead process and never reconnect,
        // so notifications go silent. Force a rebind whenever we start.
        requestNotificationRebind()
        setContent {
            // The security row replaces the status bar unless the user opts
            // to keep the system one; swipe down still peeks it when hidden.
            val showStatusBar by viewModel.showStatusBar.collectAsStateWithLifecycle()
            LaunchedEffect(showStatusBar) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (showStatusBar) {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            val wallpaperDarkText by viewModel.wallpaperSupportsDarkText.collectAsStateWithLifecycle()
            val textMode by viewModel.textMode.collectAsStateWithLifecycle()
            val customColorInt by viewModel.customTextColor.collectAsStateWithLifecycle()
            val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
            val fontId by viewModel.fontId.collectAsStateWithLifecycle()
            val accentColorInt by viewModel.accentColor.collectAsStateWithLifecycle()
            val customColor =
                if (textMode == TextMode.Custom) customColorInt?.let { Color(it) } else null
            val darkText = when {
                customColor != null -> customColor.luminance() < 0.5f
                textMode == TextMode.Light -> false
                textMode == TextMode.Dark -> true
                else -> wallpaperDarkText
            }
            GantryTheme(
                darkText = darkText,
                customTextColor = customColor,
                fontScale = fontScale,
                fontId = fontId,
                accentColor = accentColorInt?.let { Color(it) } ?: DefaultAccent,
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    darkText = darkText,
                    onOpenCalendar = {
                        launchSafely(
                            Intent(Intent.ACTION_VIEW).setData(
                                CalendarContract.CONTENT_URI.buildUpon()
                                    .appendPath("time").build()
                            )
                        )
                    },
                    onOpenClock = { launchSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
                    onOpenBatterySettings = {
                        launchSafely(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                    },
                    onOpenCamera = {
                        launchSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                    },
                    onOpenSettings = { launchSafely(Intent(Settings.ACTION_SETTINGS)) },
                    onOpenTtsSettings = {
                        // No public constant; this is the stable de-facto action,
                        // with a fallback to the top-level settings.
                        try {
                            startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                        } catch (_: Exception) {
                            launchSafely(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationRebind()
    }

    private fun requestNotificationRebind() {
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(this, MediaNotificationListener::class.java)
            )
        }
    }

    private fun launchSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // No app handles this intent, or the resolver refused it
            // (SecurityException, not just ActivityNotFoundException — a
            // missing clock app crashed the launcher). A failed hand-off
            // must never take the home screen down.
        }
    }
}
