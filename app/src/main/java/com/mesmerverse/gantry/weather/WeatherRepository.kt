package com.mesmerverse.gantry.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesmerverse.gantry.apps.dataStore
import com.mesmerverse.gantry.net.httpGetJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLEncoder
import kotlin.coroutines.resume
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherSettings(
    val enabled: Boolean = false,
    val place: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    /** Pull coordinates from the device (coarse) instead of a fixed place. */
    val useDeviceLocation: Boolean = false,
    /** Minutes between fetches; user-tunable from widget settings. */
    val refreshMinutes: Int = DEFAULT_REFRESH_MINUTES,
) {
    companion object {
        const val DEFAULT_REFRESH_MINUTES = 30
    }
}

data class GeoPlace(val label: String, val lat: Double, val lon: Double)

/** WMO weather-code group, rendered as a monochrome glyph in the UI. */
enum class WeatherCondition { Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Thunder, Unknown }

sealed interface WeatherUi {
    /** Disabled or no location configured — show the idle placeholder. */
    data object Hidden : WeatherUi

    /** Enabled and configured but nothing fetched yet (offline, first run). */
    data object Unavailable : WeatherUi

    data class Current(
        val temp: String,
        val condition: WeatherCondition,
        val stale: Boolean,
    ) : WeatherUi
}

/**
 * Current conditions from Open-Meteo (keyless, no account). Opt-in: nothing
 * is fetched unless the user enabled weather and picked a location. The last
 * reading is cached so the widget still shows something (marked stale) when
 * the network is unavailable or the Network permission is revoked.
 */
