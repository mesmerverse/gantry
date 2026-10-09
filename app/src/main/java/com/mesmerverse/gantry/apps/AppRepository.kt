package com.mesmerverse.gantry.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val baseLabel: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    val monoIcon: ImageBitmap?,
    /** Foreground layer alone, full colour, no background plate — for [IconStyle.Flat]. */
    val flatIcon: ImageBitmap? = null,
    val iconStyle: IconStyle = IconStyle.Default,
    /** Applied [AppStyle.iconSource]; null = automatic (pack mapping or system). */
    val iconSource: String? = null,
    /** Set for pinned shortcuts (PWAs, browser/app shortcuts); null for apps. */
    val shortcutId: String? = null,
) {
    val key: String
        get() = shortcutId?.let { shortcutKey(user, componentName.packageName, it) }
            ?: "${user.hashCode()}/${componentName.flattenToString()}"

    companion object {
        /** Stable tag/style key for a pinned shortcut, usable before an entry exists. */
        fun shortcutKey(user: UserHandle, packageName: String, shortcutId: String) =
            "${user.hashCode()}/shortcut:$packageName/$shortcutId"
    }
}

/**
 * Loads launchable activities for all profiles visible to this launcher via
 * [LauncherApps]. No QUERY_ALL_PACKAGES; visibility comes from the manifest
 * <queries> block plus the profile access LauncherApps grants home apps.
 */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val iconSizePx = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
        // Every app's icon is another package's resource: one broken or
        // oversized icon (or an unavailable profile) must cost that app its
        // icon, never crash the home screen at every start.
        val fallback by lazy { placeholderIcon(iconSizePx) }
        val apps = launcherApps.profiles.flatMap { user ->
            runCatching { launcherApps.getActivityList(null, user) }
                .getOrDefault(emptyList())
                .mapNotNull { info ->
                    runCatching {
                        val label = info.label.toString()
                        val drawable = runCatching { info.getIcon(0) }.getOrNull()
                        val adaptive = drawable as? AdaptiveIconDrawable
                        AppEntry(
                            label = label,
                            baseLabel = label,
                            componentName = info.componentName,
                            user = user,
                            icon = drawable
                                ?.let { runCatching { it.toBitmap(iconSizePx, iconSizePx) }.getOrNull() }
                                ?.asImageBitmap() ?: fallback,
                            monoIcon = adaptive?.monochrome?.let {
                                runCatching { it.toScaledBitmap(iconSizePx, MONO_SCALE) }.getOrNull()
                            }?.asImageBitmap(),
                            flatIcon = adaptive?.foreground?.let {
                                runCatching { it.toScaledBitmap(iconSizePx, MONO_SCALE) }.getOrNull()
                            }?.asImageBitmap(),
                        )
                    }.getOrNull()
                }
        }
        (apps + pinnedShortcuts(iconSizePx)).sortedBy { it.label.lowercase() }
    }

    /**
     * Shortcuts Gantry has pinned (accepted via PinShortcutActivity). The
     * system tracks pins per launcher, so no local persistence is needed;
     * querying throws while Gantry isn't the default launcher, and a profile
     * can be unavailable — either way that profile just contributes nothing.
     */
    private fun pinnedShortcuts(iconSizePx: Int): List<AppEntry> {
        val density = context.resources.displayMetrics.densityDpi
        return launcherApps.profiles.flatMap { user ->
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            runCatching { launcherApps.getShortcuts(query, user).orEmpty() }
                .getOrDefault(emptyList())
                .filter { it.isEnabled }
                .map { shortcut ->
                    val label = (shortcut.shortLabel ?: shortcut.longLabel
                        ?: shortcut.`package`).toString()
                    val drawable = runCatching {
                        launcherApps.getShortcutIconDrawable(shortcut, density)
                    }.getOrNull()
                    AppEntry(
                        label = label,
                        baseLabel = label,
                        componentName = ComponentName(shortcut.`package`, shortcut.id),
                        user = user,
                        icon = runCatching {
                            (drawable ?: ColorDrawable(Color.TRANSPARENT))
                                .toBitmap(iconSizePx, iconSizePx)
                        }.getOrElse { createBitmap(iconSizePx, iconSizePx) }.asImageBitmap(),
                        monoIcon = null,
                        shortcutId = shortcut.id,
                    )
                }
        }
    }

    /** The system's generic app icon, else a blank tile. */
    private fun placeholderIcon(sizePx: Int): ImageBitmap = runCatching {
        context.packageManager.defaultActivityIcon.toBitmap(sizePx, sizePx)
    }.getOrElse { createBitmap(sizePx, sizePx) }.asImageBitmap()

    /**
     * Monochrome adaptive-icon layers are drawn full-bleed on the 108dp
     * canvas, so rendered 1:1 the glyph looks tiny — scale up around center
     * like the system launcher does.
     */
    private fun Drawable.toScaledBitmap(sizePx: Int, scale: Float): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val inset = ((scale - 1f) / 2f * sizePx).toInt()
        setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
        draw(Canvas(bitmap))
        return bitmap
    }

    /** Emits whenever installed packages or pinned shortcuts change in any visible profile. */
    fun packageChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(
                packageNames: Array<out String>, user: UserHandle, replacing: Boolean
            ) { trySend(Unit) }
            override fun onPackagesUnavailable(
                packageNames: Array<out String>, user: UserHandle, replacing: Boolean
            ) { trySend(Unit) }
            override fun onShortcutsChanged(
                packageName: String, shortcuts: List<ShortcutInfo>, user: UserHandle
            ) { trySend(Unit) }
        }
        // registerCallback without a Handler requires a Looper thread; always
        // deliver on the main looper regardless of the collecting dispatcher.
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    fun launch(app: AppEntry, sourceBounds: Rect? = null) {
        if (app.shortcutId != null) {
            // Throws if the shortcut vanished or Gantry lost the launcher role.
            runCatching {
                launcherApps.startShortcut(
                    app.componentName.packageName, app.shortcutId, sourceBounds, null, app.user
                )
            }
        } else {
            // Throws if the app vanished mid-tap or the profile refuses the
            // start; the launcher itself must survive either way.
            runCatching {
                launcherApps.startMainActivity(app.componentName, app.user, sourceBounds, null)
            }
        }
    }

    /** Unpins a shortcut by re-pinning the package's remaining shortcut ids. */
    fun unpinShortcut(app: AppEntry) {
        val shortcutId = app.shortcutId ?: return
        val packageName = app.componentName.packageName
        runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                .setPackage(packageName)
            val remaining = launcherApps.getShortcuts(query, app.user).orEmpty()
                .map { it.id }
                .filter { it != shortcutId }
            launcherApps.pinShortcuts(packageName, remaining, app.user)
        }
    }

    /** System App-info screen; works across profiles. */
    fun openAppInfo(app: AppEntry) {
        runCatching {
            launcherApps.startAppDetailsActivity(app.componentName, app.user, null, null)
        }
    }

    private companion object {
        const val ICON_SIZE_DP = 48
        const val MONO_SCALE = 1.5f
    }
}
