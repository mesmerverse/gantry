package com.mesmerverse.gantry.tailnet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesmerverse.gantry.apps.dataStore
import com.mesmerverse.gantry.media.MediaNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

enum class TailnetStatus {
    /** Nothing to watch (no network service). */
    Unavailable,
    /** No VPN connected. */
    Off,
    /** A VPN other than Tailscale is connected; there's no tailnet to check. */
    Connected,
    /** No working internet under the VPN; not the tailnet's fault. */
    Offline,
    Checking,
    Healthy,
    /** VPN up, internet fine, but the check host or DNS stopped answering. */
    Down,
    Fixing,
}

data class TailnetState(
    val status: TailnetStatus = TailnetStatus.Checking,
    /** The app that owns the connected VPN, when Gantry can tell; null = none or unknown. */
    val vpnPackage: String? = null,
    /** When Gantry last restarted Tailscale successfully; null = never. */
    val lastFixAt: Long? = null,
    val lastFixAuto: Boolean = false,
)

data class TailnetSettings(
    /**
     * A tailnet-only host that is always up (e.g. the headscale server).
     * Blank (the default) means no health check: nothing is contacted.
     */
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    /** Restart Tailscale on its own when the tailnet hangs. */
    val autoFix: Boolean = false,
    /**
     * VPN app a tap connects when none is connected, or [LAST_USED] for
     * whichever was connected most recently.
     */
    val preferred: String = LAST_USED,
    /** The VPN app most recently seen connected; null until one is. */
    val lastUsed: String? = null,
) {
    /** The app a tap on "no VPN" brings back. */
    val connectTarget: String
        get() = if (preferred == LAST_USED) lastUsed ?: TailnetMonitor.TAILSCALE else preferred

    companion object {
        const val LAST_USED = "last_used"
        // No host until the user enters one: the health check is opt-in, so a
        // fresh install makes no connections. (Headscale's first node,
        // 100.64.0.1, is the usual "always on" box to point it at.)
        const val DEFAULT_HOST = ""
        const val DEFAULT_PORT = 22
    }
}

/**
 * Watches the phone's VPN: which app has it connected, and — when that's
 * Tailscale — whether the tailnet actually carries traffic, not just whether
 * Tailscale's VPN is up: a hung Tailscale keeps the VPN (and its key icon)
 * while nothing gets through. Fixing is what the user does by hand —
 * disconnect, then connect — through Tailscale's own automation receiver.
 *
 * One process-wide loop, running while anyone watches: the home screen
 * checks every minute while visible; with auto-fix on, the notification
 * listener keeps a slower background watch.
 */
object TailnetMonitor {

    const val TAILSCALE = "com.tailscale.ipn"

    /**
     * VPN apps Gantry recognises as a connected VPN's owner, by their ongoing
     * notification. Only Tailscale can be connected directly; the others are
     * disconnected through their notification's button and opened to connect.
     */
    val KNOWN_VPNS: Map<String, String> = linkedMapOf(
        TAILSCALE to "Tailscale",
        "ch.protonvpn.android" to "Proton VPN",
        "com.wireguard.android" to "WireGuard",
        "com.zaneschepke.wireguardautotunnel" to "WG Tunnel",
        "net.mullvad.mullvadvpn" to "Mullvad VPN",
        "net.ivpn.client" to "IVPN",
        "org.mozilla.firefox.vpn" to "Mozilla VPN",
    )
    private const val RECEIVER = "com.tailscale.ipn.IPNReceiver"
    private const val ACTION_CONNECT = "com.tailscale.ipn.CONNECT_VPN"
    private const val ACTION_DISCONNECT = "com.tailscale.ipn.DISCONNECT_VPN"

    private const val FOREGROUND_INTERVAL_MS = 60_000L
    private const val BACKGROUND_INTERVAL_MS = 5 * 60_000L
    private const val PROBE_TIMEOUT_MS = 4_000
    /** Wait after a network change before re-checking, so it has settled. */
    private const val NETWORK_SETTLE_MS = 2_000L
    /** One-second checks after a connect/disconnect, to confirm it landed. */
    private const val CONFIRM_CHECKS = 10
    /** IANA-reserved, always resolvable; the DNS check's lookup target. */
    private const val DNS_CHECK_NAME = "example.com"
    /** Consecutive failed checks before the tailnet counts as down. */
    private const val FAILURES_FOR_DOWN = 2
    private const val MIN_AUTO_FIX_GAP_MS = 10 * 60_000L
    private const val MAX_AUTO_FIX_GAP_MS = 2 * 60 * 60_000L

