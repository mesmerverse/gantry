package com.mesmerverse.gantry.net

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal fun httpGetJson(url: String): JSONObject = JSONObject(httpGetText(url))

// Every legitimate response Gantry handles (Kraken ticker, Open-Meteo
// current weather, geocoding) is a few KB; 64KB is ~20x headroom while
// keeping a hostile endpoint's worst case trivial.
private const val MAX_RESPONSE_BYTES = 64 * 1024

internal fun httpGetText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "Gantry-Launcher")
        connection.inputStream.use { input ->
            // Bound reads: a hostile/misconfigured endpoint (custom ticker
            // URLs are user-supplied) must not be able to exhaust memory.
            val bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1)
            require(bytes.size <= MAX_RESPONSE_BYTES) { "response exceeds ${MAX_RESPONSE_BYTES}B" }
            bytes.decodeToString()
        }
    } finally {
        connection.disconnect()
    }
}
