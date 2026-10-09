package com.mesmerverse.gantry.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mesmerverse.gantry.apps.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** How Gantry picks its text color relative to the background. */
enum class TextMode { Auto, Light, Dark, Custom }

/** Which notifications the read-aloud service speaks. */
enum class ReadAloudScope { MessagesOnly, All }

/** General launcher preferences: layout and appearance. */
class SettingsRepository(private val context: Context) {

    private val leftHandedKey = booleanPreferencesKey("left_handed")
    private val textModeKey = stringPreferencesKey("text_mode")
    private val customTextColorKey = intPreferencesKey("custom_text_color")
    private val fontScaleKey = floatPreferencesKey("font_scale")
    private val fontIdKey = stringPreferencesKey("font_id")
    // 0 = no custom image; >0 = custom image (bumped to invalidate caches).
    private val backgroundVersionKey = intPreferencesKey("background_version")
    private val backgroundColorKey = intPreferencesKey("background_color")
    private val accentColorKey = intPreferencesKey("accent_color")
    private val clockFormatKey = stringPreferencesKey("clock_format")
    private val showStatusBarKey = booleanPreferencesKey("show_status_bar")
    private val notificationPreviewsKey = booleanPreferencesKey("notification_previews")
    private val notificationPanelTopKey = booleanPreferencesKey("notification_panel_top")
    private val showSealedAirlockKey = booleanPreferencesKey("show_sealed_airlock")
    private val airlockFocusKey = booleanPreferencesKey("airlock_focus")
    private val iconPackKey = stringPreferencesKey("icon_pack")
    private val gestureLeftKey = stringPreferencesKey("gesture_left")
    private val gestureRightKey = stringPreferencesKey("gesture_right")
    private val dockRightKey = stringPreferencesKey("dock_right_key")
    private val readAloudEnabledKey = booleanPreferencesKey("read_aloud_enabled")
    private val tourSeenKey = booleanPreferencesKey("tour_seen")
    private val readAloudScopeKey = stringPreferencesKey("read_aloud_scope")
    private val autoArmDevicesKey = stringSetPreferencesKey("read_aloud_auto_arm_devices")
    private val autoArmedKey = booleanPreferencesKey("read_aloud_auto_armed")

    /** Mirrors handed-ness sensitive UI (alphabet rail side). */
    val leftHanded: Flow<Boolean> =
        context.dataStore.data.map { it[leftHandedKey] ?: false }.distinctUntilChanged()

    val textMode: Flow<TextMode> = context.dataStore.data.map { prefs ->
        runCatching { TextMode.valueOf(prefs[textModeKey] ?: "") }.getOrDefault(TextMode.Auto)
    }.distinctUntilChanged()

    /** ARGB text color used when [textMode] is Custom. */
    val customTextColor: Flow<Int?> =
        context.dataStore.data.map { it[customTextColorKey] }.distinctUntilChanged()

    val fontScale: Flow<Float> =
        context.dataStore.data.map { it[fontScaleKey] ?: 1f }.distinctUntilChanged()

    val fontId: Flow<String> =
        context.dataStore.data.map { it[fontIdKey] ?: "space_grotesk" }.distinctUntilChanged()

    val backgroundVersion: Flow<Int> =
        context.dataStore.data.map { it[backgroundVersionKey] ?: 0 }.distinctUntilChanged()

    /** Solid background color (ARGB); null = image or system wallpaper. */
    val backgroundColor: Flow<Int?> =
        context.dataStore.data.map { it[backgroundColorKey] }.distinctUntilChanged()

    /** ARGB highlight color for airlock/exposure accents; null = default. */
    val accentColor: Flow<Int?> =
        context.dataStore.data.map { it[accentColorKey] }.distinctUntilChanged()

    /** Show the system status bar instead of hiding it. */
    val showStatusBar: Flow<Boolean> =
        context.dataStore.data.map { it[showStatusBarKey] ?: false }.distinctUntilChanged()

    /** "system" (follow Android's setting), "12", or "24". */
    val clockFormat: Flow<String> =
        context.dataStore.data.map { it[clockFormatKey] ?: "system" }.distinctUntilChanged()

    suspend fun setClockFormat(format: String) {
        context.dataStore.edit { it[clockFormatKey] = format }
    }

    suspend fun setAccentColor(argb: Int?) {
        context.dataStore.edit { prefs ->
            if (argb == null) prefs.remove(accentColorKey) else prefs[accentColorKey] = argb
        }
    }

    suspend fun setShowStatusBar(value: Boolean) {
        context.dataStore.edit { it[showStatusBarKey] = value }
    }

    /** Whether the guided tour was ever completed or skipped (it self-starts until then). */
    val tourSeen: Flow<Boolean> =
        context.dataStore.data.map { it[tourSeenKey] ?: false }.distinctUntilChanged()

    suspend fun setTourSeen() {
        context.dataStore.edit { it[tourSeenKey] = true }
    }

    /** Show one-line notification text under app names (dots stay either way). */
    /** Notification list opens from the top of the screen instead of above the dock. */
    val notificationPanelTop: Flow<Boolean> =
        context.dataStore.data.map { it[notificationPanelTopKey] ?: false }.distinctUntilChanged()

    suspend fun setNotificationPanelTop(value: Boolean) {
        context.dataStore.edit { it[notificationPanelTopKey] = value }
    }

    val notificationPreviews: Flow<Boolean> =
        context.dataStore.data.map { it[notificationPreviewsKey] ?: true }.distinctUntilChanged()

    suspend fun setNotificationPreviews(value: Boolean) {
        context.dataStore.edit { it[notificationPreviewsKey] = value }
    }

