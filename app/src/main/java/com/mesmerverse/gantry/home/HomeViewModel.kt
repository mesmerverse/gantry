package com.mesmerverse.gantry.home

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager
import com.mesmerverse.gantry.tailnet.TailnetMonitor
import com.mesmerverse.gantry.tailnet.TailnetSettings
import com.mesmerverse.gantry.tailnet.TailnetState
import com.mesmerverse.gantry.tailnet.TailnetStatus
import com.mesmerverse.gantry.apps.AppEntry
import com.mesmerverse.gantry.apps.AppRepository
import com.mesmerverse.gantry.apps.AppStyleRepository
import com.mesmerverse.gantry.apps.AppTag
import com.mesmerverse.gantry.apps.IconPackInfo
import com.mesmerverse.gantry.apps.IconPackRepository
import com.mesmerverse.gantry.apps.IconStyle
import com.mesmerverse.gantry.apps.PrivateSpace
import com.mesmerverse.gantry.apps.PrivateSpaceRepository
import com.mesmerverse.gantry.apps.TagsRepository
import com.mesmerverse.gantry.apps.orderedApps
import com.mesmerverse.gantry.btc.BtcRepository
import com.mesmerverse.gantry.btc.BtcUi
import com.mesmerverse.gantry.btc.TickerSettings
import com.mesmerverse.gantry.media.MediaNotificationListener
import com.mesmerverse.gantry.media.NotifInfo
import com.mesmerverse.gantry.media.MediaRepository
import com.mesmerverse.gantry.media.MediaState
import com.mesmerverse.gantry.settings.ReadAloudScope
import com.mesmerverse.gantry.settings.SettingsRepository
import com.mesmerverse.gantry.settings.TextMode
import com.mesmerverse.gantry.settings.WidgetId
import com.mesmerverse.gantry.settings.WidgetPrefs
import com.mesmerverse.gantry.settings.WidgetSettingsRepository
import com.mesmerverse.gantry.weather.GeoPlace
import com.mesmerverse.gantry.weather.WeatherRepository
import com.mesmerverse.gantry.weather.WeatherSettings
import com.mesmerverse.gantry.weather.WeatherUi
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockState(val time: String = "", val date: String = "")

/** A bonded Bluetooth device offered for read-aloud auto-arm. */
data class BtDevice(val address: String, val name: String)

data class BatteryState(val percent: Int, val charging: Boolean)

sealed interface PlaceSearch {
    data object Idle : PlaceSearch
    data object Searching : PlaceSearch
    data class Done(val results: List<GeoPlace>) : PlaceSearch
    data object Failed : PlaceSearch
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val appRepository = AppRepository(application)
    private val tagsRepository = TagsRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val btcRepository = BtcRepository(application)
    private val appStyleRepository = AppStyleRepository(application)
    private val iconPackRepository = IconPackRepository(application)
    private val privateSpaceRepository = PrivateSpaceRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val widgetSettingsRepository = WidgetSettingsRepository(application)
    private val mediaRepository = MediaRepository(application)

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

    val clockFormat: StateFlow<String> = settingsRepository.clockFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setClockFormat(format: String) {
        viewModelScope.launch { settingsRepository.setClockFormat(format) }
    }

