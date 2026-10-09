package com.mesmerverse.gantry.apps

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

// Single app-wide preferences file; a second preferencesDataStore delegate on
// the same name would throw, so every repository shares this one. A corrupt
// file resets to defaults instead of failing every read — for the home app
// that would mean crashing at every start.
internal val Context.dataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