    /** Show airlock apps while sealed (tap unlocks-and-launches) or hide them. */
    val showSealedAirlockApps: Flow<Boolean> =
        context.dataStore.data.map { it[showSealedAirlockKey] ?: false }.distinctUntilChanged()

    suspend fun setShowSealedAirlockApps(value: Boolean) {
        context.dataStore.edit { it[showSealedAirlockKey] = value }
    }

    /**
     * Airlock focus: hide every non-airlock app while the airlock is open —
     * the open airlock shows only its own apps, a reminder to seal it again
     * before reaching for anything else. Whether sealed airlock apps are
     * listed is [showSealedAirlockApps]'s call alone.
     */
    val airlockFocus: Flow<Boolean> =
        context.dataStore.data.map { it[airlockFocusKey] ?: false }.distinctUntilChanged()

    suspend fun setAirlockFocus(value: Boolean) {
        context.dataStore.edit { it[airlockFocusKey] = value }
    }

    /**
     * Read-aloud "armed" state — a per-trip toggle. On means the notification
     * listener speaks newly-arriving messages aloud (see the read-aloud glyph).
     * Not auto-cleared, so it persists across the launcher's UI restarts; the
     * user turns it off explicitly.
     */
    val readAloudEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[readAloudEnabledKey] ?: false }.distinctUntilChanged()

    suspend fun setReadAloudEnabled(value: Boolean) {
        context.dataStore.edit {
            it[readAloudEnabledKey] = value
            // A manual change takes ownership back from Bluetooth auto-arm.
            it[autoArmedKey] = false
        }
    }

    /** Arms/disarms on behalf of a Bluetooth auto-arm device, remembering why. */
    suspend fun setReadAloudAutoArmed(value: Boolean) {
        context.dataStore.edit {
            it[readAloudEnabledKey] = value
            it[autoArmedKey] = value
        }
    }

    /** True while read-aloud is on only because an auto-arm device connected. */
    suspend fun isReadAloudAutoArmed(): Boolean =
        context.dataStore.data.first()[autoArmedKey] ?: false

    val readAloudScope: Flow<ReadAloudScope> = context.dataStore.data.map { prefs ->
        runCatching { ReadAloudScope.valueOf(prefs[readAloudScopeKey] ?: "") }
            .getOrDefault(ReadAloudScope.MessagesOnly)
    }.distinctUntilChanged()

    suspend fun setReadAloudScope(scope: ReadAloudScope) {
        context.dataStore.edit { it[readAloudScopeKey] = scope.name }
    }

    /**
     * Bluetooth device addresses that auto-arm read-aloud on connect (and
     * disarm on disconnect) — e.g. the car's stereo. Empty = manual only.
     */
    val autoArmDevices: Flow<Set<String>> =
        context.dataStore.data.map { it[autoArmDevicesKey] ?: emptySet() }.distinctUntilChanged()

    suspend fun setAutoArmDevice(address: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[autoArmDevicesKey] ?: emptySet()
            prefs[autoArmDevicesKey] = if (enabled) current + address else current - address
        }
    }

    /** Package name of the global icon pack; null = system icons. */
    val iconPack: Flow<String?> =
        context.dataStore.data.map { it[iconPackKey] }.distinctUntilChanged()

    suspend fun setIconPack(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(iconPackKey)
            else prefs[iconPackKey] = packageName
        }
    }

    // Swipe actions: "camera", "settings", "none", or "app:<appKey>".
    val gestureLeft: Flow<String> =
        context.dataStore.data.map { it[gestureLeftKey] ?: "camera" }.distinctUntilChanged()

    val gestureRight: Flow<String> =
        context.dataStore.data.map { it[gestureRightKey] ?: "settings" }.distinctUntilChanged()

    suspend fun setGesture(left: Boolean, action: String) {
        context.dataStore.edit {
            it[if (left) gestureLeftKey else gestureRightKey] = action
        }
    }

    // Which dock pin sits right of the rocket; unset falls back to dock order
    // (first pin left, second right).
    val dockRightApp: Flow<String?> =
        context.dataStore.data.map { it[dockRightKey] }.distinctUntilChanged()

    suspend fun setDockRightApp(key: String?) {
        context.dataStore.edit {
            if (key == null) it.remove(dockRightKey) else it[dockRightKey] = key
        }
    }

    suspend fun setLeftHanded(value: Boolean) {
        context.dataStore.edit { it[leftHandedKey] = value }
    }

    suspend fun setTextMode(mode: TextMode) {
        context.dataStore.edit { it[textModeKey] = mode.name }
    }

    suspend fun setCustomTextColor(argb: Int) {
        context.dataStore.edit {
            it[customTextColorKey] = argb
            it[textModeKey] = TextMode.Custom.name
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[fontScaleKey] = scale }
    }

    suspend fun setFontId(id: String) {
        context.dataStore.edit { it[fontIdKey] = id }
    }

    /** A custom image replaces any solid color. */
    suspend fun bumpBackgroundVersion() {
        context.dataStore.edit {
            it[backgroundVersionKey] = (it[backgroundVersionKey] ?: 0) + 1
            it.remove(backgroundColorKey)
        }
    }

    /** A solid color replaces any custom image. */
    suspend fun setBackgroundColor(argb: Int) {
        context.dataStore.edit {
            it[backgroundColorKey] = argb
            it[backgroundVersionKey] = 0
        }
    }

    /** Back to the system wallpaper. */
    suspend fun clearBackground() {
        context.dataStore.edit {
            it[backgroundVersionKey] = 0
            it.remove(backgroundColorKey)
        }
    }
}
