package com.mesmerverse.gantry.apps

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppTag(
    val name: String,
    val appKeys: Set<String>,
    /** User-chosen display order (app keys); members not listed sort after. */
    val order: List<String> = emptyList(),
    /** ARGB tint for the folder crate icon; null = accent color. */
    val folderColor: Int? = null,
)

/**
 * Members of [tag] in display order: explicitly ordered keys first, then the
 * rest in [apps]'s (alphabetical) order.
 */
fun orderedApps(apps: List<AppEntry>, tag: AppTag): List<AppEntry> {
    val members = apps.filter { it.key in tag.appKeys }
    if (tag.order.isEmpty()) return members
    val position = tag.order.withIndex().associate { (index, key) -> key to index }
    return members.sortedBy { position[it.key] ?: Int.MAX_VALUE }
}

/**
 * Tags are Gantry's single app-organization model. "Home", "Dock", and
 * "Corner" are reserved tags that drive the home screen list, the floating
 * dock, and the corner shortcut; user tags group apps in the drawer.
 */
class TagsRepository(private val context: Context) {

    private val tagNamesKey = stringSetPreferencesKey("tag_names")
    private val homeFoldersKey = stringSetPreferencesKey("home_folders")
    private val airlockHomeFoldersKey = stringSetPreferencesKey("airlock_home_folders")
    private val migratedKey = booleanPreferencesKey("tags_migrated")

    private fun appsKey(tag: String): Preferences.Key<Set<String>> =
        stringSetPreferencesKey("tag_apps_$tag")

    private fun orderKey(tag: String): Preferences.Key<String> =
        stringPreferencesKey("tag_order_$tag")

    private fun colorKey(tag: String): Preferences.Key<Int> =
        intPreferencesKey("folder_color_$tag")

    /** Reserved tags first, then user tags alphabetically. */
    val tags: Flow<List<AppTag>> = context.dataStore.data.map { prefs ->
        val custom = (prefs[tagNamesKey] ?: emptySet()).sortedBy { it.lowercase() }
        (RESERVED + custom).map { name ->
            AppTag(
                name = name,
                appKeys = prefs[appsKey(name)] ?: emptySet(),
                order = prefs[orderKey(name)]?.split("\n")?.filter { it.isNotEmpty() }
                    ?: emptyList(),
                folderColor = prefs[colorKey(name)],
            )
        }
    }

