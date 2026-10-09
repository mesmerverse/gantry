package com.mesmerverse.gantry.btc

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesmerverse.gantry.apps.dataStore
import com.mesmerverse.gantry.net.httpGetJson
import com.mesmerverse.gantry.net.httpGetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import kotlin.math.roundToInt

sealed interface BtcUi {
    data object Hidden : BtcUi
    data object Unavailable : BtcUi
    data class Current(val text: String, val stale: Boolean) : BtcUi
}

data class TickerSettings(
    val asset: String = "BTC",
    val currency: String = "USD",
    /** Custom mode: fetch any HTTPS JSON endpoint instead of Kraken. */
    val custom: Boolean = false,
    val customUrl: String = "",
    val customPath: String = "",
    val customLabel: String = "",
    val customPrefix: String = "",
    /** Minutes between fetches; user-tunable from widget settings. */
    val refreshMinutes: Int = DEFAULT_REFRESH_MINUTES,
) {
    companion object {
        const val DEFAULT_REFRESH_MINUTES = 15
    }
}

/**
 * Reads a scalar out of a JSON document at a dot/bracket path, e.g.
 * "bitcoin.usd" or "prices[0].value". Null when the path misses or lands on
 * a non-scalar.
 */
internal fun extractJsonValue(json: String, path: String): String? {
    if (path.isBlank()) return null
    var current: Any = runCatching { org.json.JSONObject(json) }.getOrNull()
        ?: runCatching { org.json.JSONArray(json) }.getOrNull()
        ?: return null
    for (segment in path.split('.')) {
        val name = segment.substringBefore('[')
        if (name.isNotEmpty()) {
            current = (current as? org.json.JSONObject)?.opt(name) ?: return null
        }
        var rest = segment.substringAfter('[', "")
        while (rest.isNotEmpty()) {
            val index = rest.substringBefore(']').toIntOrNull() ?: return null
            val array = current as? org.json.JSONArray ?: return null
            current = array.opt(index) ?: return null
            rest = rest.substringAfter(']').removePrefix("[")
        }
    }
    return when (current) {
        is org.json.JSONObject, is org.json.JSONArray -> null
        else -> current.toString()
    }
}

/**
 * Spot price for a user-chosen asset pair from Kraken's public ticker
 * (keyless). Opt-in, cached, and dimmed-stale offline — same contract as the
 * weather widget.
 */