    private val hostKey = stringPreferencesKey("tailnet_host")
    private val portKey = intPreferencesKey("tailnet_port")
    private val autoFixKey = booleanPreferencesKey("tailnet_auto_fix")
    private val lastFixAtKey = longPreferencesKey("tailnet_last_fix_at")
    private val lastFixAutoKey = booleanPreferencesKey("tailnet_last_fix_auto")
    private val preferredKey = stringPreferencesKey("vpn_preferred")
    private val lastUsedKey = stringPreferencesKey("vpn_last_used")
    // Mirrors the stored last-used VPN so each check doesn't write it again.
    @Volatile
    private var lastUsedSeen: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(TailnetState())
    val state: StateFlow<TailnetState> = _state.asStateFlow()

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val fixLock = Mutex()
    private lateinit var app: Context
    private var loop: Job? = null
    private var foregroundWatchers = 0
    private var backgroundWatchers = 0

    // Auto-fix backoff: grows while fixing doesn't help, resets when it does.
    private var autoFixGap = MIN_AUTO_FIX_GAP_MS
    private var lastAutoAttemptAt = 0L

    fun settings(context: Context): Flow<TailnetSettings> =
        context.dataStore.data.map { prefs ->
            TailnetSettings(
                host = prefs[hostKey] ?: TailnetSettings.DEFAULT_HOST,
                port = prefs[portKey] ?: TailnetSettings.DEFAULT_PORT,
                autoFix = prefs[autoFixKey] ?: false,
                preferred = prefs[preferredKey] ?: TailnetSettings.LAST_USED,
                lastUsed = prefs[lastUsedKey],
            )
        }.distinctUntilChanged()

    suspend fun setHost(context: Context, host: String) {
        val clean = host.trim().filter { it.isLetterOrDigit() || it in ".-:" }.take(253)
        context.dataStore.edit { it[hostKey] = clean }
        wake.trySend(Unit)
    }

    suspend fun setPort(context: Context, port: Int) {
        context.dataStore.edit { it[portKey] = port.coerceIn(1, 65_535) }
        wake.trySend(Unit)
    }

    suspend fun setPreferred(context: Context, packageName: String) {
        context.dataStore.edit { it[preferredKey] = packageName }
    }

    /** Known VPN apps that are installed, in [KNOWN_VPNS] order. */
    fun installedVpns(context: Context): List<Pair<String, String>> =
        KNOWN_VPNS.filterKeys { installed(context, it) }.toList()

    /**
     * Disconnects [packageName]'s VPN: Tailscale by its automation receiver,
     * anything else by pressing its notification's Disconnect button. False
     * when there's no way — the caller opens the app instead.
     */
    fun disconnect(context: Context, packageName: String): Boolean {
        val result = if (packageName == TAILSCALE) {
            send(context.applicationContext, ACTION_DISCONNECT)
            true
        } else {
            MediaNotificationListener.instance?.pressDisconnect(packageName) == true
        }
        if (result) {
            // Show it straight away — a stale "connected" invites a second
            // tap, which (with no Disconnect button left) opens the VPN app —
            // then let quick checks confirm or correct it.
            setStatus(TailnetStatus.Off, null)
            confirmSoon()
        }
        return result
    }

    /** Checks every second for a few seconds, to catch a connect/disconnect landing. */
    private fun confirmSoon() {
        scope.launch {
            repeat(CONFIRM_CHECKS) {
                delay(1_000)
                wake.trySend(Unit)
            }
        }
    }

    suspend fun setAutoFix(context: Context, value: Boolean) {
        context.dataStore.edit { it[autoFixKey] = value }
        autoFixGap = MIN_AUTO_FIX_GAP_MS
        wake.trySend(Unit)
    }

    /**
     * Live state while collected. [foreground] watchers (the visible home
     * screen) get minute-by-minute checks and an immediate one on arrival.
     */
    fun watch(context: Context, foreground: Boolean): Flow<TailnetState> = callbackFlow {
        attach(context, foreground)
        val relay = launch { state.collect { send(it) } }
        awaitClose {
            relay.cancel()
            detach(foreground)
        }
    }

    /** Checks now; used on tap when things look fine or unknown. */
    fun checkNow() {
        wake.trySend(Unit)
    }

    /** Tap with nothing connected: connect Tailscale. */
    fun connect(context: Context) {
        send(context.applicationContext, ACTION_CONNECT)
        confirmSoon()
    }

    /** Tap on a hung tailnet: restart Tailscale now. */
    fun fix(context: Context) {
        if (!::app.isInitialized) app = context.applicationContext
        scope.launch { restart(auto = false) }
    }

    @Synchronized
    private fun attach(context: Context, foreground: Boolean) {
        if (!::app.isInitialized) app = context.applicationContext
        if (foreground) foregroundWatchers++ else backgroundWatchers++
        if (loop?.isActive != true) {
            loop = scope.launch { run() }
        }
        wake.trySend(Unit)
    }