    /** Custom tags shown on the home screen as folders. */
    val homeFolders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[homeFoldersKey] ?: emptySet()
    }

    suspend fun toggleHomeFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[homeFoldersKey] ?: emptySet()
            prefs[homeFoldersKey] = if (name in current) current - name else current + name
        }
    }

    /**
     * Tags shown as folders on the Airlock home screen — its own set, so the
     * open airlock (with "Airlock home screen" on) shows only folders chosen
     * for it, not the normal profile's.
     */
    val airlockHomeFolders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[airlockHomeFoldersKey] ?: emptySet()
    }

    suspend fun toggleAirlockHomeFolder(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[airlockHomeFoldersKey] ?: emptySet()
            prefs[airlockHomeFoldersKey] = if (name in current) current - name else current + name
        }
    }

    suspend fun setFolderColor(name: String, argb: Int?) {
        context.dataStore.edit { prefs ->
            if (argb == null) prefs.remove(colorKey(name)) else prefs[colorKey(name)] = argb
        }
    }

    /** Persists the full display order for a tag. */
    suspend fun setOrder(tag: String, orderedKeys: List<String>) {
        context.dataStore.edit { it[orderKey(tag)] = orderedKeys.joinToString("\n") }
    }

    suspend fun addTag(name: String, initialAppKey: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val existing = (prefs[tagNamesKey] ?: emptySet()) + RESERVED
            if (existing.none { it.equals(trimmed, ignoreCase = true) }) {
                prefs[tagNamesKey] = (prefs[tagNamesKey] ?: emptySet()) + trimmed
            }
            // Resolve to the canonical casing if the tag already existed.
            val canonical = existing.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
            if (initialAppKey != null) {
                prefs[appsKey(canonical)] = (prefs[appsKey(canonical)] ?: emptySet()) + initialAppKey
            }
        }
    }

    /** Reserved tags cannot be renamed; no-op if the new name collides. */
    suspend fun renameTag(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || oldName in RESERVED) return
        context.dataStore.edit { prefs ->
            val names = prefs[tagNamesKey] ?: emptySet()
            if (oldName !in names) return@edit
            if ((names - oldName + RESERVED).any { it.equals(trimmed, ignoreCase = true) }) return@edit
            prefs[tagNamesKey] = names - oldName + trimmed
            prefs[appsKey(trimmed)] = prefs[appsKey(oldName)] ?: emptySet()
            prefs.remove(appsKey(oldName))
            prefs[orderKey(oldName)]?.let { prefs[orderKey(trimmed)] = it }
            prefs.remove(orderKey(oldName))
            val folders = prefs[homeFoldersKey] ?: emptySet()
            if (oldName in folders) prefs[homeFoldersKey] = folders - oldName + trimmed
            val airlockFolders = prefs[airlockHomeFoldersKey] ?: emptySet()
            if (oldName in airlockFolders) {
                prefs[airlockHomeFoldersKey] = airlockFolders - oldName + trimmed
            }
            prefs[colorKey(oldName)]?.let { prefs[colorKey(trimmed)] = it }
            prefs.remove(colorKey(oldName))
        }
    }

    /** Reserved tags cannot be deleted. Apps themselves are unaffected. */
    suspend fun deleteTag(name: String) {
        if (name in RESERVED) return
        context.dataStore.edit { prefs ->
            prefs[tagNamesKey] = (prefs[tagNamesKey] ?: emptySet()) - name
            prefs.remove(appsKey(name))
            prefs.remove(orderKey(name))
            prefs[homeFoldersKey] = (prefs[homeFoldersKey] ?: emptySet()) - name
            prefs[airlockHomeFoldersKey] = (prefs[airlockHomeFoldersKey] ?: emptySet()) - name
            prefs.remove(colorKey(name))
        }
    }

    suspend fun toggleApp(tag: String, appKey: String) {
        context.dataStore.edit { prefs ->
            val key = appsKey(tag)
            val current = prefs[key] ?: emptySet()
            prefs[key] = when {
                appKey in current -> current - appKey
                // The corner holds exactly one app: tagging a new one
                // replaces the old.
                tag == CORNER -> setOf(appKey)
                else -> current + appKey
            }
        }
    }

    /**
     * One-time: migrate the pre-tag home/dock pin sets into the reserved tags
     * and seed an empty dock with sensible defaults (phone + messages).
     */
    suspend fun migrateAndSeed(defaultDockKeys: () -> Set<String>) {
        context.dataStore.edit { prefs ->
            if (prefs[migratedKey] == true) return@edit
            prefs[migratedKey] = true
            val oldHome = stringSetPreferencesKey("home_apps")
            val oldDock = stringSetPreferencesKey("dock_apps")
            prefs[appsKey(HOME)] = (prefs[appsKey(HOME)] ?: emptySet()) + (prefs[oldHome] ?: emptySet())
            val dock = (prefs[appsKey(DOCK)] ?: emptySet()) + (prefs[oldDock] ?: emptySet())
            prefs[appsKey(DOCK)] = dock.ifEmpty { defaultDockKeys() }
            prefs.remove(oldHome)
            prefs.remove(oldDock)
        }
    }

    companion object {
        const val HOME = "Home"
        const val DOCK = "Dock"

        /** The lone critical-app shortcut beside the dock. */
        const val CORNER = "Corner"
        private val RESERVED = listOf(HOME, DOCK, CORNER)
    }
}
