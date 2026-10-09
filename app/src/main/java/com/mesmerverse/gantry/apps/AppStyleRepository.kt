package com.mesmerverse.gantry.apps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class IconStyle { Default, Monochrome, Flat, Letter }

data class AppStyle(
    val label: String? = null,
    val icon: IconStyle = IconStyle.Default,
    /**
     * Where the base icon bitmap comes from: null = automatic (global icon
     * pack when one is set, else system), [IconPackRepository.SOURCE_SYSTEM]
     * = force the system icon, or "pack:<package>:<drawable>" for a manual
     * pick from an icon pack.
     */
    val iconSource: String? = null,
)

/** Per-app display overrides: custom name, icon style, and icon source. */
class AppStyleRepository(private val context: Context) {

    val styles: Flow<Map<String, AppStyle>> = context.dataStore.data.map { prefs ->
        val result = mutableMapOf<String, AppStyle>()
        prefs.asMap().forEach { (key, value) ->
            when {
                key.name.startsWith(LABEL_PREFIX) -> {
                    val appKey = key.name.removePrefix(LABEL_PREFIX)
                    result[appKey] = (result[appKey] ?: AppStyle()).copy(label = value as String)
                }
                key.name.startsWith(ICON_SOURCE_PREFIX) -> {
                    val appKey = key.name.removePrefix(ICON_SOURCE_PREFIX)
                    result[appKey] =
                        (result[appKey] ?: AppStyle()).copy(iconSource = value as String)
                }
                key.name.startsWith(ICON_PREFIX) -> {
                    val appKey = key.name.removePrefix(ICON_PREFIX)
                    val style = runCatching { IconStyle.valueOf(value as String) }
                        .getOrDefault(IconStyle.Default)
                    result[appKey] = (result[appKey] ?: AppStyle()).copy(icon = style)
                }
            }
        }
        result
    }

    /** Null or blank restores the app's original label. */
    suspend fun setLabel(appKey: String, label: String?) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey(LABEL_PREFIX + appKey)
            if (label.isNullOrBlank()) prefs.remove(key) else prefs[key] = label.trim()
        }
    }

    suspend fun setIcon(appKey: String, style: IconStyle) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey(ICON_PREFIX + appKey)
            if (style == IconStyle.Default) prefs.remove(key) else prefs[key] = style.name
        }
    }

    /** Null restores automatic resolution (global pack, else system icon). */
    suspend fun setIconSource(appKey: String, source: String?) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey(ICON_SOURCE_PREFIX + appKey)
            if (source == null) prefs.remove(key) else prefs[key] = source
        }
    }

    private companion object {
        const val LABEL_PREFIX = "app_label_"
        const val ICON_PREFIX = "app_icon_"
        const val ICON_SOURCE_PREFIX = "app_iconsrc_"
    }
}
