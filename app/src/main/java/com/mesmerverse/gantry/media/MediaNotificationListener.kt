package com.mesmerverse.gantry.media

import android.content.pm.PackageManager
import android.Manifest
import android.app.ActivityOptions
import android.app.Notification
import android.bluetooth.BluetoothDevice
import android.os.Process
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothA2dp
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.RemoteInput
import android.media.AudioAttributes
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import com.mesmerverse.gantry.tailnet.TailnetMonitor
import com.mesmerverse.gantry.settings.ReadAloudScope
import com.mesmerverse.gantry.settings.SettingsRepository
import com.mesmerverse.gantry.settings.WidgetSettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** A notification action button (e.g. Reply, Mark as read). */
data class NotifAction(val index: Int, val title: String, val isReply: Boolean)

/**
 * One distinct notification message. [copies] is how many identical ones the
 * app posted (e.g. a camera repeating "Motion detected"); [keys] are all of
 * their dismissal keys. [key] is the most recent, used to open/act.
 */
data class NotifItem(
    val key: String,
    val title: String?,
    val text: String?,
    val actions: List<NotifAction> = emptyList(),
    val copies: Int = 1,
    val keys: List<String> = listOf(key),
) {
    val preview: String?
        get() = listOfNotNull(title, text).joinToString(" — ").take(120).ifBlank { null }
}

/** What an app currently has waiting: count, a one-line preview, the items. */
data class NotifInfo(
    val count: Int,
    val preview: String?,
    val items: List<NotifItem> = emptyList(),
    /** Post time of the newest item; orders apps newest-first on home. */
    val latestPost: Long = 0L,
)

/**
 * Grants access to active media sessions and powers the home notification
 * strip and list and per-app indicators: reads each notification's title and
 * text to display, fires its actions and inline replies when the user taps
 * them, and — when armed — speaks new ones aloud (see [maybeReadAloud]).
 * Nothing is stored or sent anywhere.
 */
class MediaNotificationListener : NotificationListenerService() {

