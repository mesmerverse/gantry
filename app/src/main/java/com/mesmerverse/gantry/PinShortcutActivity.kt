package com.mesmerverse.gantry

import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mesmerverse.gantry.ui.theme.GantryTheme

/**
 * System entry point for "Add to Home screen" pin requests (PWAs, browser
 * shortcuts, app-provided shortcuts). Declaring this handler is also what
 * makes ShortcutManager.isRequestPinShortcutSupported() true while Gantry
 * holds the HOME role — without it, browsers hide their install option
 * entirely. Accepted shortcuts surface in the app list; the user places
 * them on the home screen by tagging, like any app.
 */
class PinShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        // Only the system can hand us a valid request; anything else finishes.
        val request = launcherApps.getPinItemRequest(intent)
        val shortcut = request?.takeIf {
            it.isValid && it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        }?.shortcutInfo
        if (shortcut == null) {
            finish()
            return
        }

        val label = (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.`package`).toString()
        val icon = loadIcon(launcherApps, shortcut)
        val source = sourceAppLabel(shortcut.`package`)
        // The label/package reveal which site is being pinned, so keep this out
        // of release logs.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.i(TAG, "pin request: pkg=${shortcut.`package`} id=${shortcut.id} label=$label")
        }

        setContent {
            GantryTheme {
                AlertDialog(
                    onDismissRequest = {
                        Log.i(TAG, "dismissed without a choice")
                        finish()
                    },
                    icon = icon?.let {
                        {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    title = { Text(label) },
                    text = { PinDialogBody(source) },
                    confirmButton = {
                        TextButton(onClick = { accept(request) }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            Log.i(TAG, "cancelled")
                            finish()
                        }) { Text("Don't add") }
                    },
                )
            }
        }
    }

    /** App label for the package that sent the request, or the package name. */
    private fun sourceAppLabel(packageName: String): String = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    /**
     * The confirm prompt, which app requested it, and a prominent trust
     * warning — a pin request can come from any installed app, not just a
     * browser the user deliberately chose to "install" a site from.
     */
    @androidx.compose.runtime.Composable
    private fun PinDialogBody(source: String) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add this to your app list? It was requested by $source.")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(top = 1.dp),
                    )
                    Text(
                        "Only add shortcuts from places you trust — a website or " +
                            "app you know and chose to install from. If you're not " +
                            "sure whether this is trustworthy, or aren't sure what " +
                            "that means, don't add it.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    private fun loadIcon(launcherApps: LauncherApps, shortcut: ShortcutInfo): ImageBitmap? =
        runCatching {
            launcherApps.getShortcutIconDrawable(shortcut, resources.displayMetrics.densityDpi)
        }.getOrNull()?.let {
            val sizePx = (48 * resources.displayMetrics.density).toInt()
            it.toBitmap(sizePx, sizePx).asImageBitmap()
        }

    private fun accept(request: LauncherApps.PinItemRequest) {
        val accepted = runCatching { request.accept() }
            .onFailure { Log.w(TAG, "pin accept threw", it) }
            .getOrDefault(false)
        Log.i(TAG, "pin accept result=$accepted")
        finish()
    }

    private companion object {
        const val TAG = "PinShortcutActivity"
    }
}