    /** Ticks on the minute so recomposition happens exactly when the clock changes. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val clock: StateFlow<ClockState> = settingsRepository.clockFormat
        .flatMapLatest { format ->
            val pattern = when (format) {
                "12" -> "h:mm"
                "24" -> "H:mm"
                else -> if (android.text.format.DateFormat.is24HourFormat(getApplication())) {
                    "H:mm"
                } else {
                    "h:mm"
                }
            }
            val timeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            flow {
                while (true) {
                    val now = LocalDateTime.now()
                    emit(
                        ClockState(
                            time = now.format(timeFormatter),
                            date = now.format(dateFormatter),
                        )
                    )
                    delay(60_000L - (System.currentTimeMillis() % 60_000L))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ClockState())

    /** Battery level + charging state from the sticky ACTION_BATTERY_CHANGED broadcast. */
    val battery: StateFlow<BatteryState?> = callbackFlow {
        fun parse(intent: Intent?): BatteryState? {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level < 0 || scale <= 0) return null
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            return BatteryState(
                percent = level * 100 / scale,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                parse(intent)?.let { trySend(it) }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            getApplication(),
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // The sticky intent is returned synchronously on registration.
        parse(sticky)?.let { trySend(it) }
        awaitClose { getApplication<Application>().unregisterReceiver(receiver) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * True when the system wallpaper is light enough for dark text. Follows
     * live wallpaper changes via WallpaperManager's color-hint callback.
     */
    val wallpaperSupportsDarkText: StateFlow<Boolean> = callbackFlow {
        val wallpaperManager = WallpaperManager.getInstance(getApplication())
        fun supportsDarkText(): Boolean {
            val hints = wallpaperManager
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.colorHints ?: 0
            return hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        }
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) trySend(supportsDarkText())
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        trySend(supportsDarkText())
        awaitClose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _allApps = MutableStateFlow<List<AppEntry>>(emptyList())

    /**
     * Raw apps with icon-pack bitmaps swapped in (global pack mapping plus
     * per-app manual picks). Recomputed from the raw list, so changing or
     * removing a pack always restarts from the system icons.
     */
    private val packedApps: Flow<List<AppEntry>> =
        combine(
            _allApps,
            settingsRepository.iconPack,
            appStyleRepository.styles,
        ) { apps, pack, styles ->
            iconPackRepository.applyPacks(apps, pack, styles)
        }

    /**
     * Every launchable app with user overrides (name, icon style) applied and
     * NO visibility filtering. Feeds the dock (explicit pins stay put whatever
     * the airlock is doing) and key-based lookups, which must resolve hidden
     * apps too (menu sheet, gesture/widget targets, pending airlock launches).
     */
    val styledApps: StateFlow<List<AppEntry>> =
        combine(packedApps, appStyleRepository.styles) { apps, styles ->
            apps
                .map { app ->
                    val style = styles[app.key] ?: return@map app
                    app.copy(
                        label = style.label ?: app.baseLabel,
                        iconStyle = style.icon,
                    )
                }
                .sortedBy { it.label.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * [styledApps] minus what the airlock settings hide — drawer, search,
     * home list, and folders all at once. The two settings are independent:
     * show-sealed decides whether sealed airlock apps are listed; focus
     * hides everything else while the airlock is open.
     */
    val allApps: StateFlow<List<AppEntry>> =
        combine(
            styledApps,
            privateSpaceRepository.state,
            settingsRepository.showSealedAirlockApps,
            settingsRepository.airlockFocus,
        ) { apps, ps, showSealed, airlockFocus ->
            apps.filter { app ->
                if (ps == null) return@filter true
                if (app.user == ps.profile) {
                    ps.unlocked || (showSealed && !ps.entryHidden)
                } else {
                    !(airlockFocus && ps.unlocked)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All tags, reserved (Home, Dock) first. */
    val tags: StateFlow<List<AppTag>> = tagsRepository.tags
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun appsTagged(tagName: String): StateFlow<List<AppEntry>> =
        combine(allApps, tags) { apps, tags ->
            val tag = tags.firstOrNull { it.name == tagName }
            if (tag == null) emptyList() else orderedApps(apps, tag)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    /** Only the apps the user tagged "Home". */
    val homeApps: StateFlow<List<AppEntry>> = appsTagged(TagsRepository.HOME)

    /**
     * Apps pinned by a reserved tag, from the unfiltered list: pins are
     * deliberate, so they stay visible whatever the airlock settings hide
     * elsewhere (a sealed pin still unlocks-and-launches) — except when the
     * system hides a sealed private space, which no pin may give away.
     */
    private fun pinnedApps(tagName: String): StateFlow<List<AppEntry>> =
        combine(styledApps, tags, privateSpaceRepository.state) { apps, tags, ps ->
            val tag = tags.firstOrNull { it.name == tagName }
            if (tag == null) {
                emptyList()
            } else {
                orderedApps(apps, tag).filterNot { ps?.concealed == true && it.user == ps.profile }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    /** Apps in the floating dock, icon-only. */
    val dockApps: StateFlow<List<AppEntry>> = pinnedApps(TagsRepository.DOCK)

    /** The corner shortcut beside the dock: the first "Corner"-tagged app. */
    val cornerApps: StateFlow<List<AppEntry>> = pinnedApps(TagsRepository.CORNER)

    /**
     * While the airlock is open: normal app key → the airlock copy of the
     * same activity. Dock and corner pins launch through this, so an open
     * airlock reaches its own copy without re-pinning. Empty while sealed.
     */
    val airlockCopies: StateFlow<Map<String, AppEntry>> =
        combine(styledApps, privateSpaceRepository.state) { apps, ps ->
            if (ps == null || !ps.unlocked) return@combine emptyMap()
            val copies = apps
                .filter { it.user == ps.profile && it.shortcutId == null }
                .associateBy { it.componentName }
            apps.filter { it.user != ps.profile && it.shortcutId == null }
                .mapNotNull { app -> copies[app.componentName]?.let { app.key to it } }
                .toMap()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    /** Tailnet health, checked every minute while home is visible. */
    val tailnet: StateFlow<TailnetState> =
        TailnetMonitor.watch(application, foreground = true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TailnetState())

    val tailnetSettings: StateFlow<TailnetSettings> = TailnetMonitor.settings(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, TailnetSettings())

    /**
     * Tap on the VPN indicator. Nothing connected: bring back the last-used
     * (or chosen) VPN — Tailscale directly; others open to connect. Another VPN connected:
     * disconnect it through its notification, else open it. Tailscale
     * connected: restart when hung, otherwise check now.
     */
    fun vpnTap() {
        val context = getApplication<Application>()
        val state = tailnet.value
        when (state.status) {
            TailnetStatus.Off -> {
                val target = tailnetSettings.value.connectTarget
                if (target == TailnetMonitor.TAILSCALE &&
                    TailnetMonitor.installed(context, target)
                ) {
                    TailnetMonitor.connect(context)
                } else {
                    openVpnApp(target)
                }
            }
            TailnetStatus.Connected -> {
                val owner = state.vpnPackage
                if (owner == null || !TailnetMonitor.disconnect(context, owner)) {
                    openVpnApp(owner ?: tailnetSettings.value.connectTarget)
                }
            }
            TailnetStatus.Down -> TailnetMonitor.fix(context)
            else -> TailnetMonitor.checkNow()
        }
    }

    /** Long-press: the connected VPN's app, else the one a tap would connect. */
    fun openVpnApp(packageName: String? = null) {
        val context = getApplication<Application>()
        val target = packageName ?: tailnet.value.vpnPackage ?: tailnetSettings.value.connectTarget
        TailnetMonitor.launchIntent(context, target)?.let { runCatching { context.startActivity(it) } }
    }

    fun installedVpns(): List<Pair<String, String>> =
        TailnetMonitor.installedVpns(getApplication())

    fun setVpnPreferred(packageName: String) {
        viewModelScope.launch { TailnetMonitor.setPreferred(getApplication(), packageName) }
    }

    fun setTailnetHost(host: String) {
        viewModelScope.launch { TailnetMonitor.setHost(getApplication(), host) }
    }

    fun setTailnetPort(port: Int) {
        viewModelScope.launch { TailnetMonitor.setPort(getApplication(), port) }
    }

    fun setTailnetAutoFix(value: Boolean) {
        viewModelScope.launch { TailnetMonitor.setAutoFix(getApplication(), value) }
    }

    fun toggleTag(tag: String, app: AppEntry) {
        viewModelScope.launch { tagsRepository.toggleApp(tag, app.key) }
    }

    /** Custom tags rendered as folders on the home screen. */
    val homeFolders: StateFlow<Set<String>> = tagsRepository.homeFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleHomeFolder(tag: String) {
        viewModelScope.launch { tagsRepository.toggleHomeFolder(tag) }
    }

    /** Folders on the Airlock home screen (its own set; see TagsRepository). */
    val airlockHomeFolders: StateFlow<Set<String>> = tagsRepository.airlockHomeFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleAirlockHomeFolder(tag: String) {
        viewModelScope.launch { tagsRepository.toggleAirlockHomeFolder(tag) }
    }

    fun setFolderColor(tag: String, argb: Int?) {
        viewModelScope.launch { tagsRepository.setFolderColor(tag, argb) }
    }

    fun addTag(name: String, alsoTagApp: AppEntry? = null) {
        viewModelScope.launch { tagsRepository.addTag(name, alsoTagApp?.key) }
    }

    /** Current conditions; refetches once the cached reading outlives its refresh period. */
    val weather: StateFlow<WeatherUi> = weatherRepository.weather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeatherUi.Hidden)

    val weatherSettings: StateFlow<WeatherSettings> = weatherRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeatherSettings())

    /** State of the city search in the weather dialog. */
    val placeSearch = MutableStateFlow<PlaceSearch>(PlaceSearch.Idle)

    fun setWeatherEnabled(enabled: Boolean) {
        viewModelScope.launch { weatherRepository.setEnabled(enabled) }
    }

    fun searchPlace(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            placeSearch.value = PlaceSearch.Searching
            placeSearch.value = runCatching { weatherRepository.geocode(query) }
                .fold(
                    onSuccess = { PlaceSearch.Done(it) },
                    onFailure = { PlaceSearch.Failed },
                )
        }
    }

    fun selectPlace(place: GeoPlace) {
        viewModelScope.launch {
            weatherRepository.setPlace(place)
            placeSearch.value = PlaceSearch.Idle
        }
    }

    fun clearPlaceSearch() {
        placeSearch.value = PlaceSearch.Idle
    }

    fun setWeatherUseDeviceLocation(value: Boolean) {
        viewModelScope.launch { weatherRepository.setUseDeviceLocation(value) }
    }

    fun setWeatherRefreshMinutes(minutes: Int) {
        viewModelScope.launch { weatherRepository.setRefreshMinutes(minutes) }
    }

    fun clearWeatherPlace() {
        viewModelScope.launch { weatherRepository.clearPlace() }
    }

    /** BTC spot price; refetches on subscription and every 15 min. */
    val btc: StateFlow<BtcUi> = btcRepository.price
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BtcUi.Hidden)

    val btcEnabled: StateFlow<Boolean> = btcRepository.enabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setBtcEnabled(enabled: Boolean) {
        viewModelScope.launch { btcRepository.setEnabled(enabled) }
    }

    /**
     * Now-playing session; null without notification access or active media.
     * Once a session has been seen playing, its paused state stays visible
     * for as long as the session survives — until the app shuts playback
     * down or is killed, when the repository emits null. A session that is
     * already paused when we attach (e.g. a dormant app) never shows at all.
     */
    val media: StateFlow<MediaState?> = mediaRepository.state
        .runningFold<MediaState?, Pair<MediaState?, Boolean>>(null to false) { (previous, played), current ->
            current to when {
                current == null -> false
                current.playing -> true
                previous?.packageName == current.packageName -> played
                else -> false
            }
        }
        .map { (state, played) -> state?.takeIf { played } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun mediaPlayPause() = mediaRepository.playPause()
    fun mediaNext() = mediaRepository.next()
    fun mediaPrevious() = mediaRepository.previous()
    fun mediaRewind() = mediaRepository.rewind()
    fun mediaFastForward() = mediaRepository.fastForward()

    /** "userHash/package" -> what that app has waiting (count + preview + items). */
    val notifications: StateFlow<Map<String, NotifInfo>> = MediaNotificationListener.notifications

    fun dismissNotification(key: String) {
        MediaNotificationListener.instance?.dismiss(key)
    }

    fun dismissApp(pkgKey: String) {
        val items = notifications.value[pkgKey]?.items ?: return
        val listener = MediaNotificationListener.instance ?: return
        items.flatMap { it.keys }.forEach { listener.dismiss(it) }
    }

    /** Dismiss every copy of a collapsed message. */
    fun dismissKeys(keys: List<String>) {
        val listener = MediaNotificationListener.instance ?: return
        keys.forEach { listener.dismiss(it) }
    }

    /** Fire a notification action; a reply action sends [replyText]. */
    fun sendNotificationAction(itemKey: String, actionIndex: Int, replyText: String? = null) {
        MediaNotificationListener.instance?.sendAction(itemKey, actionIndex, replyText)
    }

    /**
     * Tap a notification: fire its content intent so the app opens the exact
     * item (conversation, email…). We send it ourselves — from the foreground
     * with an explicit background-activity-launch allowance — so it isn't
     * silently blocked the way a listener-service send is. Falls back to a
     * plain app launch when there's no content intent or the send is refused.
     */
    fun openNotification(itemKey: String, pkgKey: String) {
        val keys = notifications.value[pkgKey]?.items
            ?.firstOrNull { it.key == itemKey }?.keys ?: listOf(itemKey)
        val pending = MediaNotificationListener.instance?.contentIntentFor(itemKey)
        val opened = pending != null && runCatching {
            val options = android.app.ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            pending.send(getApplication(), 0, null, null, null, null, options.toBundle())
        }.isSuccess
        if (!opened) launchAppForPkgKey(pkgKey)
        // Clear the tapped notification (all its copies), like the shade does.
        dismissKeys(keys)
    }

    fun launchAppForPkgKey(pkgKey: String) {
        styledApps.value.firstOrNull {
            it.shortcutId == null &&
                "${it.user.hashCode()}/${it.componentName.packageName}" == pkgKey
        }?.let { launchApp(it) }
    }

    /** Private space handle + lock state; null unless Gantry is default launcher. */
    val privateSpace: StateFlow<PrivateSpace?> = privateSpaceRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePrivateSpace() {
        val current = privateSpace.value ?: return
        viewModelScope.launch {
            privateSpaceRepository.setLocked(locked = current.unlocked, profile = current.profile)
        }
    }

    // App key waiting for the airlock to open, with the request time so a
    // cancelled auth prompt can't trigger a surprise launch much later.
    private var pendingAirlockLaunch: Pair<String, Long>? = null

    /**
     * Launches an app. Tapping a sealed-airlock app first requests the
     * system unlock, then launches it once the profile comes online.
     */
    fun launchApp(app: AppEntry) {
        val ps = privateSpace.value
        if (ps != null && app.user == ps.profile && !ps.unlocked) {
            pendingAirlockLaunch = app.key to System.currentTimeMillis()
            viewModelScope.launch {
                privateSpaceRepository.setLocked(locked = false, profile = ps.profile)
            }
        } else {
            appRepository.launch(app)
        }
    }

    /** Unpins a shortcut and drops it from the list right away. */
    fun removeShortcut(app: AppEntry) {
        viewModelScope.launch {
            appRepository.unpinShortcut(app)
            reloadApps()
        }
    }

    val leftHanded: StateFlow<Boolean> = settingsRepository.leftHanded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setLeftHanded(value: Boolean) {
        viewModelScope.launch { settingsRepository.setLeftHanded(value) }
    }

    val textMode: StateFlow<TextMode> = settingsRepository.textMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, TextMode.Auto)

    val customTextColor: StateFlow<Int?> = settingsRepository.customTextColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fontScale: StateFlow<Float> = settingsRepository.fontScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

    /** Package name of the global icon pack; null = system icons. */
    val iconPack: StateFlow<String?> = settingsRepository.iconPack
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setIconPack(packageName: String?) {
        viewModelScope.launch { settingsRepository.setIconPack(packageName) }
    }

    val fontId: StateFlow<String> = settingsRepository.fontId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "space_grotesk")

    val backgroundVersion: StateFlow<Int> = settingsRepository.backgroundVersion
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val backgroundColor: StateFlow<Int?> = settingsRepository.backgroundColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Per-widget enable state and tap overrides. */
    val widgets: StateFlow<Map<WidgetId, WidgetPrefs>> = widgetSettingsRepository.widgets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val accentColor: StateFlow<Int?> = settingsRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val showStatusBar: StateFlow<Boolean> = settingsRepository.showStatusBar
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAccentColor(argb: Int?) {
        viewModelScope.launch { settingsRepository.setAccentColor(argb) }
    }

    fun setShowStatusBar(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowStatusBar(value) }
    }

    // Initial true so the tour never flashes before DataStore has loaded;
    // a real stored false (first run) arrives and starts it.
    val tourSeen: StateFlow<Boolean> = settingsRepository.tourSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setTourSeen() {
        viewModelScope.launch { settingsRepository.setTourSeen() }
    }

    val notificationPanelTop: StateFlow<Boolean> = settingsRepository.notificationPanelTop
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setNotificationPanelTop(value: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationPanelTop(value) }
    }

    val notificationPreviews: StateFlow<Boolean> = settingsRepository.notificationPreviews
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setNotificationPreviews(value: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationPreviews(value) }
    }

    val showSealedAirlockApps: StateFlow<Boolean> = settingsRepository.showSealedAirlockApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setShowSealedAirlockApps(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowSealedAirlockApps(value) }
    }

    val airlockFocus: StateFlow<Boolean> = settingsRepository.airlockFocus
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAirlockFocus(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAirlockFocus(value) }
    }

    /** Read-aloud "armed" state; toggled by the read-aloud glyph. */
    val readAloudEnabled: StateFlow<Boolean> = settingsRepository.readAloudEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleReadAloud() {
        viewModelScope.launch { settingsRepository.setReadAloudEnabled(!readAloudEnabled.value) }
    }

    val readAloudScope: StateFlow<ReadAloudScope> = settingsRepository.readAloudScope
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReadAloudScope.MessagesOnly)

    fun setReadAloudScope(scope: ReadAloudScope) {
        viewModelScope.launch { settingsRepository.setReadAloudScope(scope) }
    }

    /** Whether any text-to-speech engine is installed to speak through. */
    fun isTtsAvailable(): Boolean = runCatching {
        getApplication<Application>().packageManager
            .queryIntentServices(
                android.content.Intent(android.speech.tts.TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
                0,
            ).isNotEmpty()
    }.getOrDefault(false)

    /** Bluetooth devices that auto-arm read-aloud on connect (addresses). */
    val autoArmDevices: StateFlow<Set<String>> = settingsRepository.autoArmDevices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleAutoArmDevice(address: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoArmDevice(address, enabled) }
    }

    fun hasBluetoothPermission(): Boolean =
        getApplication<Application>().checkSelfPermission(
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Bonded (paired) devices to choose from; empty without the permission. */
    fun bondedBtDevices(): List<BtDevice> = runCatching {
        val context = getApplication<Application>()
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val manager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        manager?.adapter?.bondedDevices
            ?.map { BtDevice(it.address, it.name ?: it.address) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }.getOrDefault(emptyList())

    val gestureLeft: StateFlow<String> = settingsRepository.gestureLeft
        .stateIn(viewModelScope, SharingStarted.Eagerly, "camera")

    val gestureRight: StateFlow<String> = settingsRepository.gestureRight
        .stateIn(viewModelScope, SharingStarted.Eagerly, "settings")

    fun setGesture(left: Boolean, action: String) {
        viewModelScope.launch { settingsRepository.setGesture(left, action) }
    }

    val dockRightKey: StateFlow<String?> = settingsRepository.dockRightApp
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Pins [app] to the chosen side of the rocket. Only the right slot is
     * stored: sending an app left means the other shown pin (if any) takes
     * the right slot, so a lone pin can still live on either side.
     */
    fun setDockSide(app: AppEntry, right: Boolean) {
        viewModelScope.launch {
            val key = if (right) {
                app.key
            } else {
                dockApps.value.take(2).firstOrNull { it.key != app.key }?.key
            }
            settingsRepository.setDockRightApp(key)
        }
    }

    val tickerSettings: StateFlow<TickerSettings> = btcRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, TickerSettings())

    fun setTextMode(mode: TextMode) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun setCustomTextColor(argb: Int) {
        viewModelScope.launch { settingsRepository.setCustomTextColor(argb) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }

    fun setFontId(id: String) {
        viewModelScope.launch { settingsRepository.setFontId(id) }
    }

    fun setBackgroundColor(argb: Int) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            backgroundFile(context).delete()
            settingsRepository.setBackgroundColor(argb)
        }
    }

    fun setWidgetEnabled(id: WidgetId, enabled: Boolean) {
        viewModelScope.launch { widgetSettingsRepository.setEnabled(id, enabled) }
    }

    fun setWidgetTapApp(id: WidgetId, appKey: String?) {
        viewModelScope.launch { widgetSettingsRepository.setTapApp(id, appKey) }
    }

    fun setTickerAsset(asset: String) {
        viewModelScope.launch { btcRepository.setAsset(asset) }
    }

    fun setTickerCurrency(currency: String) {
        viewModelScope.launch { btcRepository.setCurrency(currency) }
    }

    fun setTickerRefreshMinutes(minutes: Int) {
        viewModelScope.launch { btcRepository.setRefreshMinutes(minutes) }
    }

    fun setTickerCustomEnabled(enabled: Boolean) {
        viewModelScope.launch { btcRepository.setCustomEnabled(enabled) }
    }

    fun setTickerCustomField(field: String, value: String) {
        viewModelScope.launch { btcRepository.setCustomField(field, value) }
    }

    /** Launches the app stored under [key], if it still exists. */
    fun launchAppByKey(key: String): Boolean {
        val app = styledApps.value.firstOrNull { it.key == key } ?: return false
        launchApp(app)
        return true
    }

    /**
     * Imports the picked image (kept as the source for later re-adjustment)
     * and switches the background to it. The pick is size-capped, decoded
     * within the background pixel budget, and re-encoded — never stored raw,
     * so no picked file can make every later decode blow up.
     */
    fun setBackgroundImage(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val staged = java.io.File(context.cacheDir, "background_import")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    staged.outputStream().use { input.copyCapped(it) }
                } ?: error("unreadable image")
                val bitmap = decodeBoundedBitmap(staged, IMPORT_TARGET_WIDTH)
                    ?: error("undecodable image")
                try {
                    backgroundSourceFile(context).outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it)
                    }
                } finally {
                    bitmap.recycle()
                }
                backgroundSourceFile(context).copyTo(backgroundFile(context), overwrite = true)
            }.onSuccess { settingsRepository.bumpBackgroundVersion() }
            staged.delete()
        }
    }

    /** Writes the user's pan/zoom crop as the displayed background. */
    fun saveAdjustedBackground(bitmap: android.graphics.Bitmap) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                backgroundFile(context).outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it)
                }
            }.onSuccess { settingsRepository.bumpBackgroundVersion() }
        }
    }

    fun clearBackgroundImage() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            backgroundFile(context).delete()
            backgroundSourceFile(context).delete()
            settingsRepository.clearBackground()
        }
    }

    /** Opens the app that owns the current media session. */
    fun openMediaApp() {
        val packageName = media.value?.packageName ?: return
        // Skip pinned shortcuts: a PWA pinned from the playing app's package
        // must not shadow the app itself.
        val app = styledApps.value.firstOrNull {
            it.shortcutId == null && it.componentName.packageName == packageName
        }
        if (app != null) {
            appRepository.launch(app)
        } else {
            val context = getApplication<Application>()
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { runCatching { context.startActivity(it) } }
        }
    }

    companion object {
        private const val PENDING_LAUNCH_TIMEOUT_MS = 60_000L

        fun backgroundFile(context: android.content.Context) =
            java.io.File(context.filesDir, "background.jpg")

        fun backgroundSourceFile(context: android.content.Context) =
            java.io.File(context.filesDir, "background_src.jpg")
    }

    fun setAppLabel(app: AppEntry, label: String?) {
        viewModelScope.launch { appStyleRepository.setLabel(app.key, label) }
    }

    fun setAppIcon(app: AppEntry, style: IconStyle) {
        viewModelScope.launch { appStyleRepository.setIcon(app.key, style) }
    }

    /**
     * Per-app icon source (see [AppStyle.iconSource]). A manual pick also
     * resets the style to Default so the chosen bitmap actually shows.
     */
    fun setAppIconSource(app: AppEntry, source: String?) {
        viewModelScope.launch {
            appStyleRepository.setIconSource(app.key, source)
            appStyleRepository.setIcon(app.key, IconStyle.Default)
        }
    }

    suspend fun installedIconPacks(): List<IconPackInfo> = iconPackRepository.installedPacks()

    suspend fun iconPackDrawables(packPackage: String): List<String> =
        iconPackRepository.drawableNames(packPackage)

    suspend fun iconPackIcon(packPackage: String, drawableName: String): ImageBitmap? =
        iconPackRepository.iconByName(packPackage, drawableName)

    /** Persists a full drag-and-drop ordering for a tag. */
    fun setTagOrder(tagName: String, orderedKeys: List<String>) {
        viewModelScope.launch { tagsRepository.setOrder(tagName, orderedKeys) }
    }

    fun renameTag(oldName: String, newName: String) {
        viewModelScope.launch { tagsRepository.renameTag(oldName, newName) }
    }

    fun deleteTag(name: String) {
        viewModelScope.launch { tagsRepository.deleteTag(name) }
    }

    /**
     * Refreshes the app list. A failure keeps the previous list instead of
     * escaping into viewModelScope — which would crash the home screen, and
     * on the startup path, crash it at every launch.
     */
    private suspend fun reloadApps() {
        runCatching { appRepository.loadApps() }
            .onSuccess { _allApps.value = it }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
    }

    init {
        viewModelScope.launch {
            reloadApps()
            tagsRepository.migrateAndSeed { defaultDockKeys() }
        }
        viewModelScope.launch {
            appRepository.packageChanges().collect {
                // Installed/updated packages may include the icon pack itself.
                iconPackRepository.clearCache()
                reloadApps()
            }
        }
        // Fire a queued airlock-app launch as soon as the profile is running.
        viewModelScope.launch {
            privateSpace.collect { ps ->
                if (ps?.unlocked == true) {
                    val (key, requestedAt) = pendingAirlockLaunch ?: return@collect
                    pendingAirlockLaunch = null
                    if (System.currentTimeMillis() - requestedAt < PENDING_LAUNCH_TIMEOUT_MS) {
                        // Unfiltered lookup: with sealed apps hidden, the
                        // filtered list may not have recomputed yet when the
                        // profile comes online — the launch must not miss.
                        styledApps.value.firstOrNull { it.key == key }
                            ?.let { appRepository.launch(it) }
                    }
                }
            }
        }
    }

    /** Default dialer + SMS apps in the main profile, as dock seed. */
    private fun defaultDockKeys(): Set<String> {
        val context = getApplication<Application>()
        val packages = setOfNotNull(
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage,
            Telephony.Sms.getDefaultSmsPackage(context),
        )
        val mainUser = Process.myUserHandle()
        return _allApps.value
            .filter { it.user == mainUser && it.componentName.packageName in packages }
            .map { it.key }
            .toSet()
    }
}