    @Synchronized
    private fun detach(foreground: Boolean) {
        if (foreground) foregroundWatchers-- else backgroundWatchers--
        if (foregroundWatchers <= 0 && backgroundWatchers <= 0) {
            foregroundWatchers = 0
            backgroundWatchers = 0
            loop?.cancel()
            loop = null
        }
    }

    private suspend fun run() {
        val prefs = app.dataStore.data.first()
        _state.update {
            it.copy(lastFixAt = prefs[lastFixAtKey], lastFixAuto = prefs[lastFixAutoKey] ?: false)
        }
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        // Network changes (Wi-Fi ↔ mobile) are when Tailscale tends to hang:
        // re-check once things settle rather than waiting out the interval.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { settleThenWake() }
            override fun onLost(network: Network) { settleThenWake() }
        }
        runCatching { connectivity?.registerDefaultNetworkCallback(callback) }
        var failures = 0
        try {
            while (true) {
                val settings = settings(app).first()
                failures = checkOnce(settings, connectivity, failures)
                if (_state.value.status == TailnetStatus.Down && settings.autoFix) {
                    maybeAutoFix()
                }
                val interval = if (foregroundWatchers > 0) FOREGROUND_INTERVAL_MS
                else BACKGROUND_INTERVAL_MS
                withTimeoutOrNull(interval) { wake.receive() }
            }
        } finally {
            runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
    }

    private fun settleThenWake() {
        scope.launch {
            delay(NETWORK_SETTLE_MS)
            wake.trySend(Unit)
        }
    }

    /** One check; returns the updated consecutive-failure count. */
    private fun checkOnce(
        settings: TailnetSettings,
        connectivity: ConnectivityManager?,
        failures: Int,
    ): Int {
        if (fixLock.isLocked) return failures
        if (connectivity == null) {
            setStatus(TailnetStatus.Unavailable, null)
            return 0
        }
        if (!vpnUp(connectivity)) {
            setStatus(TailnetStatus.Off, null)
            return 0
        }
        // Tailnet checks only while Tailscale owns the VPN: with another VPN
        // up the host can't answer, and "fixing" would knock that VPN off.
        val owner = vpnOwner(connectivity)
        if (owner != TAILSCALE) {
            setStatus(TailnetStatus.Connected, owner)
            return 0
        }
        val status = when {
            // No check host set: report it like any other connected VPN
            // rather than claiming a health we haven't measured.
            settings.host.isBlank() -> TailnetStatus.Connected
            !underlyingInternet(connectivity) -> TailnetStatus.Offline
            probe(settings.host, settings.port) && dnsWorks() -> TailnetStatus.Healthy
            else -> {
                val count = failures + 1
                setStatus(
                    if (count >= FAILURES_FOR_DOWN) TailnetStatus.Down else TailnetStatus.Checking,
                    TAILSCALE,
                )
                return count
            }
        }
        setStatus(status, TAILSCALE)
        if (status == TailnetStatus.Healthy) autoFixGap = MIN_AUTO_FIX_GAP_MS
        return 0
    }

    /**
     * Which app owns the connected VPN. Tailscale's tunnel carries its own
     * address ranges (100.64/10, fd7a:115c:a1e0::/48); other apps are known
     * by their ongoing notification. Null when it can't be told.
     */
    private fun vpnOwner(connectivity: ConnectivityManager): String? {
        val tailscaleTunnel = runCatching {
            @Suppress("DEPRECATION")
            connectivity.allNetworks.any { network ->
                connectivity.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true &&
                    connectivity.getLinkProperties(network)?.linkAddresses
                        ?.any { isTailscaleAddress(it.address) } == true
            }
        }.getOrDefault(false)
        if (tailscaleTunnel) return TAILSCALE
        return MediaNotificationListener.instance
            ?.ongoingFrom(KNOWN_VPNS.keys - TAILSCALE)?.packageName
    }

    private fun isTailscaleAddress(address: java.net.InetAddress): Boolean {
        val b = address.address
        return when (b.size) {
            4 -> (b[0].toInt() and 0xFF) == 100 && (b[1].toInt() and 0xC0) == 64
            16 -> (b[0].toInt() and 0xFF) == 0xFD && (b[1].toInt() and 0xFF) == 0x7A &&
                (b[2].toInt() and 0xFF) == 0x11 && (b[3].toInt() and 0xFF) == 0x5C &&
                (b[4].toInt() and 0xFF) == 0xA1 && (b[5].toInt() and 0xFF) == 0xE0
            else -> false
        }
    }

    private suspend fun maybeAutoFix() {
        val now = System.currentTimeMillis()
        if (now - lastAutoAttemptAt < autoFixGap) return
        lastAutoAttemptAt = now
        val fixed = restart(auto = true)
        // A restart that didn't help won't help again soon: back off.
        autoFixGap = if (fixed) MIN_AUTO_FIX_GAP_MS
        else (autoFixGap * 2).coerceAtMost(MAX_AUTO_FIX_GAP_MS)
    }

