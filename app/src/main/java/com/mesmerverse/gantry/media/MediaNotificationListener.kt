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
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.mesmerverse.gantry.tailnet.TailnetMonitor
import com.mesmerverse.gantry.settings.ReadAloudScope
import com.mesmerverse.gantry.settings.SettingsRepository
import com.mesmerverse.gantry.settings.WidgetSettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

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
    // Completes with whether the engine initialized; null until one is made.
    private var ttsInit: CompletableDeferred<Boolean>? = null
    // Utterance id -> completes when the engine finishes, stops or fails it.
    // Completed from the engine's binder thread, hence concurrent.
    private val utteranceDone = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private var utteranceSeq = 0L
    // Key of the notification being spoken right now, if any.
    private var speakingKey: String? = null
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
    // Notifications waiting to be spoken, in post order, drained one at a
    // time by [speechJob]. Only the key is kept: what's said (and whether
    // it's said at all) is decided from the live notification, the current
    // scope and the lock state at the moment it's spoken, not when posted.
    private class QueuedSpeech(val key: String, val postedAt: Long)
    private val speechQueue = ArrayDeque<QueuedSpeech>()
    private var speechJob: Job? = null
    // Notification-sound players seen while waiting, with when first seen. One
    // older than [SOUND_MAX_WAIT_MS] no longer counts as a chime (another
    // app's idle player, or a looping sound). A list matched with equals():
    // the config's hashCode also covers its routing and mute state.
    private class SeenSound(val config: AudioPlaybackConfiguration, val firstSeen: Long)
    private val seenSounds = ArrayList<SeenSound>()

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
        if (!hasAccess()) notifications.value = emptyMap()
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
        // Access revoked: what was received is no longer current; clear it.
        // Otherwise don't blank on a transient disconnect; ask to rebind.
        if (!hasAccess()) {
            notifications.value = emptyMap()
            clearSpeechQueue()
            return
        }
        runCatching { requestRebind(ComponentName(this, MediaNotificationListener::class.java)) }
    }

    private fun hasAccess(): Boolean = runCatching {
        getSystemService(android.app.NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(this, MediaNotificationListener::class.java))
    }.getOrDefault(false)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        maybeReadAloud(sbn)
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { key ->
            spokenHashes.remove(key)
            // Withdrawn before its turn: don't announce it (#6).
            if (speechQueue.removeAll { it.key == key }) debug("dequeued: removed")
            if (key == speakingKey) {
                debug("stopped: removed while speaking")
                runCatching { tts?.stop() }
            }
        }
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
        if (sbn == null || !speakable(sbn)) return
        val full = spokenText(sbn.notification) ?: return
        val hash = full.hashCode()
        if (spokenHashes[sbn.key] == hash) return
        spokenHashes[sbn.key] = hash

        // An update to a notification still waiting replaces its entry, so
        // superseded text is never spoken (#7). The system plays the sound
        // just after a post; the drain waits that (and any sound from posts
        // right behind it) out, then speaks in the order posts arrived.
        if (speechQueue.removeAll { it.key == sbn.key }) debug("dequeued: superseded")
        speechQueue.addLast(QueuedSpeech(sbn.key, SystemClock.elapsedRealtime()))
        if (speechJob?.isActive != true) {
            speechJob = serviceScope.launch { drainSpeechQueue() }
        }
    }

    /**
     * Whether [sbn] may be spoken under the current settings. Checked on
     * arrival and again just before speaking, so a scope change applies to
     * what's already waiting (#8).
     */
    private fun speakable(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        if (sbn.packageName == packageName) return false
        if (!sbn.isClearable) return false
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        // Never speak a notification the app marked sensitive (OTP/2FA, banking)
        // aloud — it may be overheard, e.g. over car Bluetooth with passengers.
        if (notification.visibility == Notification.VISIBILITY_SECRET) return false
        // The airlock's notifications stay in the airlock: never spoken.
        if (sbn.user != Process.myUserHandle()) return false
        if (scope == ReadAloudScope.MessagesOnly &&
            notification.category != Notification.CATEGORY_MESSAGE
        ) return false
        return true
    }

    /**
     * Speaks the queue one utterance at a time. Each entry is re-read from
     * the live notification when its turn comes: gone means skipped (#6), its
     * current text is what's said (#7), and [speakable] runs again (#8). The
     * lock check happens right before each utterance and the next waits for
     * it to finish, so a private message queued behind a long one is still
     * redacted if the phone locks meanwhile (#9).
     */
    private suspend fun drainSpeechQueue() {
        while (speechQueue.isNotEmpty()) {
            awaitNotificationSounds()
            if (speechQueue.isEmpty()) break
            // Engine first: everything below is decided immediately before
            // speaking, never before a wait.
            val engine = awaitEngine()
            val next = speechQueue.removeFirstOrNull() ?: break
            if (!armed) break
            if (engine == null) { debug("skip: no speech engine"); continue }
            val live = runCatching { getActiveNotifications(arrayOf(next.key)) }
                .getOrNull()?.firstOrNull { it.key == next.key }
            if (live == null) { debug("skip: no longer posted"); continue }
            if (!speakable(live)) { debug("skip: no longer speakable"); continue }
            val full = spokenText(live.notification) ?: continue
            spokenHashes[live.key] = full.hashCode()
            // Locked (or screen off, which may not have locked yet), no car
            // connected: honour lock-screen privacy — speak the app's public
            // version, else just which app it is, never the private content
            // out of the phone's own speaker.
            val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true ||
                getSystemService(PowerManager::class.java)?.isInteractive == false
            val text = if (locked && connectedAutoArm.isEmpty() &&
                live.notification.visibility != Notification.VISIBILITY_PUBLIC
            ) {
                live.notification.publicVersion?.let(::spokenText)
                    ?: "New notification from ${appLabel(live.packageName)}"
            } else full
            debug("speak locked=$locked redacted=${text !== full} queued=${speechQueue.size}")
            speakAndWait(engine, live.key, text)
        }
    }

    /** "Title. Text" from a notification's extras, or null when both are blank. */
    private fun spokenText(notification: Notification): String? {
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        return when {
            title.isNotBlank() && text.isNotBlank() -> "$title. $text"
            else -> title.ifBlank { text }.ifBlank { null }
        }
    }

    /**
     * Waits until notification sounds have gone quiet for the queued posts:
     * the newest post's sound gets [SOUND_START_WAIT_MS] to start, and speech
     * waits until no sound has played for [SOUND_TAIL_MS] (output latency,
     * e.g. Bluetooth). Capped at [SOUND_MAX_WAIT_MS] so a stream of posts
     * can't hold speech back for good. A silent post costs only the start wait.
     */
    private suspend fun awaitNotificationSounds() {
        val audio = getSystemService(AudioManager::class.java) ?: run {
            delay(CHIME_GAP_MS)
            return
        }
        val begin = SystemClock.elapsedRealtime()
        var lastSound = 0L
        var newestSound = 0L
        var firstPoll = true
        while (true) {
            val now = SystemClock.elapsedRealtime()
            if (now - begin >= SOUND_MAX_WAIT_MS) break
            val newest = speechQueue.lastOrNull()?.postedAt ?: return
            notificationSoundStart(audio, now, firstPoll)?.let {
                lastSound = now
                newestSound = maxOf(newestSound, it)
            }
            firstPoll = false
            val quiet = now - lastSound >= SOUND_TAIL_MS
            // Only a player that appeared after the newest post is its sound;
            // one still playing from an earlier post doesn't count.
            val awaitingStart = newestSound < newest && now - newest < SOUND_START_WAIT_MS
            if (quiet && !awaitingStart) break
            delay(SOUND_POLL_MS)
        }
        debug("chime wait ${SystemClock.elapsedRealtime() - begin}ms queued=${speechQueue.size}")
    }

    /**
     * When the newest notification sound now playing was first seen, or null
     * if none is. Players already there on a wait's [firstPoll] predate the
     * post, so they count for at most [SOUND_PREEXISTING_MS] — enough for a
     * chime in progress, without an idle player costing the full cap.
     */
    private fun notificationSoundStart(audio: AudioManager, now: Long, firstPoll: Boolean): Long? {
        val players = runCatching {
            audio.activePlaybackConfigurations.filter {
                it.audioAttributes.usage == AudioAttributes.USAGE_NOTIFICATION ||
                    it.audioAttributes.usage == AudioAttributes.USAGE_NOTIFICATION_EVENT
            }
        }.getOrDefault(emptyList())
        seenSounds.removeAll { seen -> players.none { it == seen.config } }
        val unseenAt = if (firstPoll) now - SOUND_MAX_WAIT_MS + SOUND_PREEXISTING_MS else now
        return players.mapNotNull { config ->
            val seen = seenSounds.firstOrNull { it.config == config }
                ?: SeenSound(config, unseenAt).also { seenSounds += it }
            seen.firstSeen.takeIf { now - it < SOUND_MAX_WAIT_MS }
        }.maxOrNull()
    }

    private fun appLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("an app")

    /** Creates the engine if needed; the result completes once it's usable (or not). */
    private fun ensureTts(): CompletableDeferred<Boolean> {
        ttsInit?.let { if (tts != null) return it }
        val init = CompletableDeferred<Boolean>()
        ttsInit = init
        // The callback can run inside the constructor when no engine exists.
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(this) { status ->
            val ok = status == TextToSpeech.SUCCESS
            debug("tts init status=$status ok=$ok")
            if (ok) {
                // Assistant usage ducks music and routes to the active output
                // (e.g. the car's Bluetooth) instead of fighting it.
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = finished(utteranceId)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = finished(utteranceId)
                    override fun onError(utteranceId: String?, errorCode: Int) = finished(utteranceId)
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
                })
            }
            init.complete(ok)
        }
        tts = engine
        return init
    }

    private fun finished(utteranceId: String?) {
        utteranceId?.let { utteranceDone.remove(it)?.complete(Unit) }
    }

    /**
     * A ready engine, creating one if needed; null if it failed or didn't
     * initialize in time, in which case it's dropped so the next message
     * starts a fresh one.
     */
    private suspend fun awaitEngine(): TextToSpeech? {
        val init = ensureTts()
        val ok = withTimeoutOrNull(TTS_INIT_TIMEOUT_MS) { init.await() } ?: false
        if (ok) tts?.let { return it }
        if (ttsInit === init) releaseTts()
        return null
    }

    /**
     * Speaks [text] and suspends until the engine has finished it, so the
     * next queued message is only chosen (and redacted) once this one is
     * done. QUEUE_FLUSH (this app's queue only) so the engine never holds
     * text decided earlier. An engine that refuses the text or never reports
     * completion is presumed dead and released, so the next message gets a
     * fresh one.
     */
    private suspend fun speakAndWait(engine: TextToSpeech, key: String, text: String) {
        val id = "gantry-${utteranceSeq++}"
        val done = CompletableDeferred<Unit>()
        utteranceDone[id] = done
        val queued = runCatching { engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) }
            .getOrDefault(TextToSpeech.ERROR)
        if (queued != TextToSpeech.SUCCESS) {
            utteranceDone.remove(id)
            debug("speak refused; releasing engine")
            if (tts === engine) releaseTts()
            return
        }
        speakingKey = key
        try {
            val finished = withTimeoutOrNull(UTTERANCE_BASE_MS + text.length * UTTERANCE_PER_CHAR_MS) {
                done.await()
            }
            if (finished == null) {
                debug("utterance timed out; releasing engine")
                if (tts === engine) releaseTts()
            }
        } finally {
            utteranceDone.remove(id)
            speakingKey = null
        }
    }

    /** Stop and release the engine; a fresh trip re-reads new arrivals. */
    private fun disarm() {
        clearSpeechQueue()
        spokenHashes.clear()
        releaseTts()
    }

    private fun releaseTts() {
        tts?.let {
            runCatching { it.stop() }
            runCatching { it.shutdown() }
        }
        tts = null
        ttsInit = null
        utteranceDone.values.forEach { it.complete(Unit) }
        utteranceDone.clear()
    }

    private fun clearSpeechQueue() {
        speechQueue.clear()
        runCatching { tts?.stop() }
        speechJob?.cancel()
        speechJob = null
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

        /** Fixed wait after a post when the audio service isn't available. */
        private const val CHIME_GAP_MS = 1_500L

        // Waiting out notification sounds: how long a post's sound gets to
        // start, the overall cap, polling interval, and the quiet gap needed.
        private const val SOUND_START_WAIT_MS = 1_000L
        private const val SOUND_MAX_WAIT_MS = 10_000L
        private const val SOUND_POLL_MS = 100L
        private const val SOUND_PREEXISTING_MS = 5_000L
        private const val SOUND_TAIL_MS = 300L

        // Bounds on waiting for the speech engine: initialization, and one
        // utterance (generous: ~15 chars/s is typical, this allows ~0.2x rate).
        private const val TTS_INIT_TIMEOUT_MS = 10_000L
        private const val UTTERANCE_BASE_MS = 15_000L
        private const val UTTERANCE_PER_CHAR_MS = 300L

        private val DISCONNECT_WORDS =
            Regex("disconnect|stop|turn off|pause|quit", RegexOption.IGNORE_CASE)
    }
}