    // --- Read-aloud state (all touched only on the main thread) ---
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settings: SettingsRepository
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var armed = false
    private var scope = ReadAloudScope.MessagesOnly
    // Effective arming = the read-aloud flag AND the widget being enabled (its
    // master switch), so disabling the widget can never leave it reading with
    // no visible glyph.
    private var readEnabled = false
    private var widgetOn = false
    // Bluetooth device addresses that auto-arm on connect / disarm on disconnect.
    private var autoArmDevices: Set<String> = emptySet()
    // Auto-arm devices currently connected over an audio profile — the one
    // case where a locked phone may speak full content (it's in the car).
    private val connectedAutoArm = HashSet<String>()
    private var btReceiver: BroadcastReceiver? = null
    // key -> hash of the last content spoken, so a conversation's new message
    // (same key, new text) reads but identical reposts don't.
    private val spokenHashes = HashMap<String, Int>()
    // Messages that arrived before the engine finished initializing.
    private val pending = ArrayDeque<Pair<String, String>>()

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        val widgetSettings = WidgetSettingsRepository(applicationContext)
        serviceScope.launch {
            settings.readAloudEnabled.collect { readEnabled = it; updateArmed() }
        }
        serviceScope.launch {
            widgetSettings.readAloudWidgetEnabled.collect { widgetOn = it; updateArmed() }
        }
        serviceScope.launch {
            settings.readAloudScope.collect { scope = it }
        }
        serviceScope.launch {
            settings.autoArmDevices.collect { autoArmDevices = it; reconcileAutoArm() }
        }
        registerBluetoothReceiver()
        // Tailnet auto-fix needs a watch while home isn't on screen; this
        // service is the one part of Gantry the system keeps running.
        serviceScope.launch {
            TailnetMonitor.settings(applicationContext)
                .map { it.autoFix }
                .distinctUntilChanged()
                .collectLatest { autoFix ->
                    if (autoFix) TailnetMonitor.watch(applicationContext, foreground = false).collect {}
                }
        }
    }

    override fun onDestroy() {
        btReceiver?.let { runCatching { unregisterReceiver(it) } }
        disarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    // Diagnostics only; content-free, and silent in release builds.
    private val debuggable by lazy {
        applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    private fun debug(msg: String) { if (debuggable) Log.i(TAG, msg) }

    private fun updateArmed() {
        val effective = readEnabled && widgetOn
        if (effective == armed) return
        armed = effective
        debug("read-aloud armed=$armed")
        if (armed) ensureTts() else disarm()
    }

    /**
     * Flips the read-aloud flag when the phone connects to / disconnects from a
     * Bluetooth device the user chose (e.g. the car), so a drive arms it with
     * no touch. Only the flag is written; [updateArmed] still gates on the
     * widget's master switch.
     *
     * Keyed on audio-profile (A2DP / headset) connections of a bonded device,
     * not raw ACL links: a profile connection needs the paired, authenticated
     * device, where a baseband link can come from anything spoofing its address.
     */
    private fun registerBluetoothReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val device: BluetoothDevice? = intent?.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java,
                )
                val address = device?.address ?: return
                if (address !in autoArmDevices) return
                val bonded = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
                    runCatching { device.bondState == BluetoothDevice.BOND_BONDED }
                        .getOrDefault(false)
                val arm = when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                    BluetoothProfile.STATE_CONNECTED -> if (bonded) true else return
                    BluetoothProfile.STATE_DISCONNECTED -> false
                    else -> return
                }
                if (arm) connectedAutoArm += address else connectedAutoArm -= address
                debug("bluetooth auto-arm -> $arm")
                // One profile dropping while another stays up isn't a departure.
                if (!arm && connectedAutoArm.isNotEmpty()) return
                serviceScope.launch { runCatching { settings.setReadAloudAutoArmed(arm) } }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        btReceiver = receiver
    }

    /**
     * Re-derives which auto-arm devices are actually connected, and drops an
     * auto-arm whose disconnect was missed (the receiver is dynamic, so a
     * disconnect while this service was dead never arrived) — read-aloud
     * must not stay armed after leaving the car.
     */
    private fun reconcileAutoArm() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || autoArmDevices.isEmpty()) {
            connectedAutoArm.clear()
            serviceScope.launch { disarmIfAutoArmed() }
            return
        }
        val found = HashSet<String>()
        var pendingProfiles = 2
        fun done() {
            if (--pendingProfiles > 0) return
            connectedAutoArm.clear()
            connectedAutoArm += found
            if (found.isEmpty()) serviceScope.launch { disarmIfAutoArmed() }
        }
        listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).forEach { profile ->
            val asked = runCatching {
                adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        runCatching {
                            proxy.connectedDevices.forEach { d ->
                                if (d.address in autoArmDevices) found += d.address
                            }
                        }
                        runCatching { adapter.closeProfileProxy(p, proxy) }
                        done()
                    }
                    override fun onServiceDisconnected(p: Int) {}
                }, profile)
            }.getOrDefault(false)
            if (!asked) done()
        }
    }

    private suspend fun disarmIfAutoArmed() {
        runCatching {
            if (settings.isReadAloudAutoArmed()) settings.setReadAloudAutoArmed(false)
        }
    }

    override fun onListenerConnected() {
        instance = this
        reconcileAutoArm()
        publish()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        // Don't blank on a transient disconnect; ask to rebind to recover.
        runCatching { requestRebind(ComponentName(this, MediaNotificationListener::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        maybeReadAloud(sbn)
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { spokenHashes.remove(it) }
        publish()
    }

    /**
     * Speaks a newly-arrived notification when armed. Driven from the listener
     * (not the launcher UI) so it works while another app — a map, say — is in
     * the foreground. Only reacts to fresh posts, so arming never replays the
     * backlog: existing notifications don't re-fire this callback.
     */
    private fun maybeReadAloud(sbn: StatusBarNotification?) {
        if (!armed) return
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        if (!sbn.isClearable) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        // Never speak a notification the app marked sensitive (OTP/2FA, banking)
        // aloud — it may be overheard, e.g. over car Bluetooth with passengers.
        if (notification.visibility == Notification.VISIBILITY_SECRET) return
        // The airlock's notifications stay in the airlock: never spoken.
        if (sbn.user != Process.myUserHandle()) return
        if (scope == ReadAloudScope.MessagesOnly &&
            notification.category != Notification.CATEGORY_MESSAGE
        ) return

        // Locked phone, no car connected: honour lock-screen privacy — speak
        // the app's public version, else just which app it is, never the
        // private content out of the phone's own speaker.
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        val redact = locked && connectedAutoArm.isEmpty() &&
            notification.visibility != Notification.VISIBILITY_PUBLIC
        val source = if (redact) notification.publicVersion else notification
        val extras = source?.extras
        var title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (redact && title.isBlank() && text.isBlank()) {
            title = "New notification from ${appLabel(sbn.packageName)}"
            text = ""
        }
        if (title.isBlank() && text.isBlank()) return

        val hash = "$title|$text".hashCode()
        if (spokenHashes[sbn.key] == hash) return
        spokenHashes[sbn.key] = hash

        val spoken = when {
            title.isNotBlank() && text.isNotBlank() -> "$title. $text"
            else -> title.ifBlank { text }
        }
        speak(spoken, sbn.key + hash)
    }

    private fun appLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("an app")

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            debug("tts init status=$status ready=$ttsReady pending=${pending.size}")
            if (ttsReady) {
                // Assistant usage ducks music and routes to the active output
                // (e.g. the car's Bluetooth) instead of fighting it.
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                while (pending.isNotEmpty()) {
                    val (t, id) = pending.removeFirst()
                    tts?.speak(t, TextToSpeech.QUEUE_ADD, null, id)
                }
            } else {
                pending.clear()
            }
        }
    }

    private fun speak(text: String, utteranceId: String) {
        val engine = tts
        if (engine == null || !ttsReady) {
            // Engine still initializing — read it as soon as it's ready.
            pending.addLast(text to utteranceId)
            ensureTts()
            return
        }
        runCatching { engine.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId) }
    }

    /** Stop and release the engine; a fresh trip re-reads new arrivals. */
    private fun disarm() {
        pending.clear()
        spokenHashes.clear()
        ttsReady = false
        tts?.let {
            runCatching { it.stop() }
            runCatching { it.shutdown() }
        }
        tts = null
    }

    /** Dismiss one notification by key (clears it from the shade too). */
    fun dismiss(key: String) {
        runCatching { cancelNotification(key) }
    }

    /**
     * The ongoing (foreground-service) notification one of [packages] shows
     * while its VPN is connected — how Gantry tells which app owns the VPN,
     * since Android hides a VPN's owner from other apps.
     */
    fun ongoingFrom(packages: Set<String>): StatusBarNotification? = runCatching {
        activeNotifications?.firstOrNull {
            it.packageName in packages &&
                it.user == android.os.Process.myUserHandle() &&
                it.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        }
    }.getOrNull()

    /**
     * Presses the "Disconnect" (or Stop / Turn off / Pause) button on
     * [packageName]'s ongoing notification. False when there's no such
     * notification or button — the caller falls back to opening the app.
     */
    fun pressDisconnect(packageName: String): Boolean {
        val sbn = ongoingFrom(setOf(packageName)) ?: return false
        val index = sbn.notification.actions
            ?.indexOfFirst { action -> action.title?.let { DISCONNECT_WORDS.containsMatchIn(it) } == true }
            ?: -1
        return index >= 0 && sendAction(sbn.key, index, null)
    }

    /**
     * The notification's content intent — fires the app to the specific item
     * (conversation, email…). Sent by the caller from the foreground so the
     * background-activity-launch rules allow it.
     */
    fun contentIntentFor(key: String): android.app.PendingIntent? = runCatching {
        activeNotifications?.firstOrNull { it.key == key }?.notification?.contentIntent
    }.getOrNull()

    /**
     * Fire a notification action. For a reply action, [replyText] is filled
     * into its RemoteInput. Actions are usually broadcasts/services (not
     * background-launch restricted); an explicit allowance covers the rest.
     */
    fun sendAction(key: String, index: Int, replyText: String?): Boolean = runCatching {
        val notif = activeNotifications?.firstOrNull { it.key == key }?.notification
            ?: return false
        val action = notif.actions?.getOrNull(index) ?: return false
        val actionIntent = action.actionIntent ?: return false
        val options = ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
            .toBundle()
        val remoteInputs = action.remoteInputs
        if (!replyText.isNullOrEmpty() && !remoteInputs.isNullOrEmpty()) {
            val fill = Intent()
            val results = Bundle()
            remoteInputs.forEach { results.putCharSequence(it.resultKey, replyText) }
            RemoteInput.addResultsToIntent(remoteInputs, fill, results)
            actionIntent.send(this, 0, fill, null, null, null, options)
        } else {
            actionIntent.send(this, 0, null, null, null, null, options)
        }
        true
    }.getOrDefault(false)

    private fun publish() {
        notifications.value = runCatching {
            fun isSummary(sbn: StatusBarNotification) =
                (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

            fun item(sbn: StatusBarNotification): NotifItem {
                val extras = sbn.notification.extras
                val actions = sbn.notification.actions?.mapIndexedNotNull { i, a ->
                    val title = a.title?.toString()?.takeIf { it.isNotBlank() }
                        ?: return@mapIndexedNotNull null
                    NotifAction(i, title, isReply = !a.remoteInputs.isNullOrEmpty())
                }.orEmpty()
                return NotifItem(
                    key = sbn.key,
                    title = extras?.getCharSequence(Notification.EXTRA_TITLE)
                        ?.toString()?.takeIf { it.isNotBlank() },
                    text = extras?.getCharSequence(Notification.EXTRA_TEXT)
                        ?.toString()?.takeIf { it.isNotBlank() },
                    actions = actions,
                )
            }

            activeNotifications
                .filter { it.isClearable }
                .groupBy { "${it.user.hashCode()}/${it.packageName}" }
                .mapValues { (_, group) ->
                    // Prefer real children; fall back to a lone group summary.
                    val children = group.filterNot { isSummary(it) }
                    val source = children.ifEmpty { group }.sortedByDescending { it.postTime }
                    // Collapse identical messages (an app repeating the same
                    // alert) into one row; keep a copy count and every key so
                    // dismissing clears them all.
                    val byContent = LinkedHashMap<String, MutableList<StatusBarNotification>>()
                    source.forEach { sbn ->
                        val base = item(sbn)
                        val sig = base.title.orEmpty() + "|" + base.text.orEmpty()
                        byContent.getOrPut(sig) { mutableListOf() }.add(sbn)
                    }
                    val items = byContent.values.map { copies ->
                        item(copies.first()).copy(
                            copies = copies.size,
                            keys = copies.map { it.key },
                        )
                    }
                    NotifInfo(
                        // Distinct messages; copies are surfaced per item.
                        count = items.size,
                        preview = items.firstOrNull()?.preview,
                        items = items,
                        latestPost = source.firstOrNull()?.postTime ?: 0L,
                    )
                }
                .filterValues { it.items.isNotEmpty() }
        }.getOrDefault(emptyMap())
    }

    companion object {
        /** "userHash/package" -> what that app has waiting. */
        val notifications = MutableStateFlow<Map<String, NotifInfo>>(emptyMap())

        /** Live service instance for dismiss/launch actions; null when unbound. */
        @Volatile
        var instance: MediaNotificationListener? = null

        private const val TAG = "GantryReadAloud"

        private val DISCONNECT_WORDS =
            Regex("disconnect|stop|turn off|pause|quit", RegexOption.IGNORE_CASE)
    }
}