    /**
     * Disconnect, wait, connect — then wait up to ~30s for the check host to
     * answer again. True when the tailnet came back.
     */
    private suspend fun restart(auto: Boolean): Boolean = fixLock.withLock {
        setStatus(TailnetStatus.Fixing)
        send(app, ACTION_DISCONNECT)
        delay(3_000)
        send(app, ACTION_CONNECT)
        val settings = settings(app).first()
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        repeat(15) {
            delay(2_000)
            if (connectivity != null && vpnUp(connectivity) &&
                probe(settings.host, settings.port) && dnsWorks()
            ) {
                val at = System.currentTimeMillis()
                runCatching {
                    app.dataStore.edit {
                        it[lastFixAtKey] = at
                        it[lastFixAutoKey] = auto
                    }
                }
                _state.update {
                    it.copy(status = TailnetStatus.Healthy, lastFixAt = at, lastFixAuto = auto)
                }
                return@withLock true
            }
        }
        setStatus(TailnetStatus.Down)
        false
    }

    private fun setStatus(status: TailnetStatus) = _state.update { it.copy(status = status) }

    private fun setStatus(status: TailnetStatus, vpnPackage: String?) {
        _state.update { it.copy(status = status, vpnPackage = vpnPackage) }
        if (vpnPackage != null) rememberLastUsed(vpnPackage)
    }

    /** Records [packageName] as the last VPN seen connected, once per change. */
    private fun rememberLastUsed(packageName: String) {
        if (packageName == lastUsedSeen) return
        lastUsedSeen = packageName
        scope.launch { runCatching { app.dataStore.edit { it[lastUsedKey] = packageName } } }
    }

    private fun send(context: Context, action: String) {
        runCatching {
            context.sendBroadcast(Intent(action).setComponent(ComponentName(TAILSCALE, RECEIVER)))
        }
    }

    fun installed(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)

    private fun capabilities(connectivity: ConnectivityManager): List<NetworkCapabilities> =
        runCatching {
            @Suppress("DEPRECATION")
            connectivity.allNetworks.mapNotNull { connectivity.getNetworkCapabilities(it) }
        }.getOrDefault(emptyList())

    private fun vpnUp(connectivity: ConnectivityManager): Boolean =
        capabilities(connectivity).any { it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }

    /** A validated, non-VPN network with internet under the tunnel. */
    private fun underlyingInternet(connectivity: ConnectivityManager): Boolean =
        capabilities(connectivity).any {
            !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

    /**
     * Resolves a fixed name through the phone's DNS — which, with Tailscale
     * overriding local DNS, is Tailscale's resolver. Tailscale's typical hang
     * is exactly this ("DNS unavailable: can't reach the configured DNS
     * servers") while the tunnel itself still answers, so a host check alone
     * would call it healthy. Cache skipped so a stale answer can't pass.
     */
    private fun dnsWorks(): Boolean {
        val done = java.util.concurrent.CountDownLatch(1)
        var ok = false
        val cancel = android.os.CancellationSignal()
        runCatching {
            DnsResolver.getInstance().query(
                null, DNS_CHECK_NAME, DnsResolver.TYPE_A, DnsResolver.FLAG_NO_CACHE_LOOKUP,
                Runnable::run, cancel,
                object : DnsResolver.Callback<List<java.net.InetAddress>> {
                    override fun onAnswer(answer: List<java.net.InetAddress>, rcode: Int) {
                        ok = answer.isNotEmpty()
                        done.countDown()
                    }
                    override fun onError(error: DnsResolver.DnsException) {
                        done.countDown()
                    }
                },
            )
        }.onFailure { return false }
        if (!done.await(PROBE_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
            cancel.cancel()
            return false
        }
        return ok
    }

    /**
     * Reaches [host] over the tailnet. On port 22 the SSH server's greeting
     * must come back too: carriers use the same 100.64/10 range, so a bare
     * open port could be some other machine when the tunnel is down. Nothing
     * is sent — no login, no data — just the connection and its greeting.
     */
    private fun probe(host: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
            if (port != 22) return@use true
            socket.soTimeout = PROBE_TIMEOUT_MS
            val greeting = ByteArray(4)
            var read = 0
            val input = socket.getInputStream()
            while (read < greeting.size) {
                val n = input.read(greeting, read, greeting.size - read)
                if (n < 0) break
                read += n
            }
            read == 4 && String(greeting, Charsets.US_ASCII) == "SSH-"
        }
    }.getOrDefault(false)

    /** A VPN app's own screen, for connecting it or a long-press. */
    fun launchIntent(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