class WeatherRepository(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("weather_enabled")
    private val placeKey = stringPreferencesKey("weather_place")
    private val latKey = doublePreferencesKey("weather_lat")
    private val lonKey = doublePreferencesKey("weather_lon")
    private val useDeviceKey = booleanPreferencesKey("weather_use_device")
    private val refreshKey = intPreferencesKey("weather_refresh_minutes")
    private val cacheTextKey = stringPreferencesKey("weather_cache_text")
    private val cacheAtKey = longPreferencesKey("weather_cache_at")

    // Last device fix (already coarse), memory only — never written to disk.
    // Reused between refreshes so each one doesn't touch location.
    @Volatile
    private var lastFix: Pair<Pair<Double, Double>, Long>? = null

    val settings: Flow<WeatherSettings> = context.dataStore.data.map { prefs ->
        WeatherSettings(
            enabled = prefs[enabledKey] ?: false,
            place = prefs[placeKey],
            lat = prefs[latKey],
            lon = prefs[lonKey],
            useDeviceLocation = prefs[useDeviceKey] ?: false,
            refreshMinutes = prefs[refreshKey] ?: WeatherSettings.DEFAULT_REFRESH_MINUTES,
        )
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val weather: Flow<WeatherUi> = settings.flatMapLatest { s ->
        val configured = s.useDeviceLocation || (s.lat != null && s.lon != null)
        if (!s.enabled || !configured) {
            flowOf(WeatherUi.Hidden)
        } else {
            flow {
                val period = s.refreshMinutes * 60_000L
                val cached = cachedReading()
                val age = cached?.let { System.currentTimeMillis() - it.at }
                // Home resubscribes on every return; a reading still inside
                // its refresh period is current, so wait out the rest of the
                // period instead of refetching (and re-locating) each visit.
                if (cached != null && age != null && age in 0 until period) {
                    emit(WeatherUi.Current(cached.temp, cached.condition, stale = false))
                    delay(period - age)
                } else {
                    emit(cached?.let { WeatherUi.Current(it.temp, it.condition, stale = true) }
                        ?: WeatherUi.Unavailable)
                }
                while (true) {
                    val fresh = runCatching {
                        val coords = if (s.useDeviceLocation) {
                            deviceCoordinates(maxAgeMs = maxOf(period, LOCATION_REUSE_MS))
                        } else {
                            s.lat!! to s.lon!!
                        }
                        coords?.let { (lat, lon) -> fetchCurrent(lat, lon) }
                    }.getOrNull()
                    if (fresh != null) {
                        saveCache(fresh)
                        emit(WeatherUi.Current(fresh.temp, fresh.condition, stale = false))
                    } else {
                        emit(cachedReading()?.let { WeatherUi.Current(it.temp, it.condition, stale = true) }
                            ?: WeatherUi.Unavailable)
                    }
                    // Settings changes cancel this flow (flatMapLatest), so a
                    // new period takes effect — with a fresh fetch — at once.
                    delay(period)
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setRefreshMinutes(minutes: Int) {
        context.dataStore.edit { it[refreshKey] = minutes.coerceIn(5, 24 * 60) }
    }

    suspend fun setUseDeviceLocation(value: Boolean) {
        lastFix = null
        context.dataStore.edit {
            it[useDeviceKey] = value
            it.remove(cacheTextKey)
        }
    }

    /** Forgets the fixed place entirely. */
    suspend fun clearPlace() {
        context.dataStore.edit {
            it.remove(placeKey)
            it.remove(latKey)
            it.remove(lonKey)
            it.remove(cacheTextKey)
        }
    }

    /**
     * Coarse device position, already rounded by [coordinate] before any
     * request. A fix younger than [maxAgeMs] is reused without asking the
     * system again — each ask lights the location indicator. Null without
     * the permission or a resolvable location.
     */
    private suspend fun deviceCoordinates(maxAgeMs: Long): Pair<Double, Double>? {
        lastFix?.let { (coords, at) ->
            if (System.currentTimeMillis() - at in 0 until maxAgeMs) return coords
        }
        return locateDevice()?.also { lastFix = it to System.currentTimeMillis() }
    }

    private suspend fun locateDevice(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> return null
        }
        manager.getLastKnownLocation(provider)?.let { return it.latitude to it.longitude }
        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(
                    provider, signal, context.mainExecutor
                ) { location ->
                    continuation.resume(location?.let { it.latitude to it.longitude })
                }
            }
        }
    }

    suspend fun setPlace(place: GeoPlace) {
        context.dataStore.edit { prefs ->
            prefs[placeKey] = place.label
            prefs[latKey] = place.lat
            prefs[lonKey] = place.lon
            // Old reading is for the old place.
            prefs.remove(cacheTextKey)
        }
    }

    /** City search via Open-Meteo's geocoding API. */
    suspend fun geocode(query: String): List<GeoPlace> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=5&language=en&format=json"
        val results = httpGetJson(url).optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val place = results.getJSONObject(i)
            val label = listOfNotNull(
                place.getString("name"),
                place.optString("admin1").takeIf { it.isNotBlank() },
                place.optString("country_code").takeIf { it.isNotBlank() },
            ).joinToString(", ")
            GeoPlace(label, place.getDouble("latitude"), place.getDouble("longitude"))
        }
    }

    /** [at] is when it was fetched (0 for a fresh one not yet saved). */
    private data class CurrentReading(
        val temp: String,
        val condition: WeatherCondition,
        val at: Long = 0L,
    )

    private fun fetchCurrent(lat: Double, lon: Double): CurrentReading {
        val unit =
            if (Locale.getDefault().country in FAHRENHEIT_COUNTRIES) "fahrenheit" else "celsius"
        // Coordinates rounded to ~1km so the exact location never leaves the device.
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinate(lat)}&longitude=${coordinate(lon)}" +
            "&current=temperature_2m,weather_code&temperature_unit=$unit"
        val current = httpGetJson(url).getJSONObject("current")
        val temp = current.getDouble("temperature_2m").roundToInt()
        return CurrentReading("$temp°", conditionFor(current.getInt("weather_code")))
    }

    // Locale-independent formatting: some locales would emit "39,74".
    private fun coordinate(value: Double): String = String.format(Locale.US, "%.2f", value)

    // Cached as "temp°|CONDITION" so a stale reading still renders the glyph.
    private suspend fun cachedReading(): CurrentReading? {
        val prefs = context.dataStore.data.first()
        return prefs[cacheTextKey]?.let { cached ->
            val parts = cached.split("|", limit = 2)
            val condition = parts.getOrNull(1)
                ?.let { runCatching { WeatherCondition.valueOf(it) }.getOrNull() }
                ?: WeatherCondition.Unknown
            CurrentReading(parts[0], condition, at = prefs[cacheAtKey] ?: 0L)
        }
    }

    private suspend fun saveCache(reading: CurrentReading) {
        context.dataStore.edit {
            it[cacheTextKey] = "${reading.temp}|${reading.condition.name}"
            it[cacheAtKey] = System.currentTimeMillis()
        }
    }

    /** WMO weather code groups. */
    private fun conditionFor(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.Clear
        1, 2 -> WeatherCondition.PartlyCloudy
        3 -> WeatherCondition.Cloudy
        45, 48 -> WeatherCondition.Fog
        in 51..57 -> WeatherCondition.Drizzle
        in 61..67, in 80..82 -> WeatherCondition.Rain
        in 71..77, 85, 86 -> WeatherCondition.Snow
        in 95..99 -> WeatherCondition.Thunder
        else -> WeatherCondition.Unknown
    }

    private companion object {
        /** Minimum time a device fix is reused before locating again. */
        const val LOCATION_REUSE_MS = 60 * 60_000L

        val FAHRENHEIT_COUNTRIES = setOf("US", "BS", "BZ", "KY", "PW", "LR", "MM")
    }
}
