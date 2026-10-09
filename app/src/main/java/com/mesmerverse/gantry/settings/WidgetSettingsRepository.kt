package com.mesmerverse.gantry.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesmerverse.gantry.apps.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Home-screen components the user can enable and configure. */
enum class WidgetId(val label: String) {
    Date("Date"),
    Clock("Clock"),
    Weather("Weather"),
    Battery("Battery"),
    Ticker("Price ticker"),
    Media("Media"),
    Notifications("Notifications"),
    ReadAloud("Read aloud"),
    // Enum name kept for its stored preference keys; it covers any VPN now.
    Tailnet("VPN"),
}

data class WidgetPrefs(
    val enabled: Boolean,
    /** App launched when the widget is tapped; null = the default action. */
    val tapAppKey: String?,
)

class WidgetSettingsRepository(private val context: Context) {

    // Weather and the ticker reuse their pre-existing opt-in keys so nothing
    // migrates; everything else is on by default.
    private fun enabledKey(id: WidgetId) = booleanPreferencesKey(
        when (id) {
            WidgetId.Weather -> "weather_enabled"
            WidgetId.Ticker -> "btc_enabled"
            else -> "widget_${id.name}_on"
        }
    )

    private fun tapAppKey(id: WidgetId) = stringPreferencesKey("widget_${id.name}_app")

    val widgets: Flow<Map<WidgetId, WidgetPrefs>> = context.dataStore.data.map { prefs ->
        WidgetId.entries.associateWith { id ->
            WidgetPrefs(
                enabled = prefs[enabledKey(id)] ?: when (id) {
                    WidgetId.Weather, WidgetId.Ticker, WidgetId.ReadAloud -> false
                    else -> true
                },
                tapAppKey = prefs[tapAppKey(id)],
            )
        }
    }.distinctUntilChanged()

    /** Just the read-aloud widget's enabled flag — the feature's master switch. */
    val readAloudWidgetEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[enabledKey(WidgetId.ReadAloud)] ?: false
    }.distinctUntilChanged()

    suspend fun setEnabled(id: WidgetId, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey(id)] = enabled }
    }

    suspend fun setTapApp(id: WidgetId, appKey: String?) {
        context.dataStore.edit { prefs ->
            val key = tapAppKey(id)
            if (appKey == null) prefs.remove(key) else prefs[key] = appKey
        }
    }
}