class BtcRepository(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("btc_enabled")
    private val assetKey = stringPreferencesKey("btc_asset")
    private val currencyKey = stringPreferencesKey("btc_currency")
    private val customKey = booleanPreferencesKey("btc_custom")
    private val customUrlKey = stringPreferencesKey("btc_custom_url")
    private val customPathKey = stringPreferencesKey("btc_custom_path")
    private val customLabelKey = stringPreferencesKey("btc_custom_label")
    private val customPrefixKey = stringPreferencesKey("btc_custom_prefix")
    private val refreshKey = intPreferencesKey("btc_refresh_minutes")
    private val cacheTextKey = stringPreferencesKey("btc_cache_text")

    val enabled: Flow<Boolean> =
        context.dataStore.data.map { it[enabledKey] ?: false }.distinctUntilChanged()

    val settings: Flow<TickerSettings> = context.dataStore.data.map { prefs ->
        TickerSettings(
            asset = prefs[assetKey] ?: "BTC",
            currency = prefs[currencyKey] ?: "USD",
            custom = prefs[customKey] ?: false,
            customUrl = prefs[customUrlKey] ?: "",
            customPath = prefs[customPathKey] ?: "",
            customLabel = prefs[customLabelKey] ?: "",
            customPrefix = prefs[customPrefixKey] ?: "",
            refreshMinutes = prefs[refreshKey] ?: TickerSettings.DEFAULT_REFRESH_MINUTES,
        )
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val price: Flow<BtcUi> =
        combine(enabled, settings) { on, s -> on to s }
            .distinctUntilChanged()
            .flatMapLatest { (on, s) ->
                if (!on) {
                    flowOf(BtcUi.Hidden)
                } else {
                    flow {
                        emit(cachedText()?.let { BtcUi.Current(it, stale = true) }
                            ?: BtcUi.Unavailable)
                        // Also debounces rapid asset edits (flatMapLatest cancels).
                        delay(600)
                        while (true) {
                            val fresh = runCatching { fetchPrice(s) }.getOrNull()
                            if (fresh != null) {
                                saveCache(fresh)
                                emit(BtcUi.Current(fresh, stale = false))
                            } else {
                                emit(cachedText()?.let { BtcUi.Current(it, stale = true) }
                                    ?: BtcUi.Unavailable)
                            }
                            // Settings changes cancel this flow, so a new
                            // period takes effect — with a fresh fetch — at once.
                            delay(s.refreshMinutes * 60_000L)
                        }
                    }.flowOn(Dispatchers.IO)
                }
            }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    // Unlike the value settings, no cache clear: the shown price is still
    // for the same pair, we're only changing how often it's refetched.
    suspend fun setRefreshMinutes(minutes: Int) {
        context.dataStore.edit { it[refreshKey] = minutes.coerceIn(5, 24 * 60) }
    }

    // Asset/currency go into the Kraken URL; restrict to what a pair code
    // can contain so stored values can never smuggle URL syntax.
    private fun sanitizePairCode(value: String): String =
        value.trim().uppercase().filter { it.isLetterOrDigit() }.take(10)

    suspend fun setAsset(asset: String) {
        context.dataStore.edit {
            it[assetKey] = sanitizePairCode(asset)
            it.remove(cacheTextKey)
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit {
            it[currencyKey] = sanitizePairCode(currency)
            it.remove(cacheTextKey)
        }
    }

    suspend fun setCustomEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[customKey] = enabled
            it.remove(cacheTextKey)
        }
    }

    suspend fun setCustomField(field: String, value: String) {
        context.dataStore.edit {
            val key = when (field) {
                "url" -> customUrlKey
                "path" -> customPathKey
                "label" -> customLabelKey
                else -> customPrefixKey
            }
            it[key] = value.trim()
            it.remove(cacheTextKey)
        }
    }

    private fun fetchPrice(settings: TickerSettings): String =
        if (settings.custom) fetchCustom(settings) else fetchKraken(settings)

    private fun fetchKraken(settings: TickerSettings): String {
        // Defense in depth for values persisted before sanitization existed.
        val pair = sanitizePairCode(krakenCode(settings.asset) + settings.currency)
        val result = httpGetJson("https://api.kraken.com/0/public/Ticker?pair=$pair")
            .getJSONObject("result")
        val pairData = result.getJSONObject(result.keys().next())
        // "c" = last trade closed: [price, lot volume]
        val price = pairData.getJSONArray("c").getString(0).toDouble()
        return "${assetSymbol(settings.asset)}${formatNumber(price)}"
    }

    /** Fokus+-style endpoint value: any HTTPS JSON URL + dot/bracket path. */
    private fun fetchCustom(settings: TickerSettings): String {
        require(settings.customUrl.startsWith("https://", ignoreCase = true)) {
            "custom ticker requires an https URL"
        }
        val raw = extractJsonValue(httpGetText(settings.customUrl), settings.customPath)
            ?: error("path did not resolve")
        // Untrusted network response: strip control chars and cap length.
        val sanitized = raw.filterNot { it.isISOControl() }.trim().take(32)
        val body = sanitized.toDoubleOrNull()?.let { formatNumber(it) } ?: sanitized
        val label = settings.customLabel.takeIf { it.isNotBlank() }?.let { "$it " } ?: ""
        return "$label${settings.customPrefix}$body"
    }

    private fun formatNumber(value: Double): String =
        if (value >= 100) NumberFormat.getIntegerInstance().format(value.roundToInt())
        else NumberFormat.getInstance().also { it.maximumFractionDigits = 4 }.format(value)

    private fun krakenCode(asset: String) = when (asset) {
        "BTC" -> "XBT"
        "DOGE" -> "XDG"
        else -> asset
    }

    private fun assetSymbol(asset: String) = when (asset) {
        "BTC" -> "₿ "
        "ETH" -> "Ξ "
        "XMR" -> "ɱ "
        else -> "$asset "
    }

    private suspend fun cachedText(): String? = context.dataStore.data.first()[cacheTextKey]

    private suspend fun saveCache(text: String) {
        context.dataStore.edit { it[cacheTextKey] = text }
    }
}
