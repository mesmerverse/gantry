package com.mesmerverse.gantry.home

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.mesmerverse.gantry.apps.AppEntry
import com.mesmerverse.gantry.apps.AppTag
import com.mesmerverse.gantry.apps.IconPackInfo
import com.mesmerverse.gantry.apps.IconPackRepository
import com.mesmerverse.gantry.apps.IconStyle
import com.mesmerverse.gantry.apps.PrivateSpace
import com.mesmerverse.gantry.apps.TagsRepository
import com.mesmerverse.gantry.apps.orderedApps
import com.mesmerverse.gantry.btc.BtcUi
import com.mesmerverse.gantry.media.MediaState
import com.mesmerverse.gantry.media.NotifInfo
import com.mesmerverse.gantry.media.NotifItem
import com.mesmerverse.gantry.settings.ReadAloudScope
import com.mesmerverse.gantry.settings.TextMode
import com.mesmerverse.gantry.settings.WidgetId
import com.mesmerverse.gantry.settings.WidgetPrefs
import com.mesmerverse.gantry.tailnet.TailnetMonitor
import com.mesmerverse.gantry.ui.ShieldIcon
import com.mesmerverse.gantry.tailnet.TailnetSettings
import com.mesmerverse.gantry.tailnet.TailnetState
import com.mesmerverse.gantry.tailnet.TailnetStatus
import com.mesmerverse.gantry.ui.TailnetIcon
import com.mesmerverse.gantry.ui.AirlockIcon
import com.mesmerverse.gantry.ui.SpeakerOffIcon
import com.mesmerverse.gantry.ui.SpeakerOnIcon
import com.mesmerverse.gantry.ui.weatherIcon
import com.mesmerverse.gantry.ui.PauseIcon
import com.mesmerverse.gantry.ui.RocketIcon
import com.mesmerverse.gantry.ui.RocketLitIcon
import com.mesmerverse.gantry.ui.SatelliteLitIcon
import com.mesmerverse.gantry.ui.containerStackIcon
import com.mesmerverse.gantry.ui.ForwardIcon
import com.mesmerverse.gantry.ui.ReplayIcon
import com.mesmerverse.gantry.ui.theme.DefaultAccent
import com.mesmerverse.gantry.ui.theme.FontOptions
import com.mesmerverse.gantry.ui.theme.LocalAccentColor
import com.mesmerverse.gantry.ui.theme.GantryFonts
import com.mesmerverse.gantry.weather.GeoPlace
import com.mesmerverse.gantry.weather.WeatherSettings
import com.mesmerverse.gantry.weather.WeatherUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.animation.core.animate
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    darkText: Boolean,
    onOpenCalendar: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val homeApps by viewModel.homeApps.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()
    val dockRightKey by viewModel.dockRightKey.collectAsStateWithLifecycle()
    val cornerApps by viewModel.cornerApps.collectAsStateWithLifecycle()
    val airlockCopies by viewModel.airlockCopies.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val btc by viewModel.btc.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    fun badgeCount(app: AppEntry): Int = notifications[app.notificationKey()]?.count ?: 0
    // Resolves a notification's "userHash/package" key to its app, if listed.
    fun notifApp(pkgKey: String): AppEntry? =
        allApps.firstOrNull { "${it.user.hashCode()}/${it.componentName.packageName}" == pkgKey }
    // Folder or notification list in the shared home panel; null = closed.
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    val notificationPanelTop by viewModel.notificationPanelTop.collectAsStateWithLifecycle()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    // Search is on demand: the dock's magnifier toggles the pill, which
    // arrives focused with the keyboard up. Closing the drawer retires it.
    var searchVisible by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(drawerOpen) { if (!drawerOpen) searchVisible = false }
    var menuAppKey by remember { mutableStateOf<String?>(null) }
    var renameApp by remember { mutableStateOf<AppEntry?>(null) }
    var packIconFor by remember { mutableStateOf<AppEntry?>(null) }
    var addTagFor by remember { mutableStateOf<AppEntry?>(null) }
    var showAddTag by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val leftHanded by viewModel.leftHanded.collectAsStateWithLifecycle()
    val privateSpace by viewModel.privateSpace.collectAsStateWithLifecycle()
    val backgroundVersion by viewModel.backgroundVersion.collectAsStateWithLifecycle()
    val backgroundColor by viewModel.backgroundColor.collectAsStateWithLifecycle()
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val readAloudEnabled by viewModel.readAloudEnabled.collectAsStateWithLifecycle()
    val readAloudScope by viewModel.readAloudScope.collectAsStateWithLifecycle()
    // Re-checked on resume so installing a TTS engine and coming back reflects.
    val ttsAvailable by rememberRefreshedOnResume { viewModel.isTtsAvailable() }

    fun widgetEnabled(id: WidgetId) = widgets[id]?.enabled != false

    // Tap override: a widget launches its configured app, else its default.
    fun widgetTap(id: WidgetId, default: () -> Unit): () -> Unit = {
        val key = widgets[id]?.tapAppKey
        if (key == null || !viewModel.launchAppByKey(key)) default()
    }

    // Settings closes before the drawer (composed later => higher priority).
    BackHandler(enabled = showSettings) { showSettings = false }

    // Guided tour: self-starts on first run, replayable from Settings.
    val tourSeen by viewModel.tourSeen.collectAsStateWithLifecycle()
    var showTour by remember { mutableStateOf(false) }
    LaunchedEffect(tourSeen) { if (!tourSeen) showTour = true }

    val context = LocalContext.current
    val backgroundImage by produceState<ImageBitmap?>(initialValue = null, backgroundVersion) {
        value = if (backgroundVersion > 0) {
            withContext(Dispatchers.IO) {
                decodeBackground(HomeViewModel.backgroundFile(context), targetWidth = 1200)
            }
        } else {
            null
        }
    }

    // Resolve live so the sheet reflects tag/name/icon edits immediately.
    // From the unfiltered list: dock pins can be long-pressed even while the
    // airlock settings hide them from the drawer.
    val styledApps by viewModel.styledApps.collectAsStateWithLifecycle()
    val menuApp = menuAppKey?.let { key -> styledApps.firstOrNull { it.key == key } }

    val gestureLeft by viewModel.gestureLeft.collectAsStateWithLifecycle()
    val gestureRight by viewModel.gestureRight.collectAsStateWithLifecycle()
    fun runGesture(action: String) {
        when {
            action == "camera" -> onOpenCamera()
            action == "settings" -> onOpenSettings()
            action.startsWith("app:") ->
                viewModel.launchAppByKey(action.removePrefix("app:"))
            else -> {}
        }
    }

    val normalHomeFolders by viewModel.homeFolders.collectAsStateWithLifecycle()
    val airlockHomeFolders by viewModel.airlockHomeFolders.collectAsStateWithLifecycle()
    val airlockFocus by viewModel.airlockFocus.collectAsStateWithLifecycle()
    // "Airlock home screen": while the airlock is open with it on, home shows
    // the airlock's own folders and corner instead of the normal profile's.
    val airlockHome = airlockFocus && privateSpace?.unlocked == true
    val homeFolders = if (airlockHome) airlockHomeFolders else normalHomeFolders
    fun toggleShownFolder(name: String) {
        if (airlockHome) viewModel.toggleAirlockHomeFolder(name) else viewModel.toggleHomeFolder(name)
    }
    var showHomeReorder by remember { mutableStateOf(false) }
    // The app menu is shared by home rows, dock, drawer, and folder popups;
    // only home rows offer "Reorder home".
    var menuFromHome by remember { mutableStateOf(false) }
    var folderMenuFor by remember { mutableStateOf<String?>(null) }
    var reorderFolderFor by remember { mutableStateOf<String?>(null) }
    var renameFolderFor by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    // Subtle scrim so text stays legible on busy wallpapers, matched to the
    // adaptive text color (dark text -> light scrim, light text -> dark scrim).
    val scrim = remember(darkText) {
        val edge = if (darkText) Color(0x66FFFFFF) else Color(0x66000000)
        val mid = if (darkText) Color(0x33FFFFFF) else Color(0x33000000)
        Brush.verticalGradient(listOf(edge, mid, edge))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectSwipeUp(thresholdDp = SWIPE_UP_THRESHOLD_DP) { drawerOpen = true }
            }
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val threshold = SWIPE_SIDE_THRESHOLD_DP.dp.toPx()
                        when {
                            total > threshold -> runGesture(gestureRight)
                            total < -threshold -> runGesture(gestureLeft)
                        }
                    },
                ) { _, dragAmount -> total += dragAmount }
            }
            .pointerInput(Unit) {
                // Priority lane above the rocket: a scrollable home list
                // claims every vertical drag, so this window — a thumb's
                // reach above the pill — watches the Initial pass (ahead of
                // the list) and steals upward pulls for the drawer. Taps and
                // downward/sideways drags pass through untouched; everywhere
                // else scrolling keeps priority.
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val nearRocket = !drawerOpen &&
                        down.position.y >= size.height - 210.dp.toPx() &&
                        down.position.y <= size.height - 90.dp.toPx() &&
                        abs(down.position.x - size.width / 2f) <= 90.dp.toPx()
                    if (!nearRocket) return@awaitEachGesture
                    var dx = 0f
                    var dy = 0f
                    var captured = false
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        dx += delta.x
                        dy += delta.y
                        if (!captured) {
                            val slop = viewConfiguration.touchSlop
                            if (dy > slop || (abs(dx) > slop && abs(dx) > abs(dy))) {
                                // Downward or sideways first: not ours.
                                break
                            }
                            if (-dy > slop) captured = true
                        }
                        if (captured) {
                            change.consume()
                            if (!fired && -dy > 40.dp.toPx()) {
                                fired = true
                                drawerOpen = true
                            }
                        }
                    }
                }
            }
    ) {
        // Custom background (image or solid color) under the legibility scrim.
        backgroundImage?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: backgroundColor?.let { argb ->
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(argb))
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
        )
        val folderTags = tags.filter { it.name in homeFolders }
        val homeOrder = tags.firstOrNull { it.name == TagsRepository.HOME }?.order ?: emptyList()
        val homeItems = buildHomeItems(homeApps, folderTags, homeOrder)
        val homeListState = rememberLazyListState()
        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            // Keep the last rows reachable above the floating dock.
            contentPadding = PaddingValues(bottom = 84.dp),
            // A list that fits on screen still claims every vertical drag (and
            // its overscroll stretch eats them too), starving the root Box's
            // swipe-up-for-drawer gesture. Only let the list scroll when it
            // actually has somewhere to go.
            overscrollEffect = null,
            userScrollEnabled = homeListState.canScrollForward ||
                homeListState.canScrollBackward,
        ) {
            // Widgets scroll with the list so a long home list can use the
            // whole screen.
            item(key = "widgets") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // start=24 lands the clock on the same 48dp spine as the
                        // app rows and notification panel below (24 list pad + 24).
                        .padding(start = 24.dp, end = 16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(Modifier.height(20.dp))

                    // Top line: date on the left, read-aloud toggle right-justified.
                    val showReadAloud = widgetEnabled(WidgetId.ReadAloud)
                    if (widgetEnabled(WidgetId.Date) || showReadAloud) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (widgetEnabled(WidgetId.Date)) {
                                Text(
                                    text = clock.date,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clickable(
                                        onClick = widgetTap(WidgetId.Date, onOpenCalendar),
                                    ),
                                )
                            }
                            if (showReadAloud) {
                                Spacer(Modifier.weight(1f))
                                ReadAloudGlyph(
                                    armed = readAloudEnabled,
                                    ttsAvailable = ttsAvailable,
                                    scope = readAloudScope,
                                    onToggle = { viewModel.toggleReadAloud() },
                                    onSetScope = { viewModel.setReadAloudScope(it) },
                                    onOpenTtsSettings = onOpenTtsSettings,
                                )
                            }
                        }
                    }

                    // Big clock, left-justified under the date.
                    if (widgetEnabled(WidgetId.Clock)) {
                        Text(
                            text = clock.time,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.clickable(
                                onClick = widgetTap(WidgetId.Clock, onOpenClock),
                            ),
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Status line: battery, price ticker, weather, and tailnet.
                    val btcState = btc
                    val weatherState = weather
                    // Collected only while enabled: collecting runs the checks.
                    val tailnetState = if (widgetEnabled(WidgetId.Tailnet)) {
                        viewModel.tailnet.collectAsStateWithLifecycle().value
                            .takeUnless { it.status == TailnetStatus.Unavailable }
                    } else {
                        null
                    }
                    if (widgetEnabled(WidgetId.Battery) ||
                        btcState != BtcUi.Hidden ||
                        weatherState != WeatherUi.Hidden ||
                        tailnetState != null
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (widgetEnabled(WidgetId.Battery)) {
                                BatteryIndicator(
                                    state = battery,
                                    modifier = Modifier.clickable(
                                        onClick = widgetTap(WidgetId.Battery, onOpenBatterySettings),
                                    ),
                                )
                            }
                            if (btcState != BtcUi.Hidden) {
                                Text(
                                    text = (btcState as? BtcUi.Current)?.text ?: "--",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (btcState is BtcUi.Current && !btcState.stale) {
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.clickable(
                                        onClick = widgetTap(WidgetId.Ticker) { showSettings = true },
                                    ),
                                )
                            }
                            if (weatherState != WeatherUi.Hidden) {
                                val current = weatherState as? WeatherUi.Current
                                val weatherColor = if (current != null && !current.stale) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(
                                        onClick = widgetTap(WidgetId.Weather) { showSettings = true },
                                    ),
                                ) {
                                    if (current != null) {
                                        Icon(
                                            imageVector = weatherIcon(current.condition),
                                            contentDescription = null,
                                            tint = weatherColor,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Text(
                                        text = current?.temp ?: "--°",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = weatherColor,
                                    )
                                }
                            }
                            tailnetState?.let { state ->
                                TailnetIndicator(
                                    state = state,
                                    preferredIsTailscale = viewModel.tailnetSettings
                                        .collectAsStateWithLifecycle().value
                                        .connectTarget == TailnetMonitor.TAILSCALE,
                                    // Never widgetTap: an "Opens on tap" saved back when
                                    // this widget offered one must not shadow its control.
                                    onClick = { viewModel.vpnTap() },
                                    onLongClick = { viewModel.openVpnApp() },
                                )
                            }
                        }
                    }

                    if (widgetEnabled(WidgetId.Media)) {
                        media?.let { m ->
                            Spacer(Modifier.height(20.dp))
                            MediaWidget(
                                state = m,
                                onOpenApp = widgetTap(WidgetId.Media) { viewModel.openMediaApp() },
                                onPlayPause = { viewModel.mediaPlayPause() },
                                onRewind = { viewModel.mediaRewind() },
                                onForward = { viewModel.mediaFastForward() },
                                onNext = { viewModel.mediaNext() },
                                onPrevious = { viewModel.mediaPrevious() },
                            )
                        }
                    }

                    // 10dp + the first row's own 10dp top padding lands the
                    // widgets→apps gap on the same 20dp rhythm as row-to-row.
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Home notification strip: one line of notifying-app icons and
            // the newest message; the full list opens in the home panel.
            if (widgetEnabled(WidgetId.Notifications) && notifications.isNotEmpty()) {
                item(key = "notif-strip") {
                    NotificationStrip(
                        notifications = notifications,
                        appFor = ::notifApp,
                        onOpen = { sheet = HomeSheet.Notifications(it) },
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                    )
                }
            }

            if (homeApps.isEmpty() && folderTags.isEmpty()) {
                item(key = "hint") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Name the handle as it looks: an open airlock swaps the
                        // rocket for its hatch.
                        Text(
                            text = when {
                                airlockHome -> "Swipe up from the airlock hatch, long-press " +
                                    "an airlock app, and tag it Home"
                                privateSpace?.unlocked == true -> "Swipe up from the airlock " +
                                    "hatch, long-press an app, and tag it Home"
                                else -> "Swipe up from the rocket, long-press an app, and tag it Home"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            items(homeItems, key = { it.token }) { hi ->
                Box(modifier = Modifier.padding(start = 24.dp - HomeRowTouchExtension)) {
                    when (hi) {
                        is HomeItem.AppItem -> {
                            val app = hi.app
                            AppRow(
                                app = app,
                                onClick = { viewModel.launchApp(app) },
                                onLongClick = {
                                    menuFromHome = true
                                    menuAppKey = app.key
                                },
                                isPrivate = app.user == privateSpace?.profile,
                                hasNotification = badgeCount(app) > 0,
                                iconSize = 46.dp,
                                fullWidthTouch = false,
                                touchPadding = HomeRowTouchExtension,
                            )
                        }
                        is HomeItem.FolderItem -> {
                            FolderRow(
                                tag = hi.tag,
                                colorIndex = hi.colorIndex,
                                apps = orderedApps(allApps, hi.tag),
                                privateSpace = privateSpace,
                                notifications = notifications,
                                onOpen = { sheet = HomeSheet.Folder(hi.tag.name) },
                                onLongClick = { folderMenuFor = hi.tag.name },
                            )
                        }
                    }
                }
            }
        }

        // Always-available swipe-up zone along the bottom edge — the app list
        // consumes vertical drags, so this keeps the drawer reachable.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(Unit) {
                    detectSwipeUp(thresholdDp = 40) { drawerOpen = true }
                }
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(),
        ) {
            AppDrawer(
                apps = allApps,
                tags = tags,
                notifications = notifications,
                onLaunch = { app ->
                    viewModel.launchApp(app)
                    drawerOpen = false
                },
                onAppMenu = { menuFromHome = false; menuAppKey = it.key },
                onAddTag = { showAddTag = true },
                onRenameTag = { old, new -> viewModel.renameTag(old, new) },
                onDeleteTag = { viewModel.deleteTag(it) },
                onReorder = { tag, keys -> viewModel.setTagOrder(tag, keys) },
                onReorderHome = {
                    drawerOpen = false
                    showHomeReorder = true
                },
                homeFolders = normalHomeFolders,
                onToggleFolder = { viewModel.toggleHomeFolder(it) },
                airlockHomeFolders = airlockHomeFolders.takeIf { privateSpace != null },
                onToggleAirlockFolder = { viewModel.toggleAirlockHomeFolder(it) },
                onSetFolderColor = { t, c -> viewModel.setFolderColor(t, c) },
                searchFocus = searchFocus,
                searchVisible = searchVisible,
                leftHanded = leftHanded,
                privateSpace = privateSpace,
                onTogglePrivateSpace = { viewModel.togglePrivateSpace() },
                onClose = { drawerOpen = false },
            )
        }

        // Dock renders on top of the drawer so the rocket handle is always
        // visible: swipe up on it to open the drawer, tap it to close.
        Dock(
            apps = dockApps,
            rightKey = dockRightKey,
            airlockCopies = airlockCopies,
            privateSpace = privateSpace,
            notifications = notifications,
            drawerOpen = drawerOpen,
            leftHanded = leftHanded,
            onLaunch = { viewModel.launchApp(it) },
            onLongClick = { menuFromHome = false; menuAppKey = it.key },
            onOpenDrawer = { drawerOpen = true },
            onCloseDrawer = { drawerOpen = false },
            onTogglePrivateSpace = { viewModel.togglePrivateSpace() },
            onSearchClick = { searchVisible = !searchVisible },
            onSettingsClick = { showSettings = true },
        )

        // Corner shortcut: the "Corner"-tagged app behind a Gantry mark — a
        // satellite, one critical control beside the dock on the thumb's
        // side. Hidden while the drawer is open (the rail lives there).
        // On the airlock home only an airlock app tagged Corner qualifies;
        // otherwise an open airlock launches its own copy of the pinned app.
        val cornerPin = if (airlockHome) {
            cornerApps.firstOrNull { it.user == privateSpace?.profile }
        } else {
            cornerApps.firstOrNull()
        }
        cornerPin?.let { pinned ->
            if (!drawerOpen) {
                val corner = if (airlockHome) pinned else airlockCopies[pinned.key] ?: pinned
                val cornerPrivate = corner.user == privateSpace?.profile
                Box(
                    modifier = Modifier
                        .align(if (leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                        // One row above the dock pill (32dp bottom pad + 62dp
                        // tall + 12dp gap) — sharing its row crowded the corner.
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 106.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            // Same material as the dock pill so the two read
                            // as one control cluster.
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            // The satellite keeps its own colours, so an airlock
                            // target shows as an amber ring, like the handle.
                            .then(
                                if (cornerPrivate) Modifier.border(2.dp, ExposedAmber, CircleShape)
                                else Modifier
                            )
                            .combinedClickable(
                                onClick = { viewModel.launchApp(corner) },
                                onLongClick = { menuFromHome = false; menuAppKey = pinned.key },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SatelliteLitIcon,
                            contentDescription = "Launch ${corner.label}",
                            tint = Color.Unspecified,
                            // Sized up so the tilted satellite fills the disc.
                            modifier = Modifier.size(42.dp),
                        )
                    }
                    if (badgeCount(corner) > 0) {
                        NotificationDot(Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }

        // An emptied notification list has nothing left to show.
        LaunchedEffect(sheet, notifications.isEmpty()) {
            if (sheet is HomeSheet.Notifications && notifications.isEmpty()) sheet = null
        }
        HomePanel(
            sheet = sheet,
            onDismiss = { sheet = null },
            fromTop = { it is HomeSheet.Notifications && notificationPanelTop },
            header = { shown ->
                when (shown) {
                    is HomeSheet.Folder -> {
                        val index = folderTags.indexOfFirst { it.name == shown.tag }
                        folderTags.getOrNull(index)?.let { tag ->
                            FolderIcon(
                                colorIndex = index,
                                color = tag.folderColor?.let { Color(it) },
                                size = 28.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = shown.tag,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    is HomeSheet.Notifications -> {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelLarge,
                            color = ExposedAmber,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.dismissKeys(
                                        notifications.values.flatMap { info ->
                                            info.items.flatMap { it.keys }
                                        },
                                    )
                                    sheet = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            },
        ) { shown ->
            when (shown) {
                is HomeSheet.Folder -> {
                    val tag = folderTags.firstOrNull { it.name == shown.tag }
                    if (tag != null) {
                        FolderPanelContent(
                            tag = tag,
                            apps = orderedApps(allApps, tag),
                            privateSpace = privateSpace,
                            notifications = notifications,
                            onLaunch = {
                                sheet = null
                                viewModel.launchApp(it)
                            },
                            onAppMenu = {
                                sheet = null
                                menuFromHome = false
                                menuAppKey = it.key
                            },
                        )
                    }
                }
                is HomeSheet.Notifications -> NotificationList(
                    notifications = notifications,
                    firstPkg = shown.firstPkg,
                    appFor = ::notifApp,
                    onOpen = { itemKey, pkgKey ->
                        sheet = null
                        viewModel.openNotification(itemKey, pkgKey)
                    },
                    onDismiss = { viewModel.dismissKeys(it) },
                    onDismissApp = { viewModel.dismissApp(it) },
                    onAction = { itemKey, index, reply ->
                        viewModel.sendNotificationAction(itemKey, index, reply)
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            // Full-height slide: the page is a sheet, and closes like one.
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            SettingsPanel(
                viewModel = viewModel,
                onDismiss = { showSettings = false },
                onStartTour = {
                    showSettings = false
                    showTour = true
                },
            )
        }

        // Topmost so it can spotlight anything; its scrim passes touches
        // through, keeping the launcher live during the tour.
        if (showTour) {
            TourOverlay(
                drawerOpen = drawerOpen,
                hasAirlock = privateSpace?.concealed == false,
                onRequestDrawer = { drawerOpen = it },
                onFinish = {
                    showTour = false
                    viewModel.setTourSeen()
                },
            )
        }
    }

    if (showHomeReorder) {
        val folderTags = tags.filter { it.name in homeFolders }
        val homeOrder = tags.firstOrNull { it.name == TagsRepository.HOME }?.order ?: emptyList()
        val items = buildHomeItems(homeApps, folderTags, homeOrder).map { hi ->
            when (hi) {
                is HomeItem.AppItem ->
                    ReorderItem(hi.app.key, hi.app.label) { AppIcon(app = hi.app, size = 38.dp) }
                is HomeItem.FolderItem ->
                    ReorderItem(hi.token, hi.tag.name) {
                        FolderIcon(
                            colorIndex = hi.colorIndex,
                            color = hi.tag.folderColor?.let { Color(it) },
                            size = 38.dp,
                        )
                    }
            }
        }
        ReorderScreen(
            title = "Reorder home",
            items = items,
            onCommit = { tokens -> viewModel.setTagOrder(TagsRepository.HOME, tokens) },
            onDismiss = { showHomeReorder = false },
        )
    }

    reorderFolderFor?.let { name ->
        val tag = tags.firstOrNull { it.name == name }
        if (tag == null) {
            reorderFolderFor = null
        } else {
            ReorderScreen(
                title = "Reorder · $name",
                items = orderedApps(allApps, tag).map { app ->
                    ReorderItem(app.key, app.label) { AppIcon(app = app, size = 38.dp) }
                },
                onCommit = { keys -> viewModel.setTagOrder(name, keys) },
                onDismiss = { reorderFolderFor = null },
            )
        }
    }

    folderMenuFor?.let { name ->
        val tag = tags.firstOrNull { it.name == name }
        val colorIndex = tags.filter { it.name in homeFolders }.indexOfFirst { it.name == name }
        if (tag == null) {
            folderMenuFor = null
        } else {
            FolderMenuSheet(
                tag = tag,
                colorIndex = colorIndex.coerceAtLeast(0),
                onSetColor = { viewModel.setFolderColor(name, it) },
                onReorderHome = {
                    folderMenuFor = null
                    showHomeReorder = true
                },
                onReorderApps = {
                    folderMenuFor = null
                    reorderFolderFor = name
                },
                onRename = { renameFolderFor = name; folderMenuFor = null },
                removeLabel = if (airlockHome) "Remove from Airlock home" else null,
                onRemoveFromHome = {
                    toggleShownFolder(name)
                    folderMenuFor = null
                },
                onDismiss = { folderMenuFor = null },
            )
        }
    }

    renameFolderFor?.let { oldName ->
        TagNameDialog(
            title = "Rename folder",
            initialName = oldName,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameTag(oldName, newName)
                renameFolderFor = null
            },
            onDismiss = { renameFolderFor = null },
        )
    }

    menuApp?.let { app ->
        val menuIconPack by viewModel.iconPack.collectAsStateWithLifecycle()
        val menuIconPacks by produceState(initialValue = emptyList<IconPackInfo>()) {
            value = viewModel.installedIconPacks()
        }
        // Side chips only when the app occupies one of the two dock slots;
        // extra Dock-tag apps beyond two don't render, so no side to pick.
        val dockShown = dockApps.take(2)
        val dockRightApp = dockShown.firstOrNull { it.key == dockRightKey } ?: dockShown.getOrNull(1)
        val dockSideRight = when {
            dockShown.none { it.key == app.key } -> null
            else -> app.key == dockRightApp?.key
        }
        AppMenuSheet(
            app = app,
            tags = tags,
            globalPack = menuIconPack,
            hasIconPacks = menuIconPacks.isNotEmpty(),
            onToggleTag = { tag -> viewModel.toggleTag(tag, app) },
            onNewTag = { addTagFor = app },
            dockSideRight = dockSideRight,
            onSetDockSide = { right -> viewModel.setDockSide(app, right) },
            onSetIcon = { style -> viewModel.setAppIcon(app, style) },
            onSetIconSource = { source -> viewModel.setAppIconSource(app, source) },
            onChoosePackIcon = { packIconFor = app },
            onRename = { renameApp = app },
            onReorderHome = if (menuFromHome) {
                {
                    menuAppKey = null
                    showHomeReorder = true
                }
            } else null,
            onAppInfo = {
                viewModel.appRepository.openAppInfo(app)
                menuAppKey = null
            },
            onRemoveShortcut = {
                viewModel.removeShortcut(app)
                menuAppKey = null
            },
            onDismiss = { menuAppKey = null },
        )
    }

    packIconFor?.let { app ->
        val currentPack by viewModel.iconPack.collectAsStateWithLifecycle()
        val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
            value = viewModel.installedIconPacks()
        }
        PackIconPickerSheet(
            app = app,
            packs = packs,
            initialPack = currentPack,
            loadDrawables = { pack -> viewModel.iconPackDrawables(pack) },
            loadIcon = { pack, name -> viewModel.iconPackIcon(pack, name) },
            onSelect = { source ->
                viewModel.setAppIconSource(app, source)
                packIconFor = null
            },
            onDismiss = { packIconFor = null },
        )
    }

    renameApp?.let { app ->
        RenameAppDialog(
            app = app,
            onConfirm = { name ->
                viewModel.setAppLabel(app, name)
                renameApp = null
            },
            onReset = {
                viewModel.setAppLabel(app, null)
                renameApp = null
            },
            onDismiss = { renameApp = null },
        )
    }

    if (showAddTag || addTagFor != null) {
        TagNameDialog(
            title = "New tag",
            initialName = "",
            confirmLabel = "Add",
            onConfirm = { name ->
                viewModel.addTag(name, alsoTagApp = addTagFor)
                showAddTag = false
                addTagFor = null
            },
            onDismiss = {
                showAddTag = false
                addTagFor = null
            },
        )
    }
}

/**
 * Now-playing widget: album art + marquee track line (both tap through to
 * the playing app) with back-15s / play-pause / ahead-30s below. The jump
 * controls seek within the episode; long-pressing one skips a whole
 * track/episode instead.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaWidget(
    state: MediaState,
    onOpenApp: () -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    // Art on the left; title and controls stacked in the remaining space,
    // controls centered between the art and the screen edge.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        state.art?.let { art ->
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenApp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            // Title carries the widget — larger than the static app rows below,
            // since this one is live and interactive.
            Text(
                text = state.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenApp)
                    // Rest readable for a few seconds before (and between) scrolls.
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 3000,
                        repeatDelayMillis = 3000,
                    ),
            )
            state.artist?.takeIf { it.isNotBlank() }?.let { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenApp),
                )
            }
            Row(
                // The controls' touch padding supplies the vertical breathing
                // room and part of the gaps; pull the row left so the first
                // glyph still lines up with the title.
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(x = (-8).dp),
            ) {
                MediaControl(
                    icon = ReplayIcon,
                    seconds = "15",
                    contentDescription = "Back 15 seconds",
                    onClick = onRewind,
                    onLongClick = onPrevious,
                )
                MediaControl(
                    icon = if (state.playing) PauseIcon else Icons.Filled.PlayArrow,
                    contentDescription = if (state.playing) "Pause" else "Play",
                    size = 32.dp,
                    onClick = onPlayPause,
                )
                MediaControl(
                    icon = ForwardIcon,
                    seconds = "30",
                    contentDescription = "Forward 30 seconds",
                    onClick = onForward,
                    onLongClick = onNext,
                )
            }
        }
    }
}

/**
 * One media control: a compact glyph inside a generous circular hit area,
 * with the jump seconds overlaid in the replay/forward ring's center.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    seconds: String? = null,
    size: Dp = 30.dp,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
        )
        if (seconds != null) {
            Text(
                text = seconds,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                // The ring's circle sits a hair below the glyph's center.
                modifier = Modifier.offset(y = 1.dp),
            )
        }
    }
}


@Composable
private fun OptionLabel(text: String) {
    // One step below a group header: smaller (labelMedium 12sp) and muted, so
    // an option label can never be mistaken for a section heading.
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/**
 * Top-level heading inside a settings screen (GLOBAL, TEXT, BACKGROUND…). A
 * bright, tracked, full-strength uppercase overline that clearly outranks the
 * muted [OptionLabel] sub-labels and owns its own vertical rhythm. Kept in
 * [MaterialTheme.colorScheme.onSurface] rather than the accent so it stays
 * legible on both the light and dark surface schemes. Distinct from the shared
 * [SectionLabel], which the app long-press sheet still uses.
 */
@Composable
private fun SettingsGroupHeader(text: String, first: Boolean = false) {
    if (first) {
        Spacer(Modifier.height(4.dp))
    } else {
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
        Spacer(Modifier.height(16.dp))
    }
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/** Decodes the custom background downsampled to roughly the screen width. */
private fun decodeBackground(file: java.io.File, targetWidth: Int): ImageBitmap? =
    if (file.exists()) decodeBoundedBitmap(file, targetWidth)?.asImageBitmap() else null

/** State recomputed on every lifecycle resume (system settings roundtrips). */
@Composable
private fun <T> rememberRefreshedOnResume(compute: () -> T): androidx.compose.runtime.State<T> {
    val state = remember { mutableStateOf(compute()) }
    LifecycleResumeEffect(Unit) {
        state.value = compute()
        onPauseOrDispose { }
    }
    return state
}

// Shared corner radii so the split is a deliberate rule, not incidental:
// full-width rows use one radius, pill chips a rounder one.
private val RowShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(16.dp)

// Home rows hug their icon+label so blank wallpaper stays inert, but a
// hurried tap needs slack: each row's hit area extends this far before the
// icon (the list inset shrinks to match, so nothing moves) and twice this
// far past the label — hurried taps drift trailing-ward.
private val HomeRowTouchExtension = 16.dp

/**
 * Top-row read-aloud control. Tap arms/disarms speaking newly-arrived
 * messages aloud (a once-per-trip toggle for the car); long-press picks the
 * scope. With no TTS engine installed it stays muted and taps route to the
 * system speech settings so setup is one hop away. Accent tint while armed
 * confirms the mode at a glance, like the airlock chip's amber.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadAloudGlyph(
    armed: Boolean,
    ttsAvailable: Boolean,
    scope: ReadAloudScope,
    onToggle: () -> Unit,
    onSetScope: (ReadAloudScope) -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val active = armed && ttsAvailable
    Box {
        Icon(
            imageVector = if (active) SpeakerOnIcon else SpeakerOffIcon,
            contentDescription = if (active) "Reading messages aloud (on)" else "Read messages aloud (off)",
            tint = if (active) LocalAccentColor.current else MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { if (ttsAvailable) onToggle() else onOpenTtsSettings() },
                    onLongClick = { menuOpen = true },
                )
                .padding(4.dp)
                .size(22.dp),
        )
        if (menuOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(LocalDensity.current) { 30.dp.roundToPx() }),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                // Matches FolderPopup — a rounded surface container, not the
                // squared-off default dropdown.
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(240.dp),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ReadAloudMenuRow("Messages only", scope == ReadAloudScope.MessagesOnly) {
                            onSetScope(ReadAloudScope.MessagesOnly); menuOpen = false
                        }
                        ReadAloudMenuRow("All notifications", scope == ReadAloudScope.All) {
                            onSetScope(ReadAloudScope.All); menuOpen = false
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        ReadAloudMenuRow("Text-to-speech settings", selected = false) {
                            onOpenTtsSettings(); menuOpen = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadAloudMenuRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = LocalAccentColor.current,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        // Marks the row as actionable, distinct from a static label.
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Top-level settings categories, each rendered on its own screen. */
/**
 * Pull-down-to-close for a full-screen page: overscroll past the top (or a
 * drag on a grip) moves the page down by [offset]; letting go past the
 * threshold, or flinging down, closes it, otherwise it springs back.
 * Only deliberate drags count — a fling that scrolls back to the top stops
 * there instead of carrying on into a close.
 */
@Stable
private class PullToClose(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val closeAtPx: Float,
    private val onClose: () -> Unit,
) {
    var offset by mutableFloatStateOf(0f)
        private set
    var enabled = true

    fun dragBy(dy: Float) {
        offset = (offset + dy).coerceAtLeast(0f)
    }

    fun release(velocity: Float) {
        if (offset > closeAtPx || (offset > 0f && velocity > 2500f)) {
            onClose()
        } else if (offset > 0f) {
            scope.launch { animate(offset, 0f) { value, _ -> offset = value } }
        }
    }

    val connection = object : NestedScrollConnection {
        // Scrolling back up retracts the pull before the content moves.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && offset > 0f) {
                val used = maxOf(available.y, -offset)
                offset += used
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (enabled && available.y > 0f && source == NestedScrollSource.UserInput) {
                offset += available.y
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            release(available.y)
            return available
        }
    }
}

@Composable
private fun rememberPullToClose(enabled: Boolean = true, onClose: () -> Unit): PullToClose {
    val scope = rememberCoroutineScope()
    val closeAt = with(LocalDensity.current) { 120.dp.toPx() }
    val latest by rememberUpdatedState(onClose)
    return remember { PullToClose(scope, closeAt) { latest() } }.also { it.enabled = enabled }
}

/** Settings sections in page order — grouped by what you're doing, not by code. */
private enum class SettingsSection(val title: String) {
    HomeScreen("Home screen"),
    Notifications("Notifications"),
    Appearance("Appearance"),
    Gestures("Gestures"),
    Vpn("VPN"),
    Privacy("Privacy"),
    About("About"),
}

/**
 * Settings: one scrolling page, every section in order, with jump chips
 * pinned under the title (the current section lit). Closes like a sheet —
 * drag the header down, or pull down from the top of the page — or back.
 */
@Composable
private fun SettingsPanel(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onStartTour: () -> Unit,
) {
    var pickTapAppFor by remember { mutableStateOf<WidgetId?>(null) }
    var showBackgroundEditor by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val sectionTops = remember { mutableStateMapOf<SettingsSection, Int>() }
    val current by remember {
        derivedStateOf {
            sectionTops.entries
                .filter { it.value <= scroll.value + 1 }
                .maxByOrNull { it.value }?.key ?: SettingsSection.entries.first()
        }
    }

    val pull = rememberPullToClose(onClose = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = pull.offset }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 1f))
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // Header: handle, title, jump chips — the drag-to-close grip.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        val tracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { tracker.resetTracking() },
                            onDragEnd = { pull.release(tracker.calculateVelocity().y) },
                            onDragCancel = { pull.release(0f) },
                        ) { change, amount ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            pull.dragBy(amount)
                        }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 10.dp),
                )
                // The chip row follows the page: whichever section you're in,
                // its chip slides to the middle so it's never off-screen.
                val chipScroll = rememberScrollState()
                val chipBounds = remember { mutableStateMapOf<SettingsSection, IntRange>() }
                var chipViewport by remember { mutableIntStateOf(0) }
                val chipPadPx = with(LocalDensity.current) { 20.dp.roundToPx() }
                LaunchedEffect(current, chipViewport) {
                    val bounds = chipBounds[current] ?: return@LaunchedEffect
                    val centre = (bounds.first + bounds.last) / 2
                    chipScroll.animateScrollTo((centre - chipViewport / 2).coerceAtLeast(0))
                }
                Row(
                    modifier = Modifier
                        .onSizeChanged { chipViewport = it.width }
                        .horizontalScroll(chipScroll)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsSection.entries.forEach { entry ->
                        SettingsJumpChip(
                            text = entry.title,
                            selected = entry == current,
                            onClick = {
                                scope.launch {
                                    scroll.animateScrollTo(sectionTops[entry] ?: 0)
                                }
                            },
                            modifier = Modifier.onGloballyPositioned {
                                // Position inside the row's padding; the
                                // scroll offset counts from the padded edge.
                                val x = it.positionInParent().x.toInt() + chipPadPx
                                chipBounds[entry] = x..(x + it.size.width)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(pull.connection)
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp)
            ) {
                SettingsSection.entries.forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier.onGloballyPositioned {
                            sectionTops[entry] = it.positionInParent().y.toInt()
                        },
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(
                                top = if (index == 0) 20.dp else 48.dp,
                                bottom = 12.dp,
                            ),
                        )
                        val pickTapApp: (WidgetId) -> Unit = { pickTapAppFor = it }
                        when (entry) {
                            SettingsSection.HomeScreen -> HomeScreenSettings(viewModel, pickTapApp)
                            SettingsSection.Notifications ->
                                NotificationSettings(viewModel, pickTapApp)
                            SettingsSection.Appearance -> AppearanceSettings(
                                viewModel = viewModel,
                                onAdjustBackground = { showBackgroundEditor = true },
                            )
                            SettingsSection.Gestures -> GestureSettings(viewModel)
                            SettingsSection.Vpn -> VpnSettings(viewModel, pickTapApp)
                            SettingsSection.Privacy -> PrivacySettings(viewModel)
                            SettingsSection.About -> AboutSettings(onStartTour)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (showBackgroundEditor) {
            BackgroundEditor(
                viewModel = viewModel,
                onDone = { showBackgroundEditor = false },
            )
        }
    }

    pickTapAppFor?.let { id ->
        val allApps by viewModel.allApps.collectAsStateWithLifecycle()
        val privateSpace by viewModel.privateSpace.collectAsStateWithLifecycle()
        TapAppPickerSheet(
            widget = id,
            apps = allApps,
            privateSpace = privateSpace,
            onSelect = { app ->
                viewModel.setWidgetTapApp(id, app?.key)
                pickTapAppFor = null
            },
            onDismiss = { pickTapAppFor = null },
        )
    }
}

/** A settings jump chip: scrolls the page to its section. */
@Composable
private fun SettingsJumpChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.secondary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.14f else 0.05f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * Widgets whose tap can be pointed at an app ("Opens on tap"). Not the VPN
 * widget: its tap is its control (connect, disconnect, fix) and long-press
 * already opens the VPN app, so an override would only shadow it.
 */
private val TapConfigurableWidgets = setOf(
    WidgetId.Date, WidgetId.Clock, WidgetId.Battery, WidgetId.Ticker,
    WidgetId.Weather, WidgetId.Media,
)

/**
 * One home component: enable switch, its inline setup while enabled, and —
 * where a tap does something — which app that tap opens.
 */
@Composable
private fun WidgetItem(
    viewModel: HomeViewModel,
    id: WidgetId,
    onPickTapApp: (WidgetId) -> Unit,
    label: String = id.label,
    divider: Boolean = true,
) {
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val prefs = widgets[id] ?: WidgetPrefs(enabled = true, tapAppKey = null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = prefs.enabled,
            onCheckedChange = { viewModel.setWidgetEnabled(id, it) },
        )
    }
    if (prefs.enabled) {
        Column(modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)) {
            when (id) {
                WidgetId.Clock -> ClockConfig(viewModel)
                WidgetId.Weather -> WeatherConfig(viewModel)
                WidgetId.Ticker -> TickerConfig(viewModel)
                WidgetId.Media -> MediaConfig()
                WidgetId.Notifications -> NotificationsConfig(viewModel)
                WidgetId.ReadAloud -> ReadAloudConfig(viewModel)
                WidgetId.Tailnet -> TailnetConfig(viewModel)
                else -> {}
            }
            if (id in TapConfigurableWidgets) {
                val tapLabel = prefs.tapAppKey
                    ?.let { key -> allApps.firstOrNull { it.key == key }?.label }
                    ?: "Default action"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickTapApp(id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Opens on tap",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(text = tapLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    if (divider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
    }
}

/** What's on the home screen, in screen order: clock and date, status row, media. */
@Composable
private fun HomeScreenSettings(viewModel: HomeViewModel, onPickTapApp: (WidgetId) -> Unit) {
    SettingsGroupHeader("Clock & date", first = true)
    WidgetItem(viewModel, WidgetId.Clock, onPickTapApp)
    WidgetItem(viewModel, WidgetId.Date, onPickTapApp, divider = false)
    SettingsGroupHeader("Status row")
    WidgetItem(viewModel, WidgetId.Battery, onPickTapApp)
    WidgetItem(viewModel, WidgetId.Ticker, onPickTapApp)
    WidgetItem(viewModel, WidgetId.Weather, onPickTapApp, divider = false)
    SettingsGroupHeader("Media")
    WidgetItem(viewModel, WidgetId.Media, onPickTapApp, label = "Now playing", divider = false)
}

/** Everything about incoming notifications: strip, list, read-aloud. */
@Composable
private fun NotificationSettings(viewModel: HomeViewModel, onPickTapApp: (WidgetId) -> Unit) {
    val context = LocalContext.current
    val hasAccess by rememberRefreshedOnResume {
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }
    if (!hasAccess) {
        Text(
            text = "Needs notification access. Tap to grant",
            style = MaterialTheme.typography.labelLarge,
            color = ExposedAmber,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                }
                .padding(vertical = 6.dp),
        )
    }
    SettingsGroupHeader("On the home screen", first = true)
    WidgetItem(
        viewModel, WidgetId.Notifications, onPickTapApp,
        label = "Notification strip", divider = false,
    )
    SettingsGroupHeader("Read aloud")
    WidgetItem(viewModel, WidgetId.ReadAloud, onPickTapApp, label = "Read notifications aloud", divider = false)
}

/** VPN status in the header; Tailscale's tailnet check and auto-fix. */
@Composable
private fun VpnSettings(viewModel: HomeViewModel, onPickTapApp: (WidgetId) -> Unit) {
    WidgetItem(viewModel, WidgetId.Tailnet, onPickTapApp, label = "Show VPN status", divider = false)
}

/** Handedness and the home screen's side swipes. */
@Composable
private fun GestureSettings(viewModel: HomeViewModel) {
    val leftHanded by viewModel.leftHanded.collectAsStateWithLifecycle()
    ToggleRow(
        text = "Left-handed mode",
        subtitle = "Alphabet rail and dock controls on the left",
        checked = leftHanded,
        onChange = { viewModel.setLeftHanded(it) },
    )
    Spacer(Modifier.height(10.dp))
    OptionLabel("Swipe gestures")
    val gestureLeft by viewModel.gestureLeft.collectAsStateWithLifecycle()
    val gestureRight by viewModel.gestureRight.collectAsStateWithLifecycle()
    val gestureApps by viewModel.allApps.collectAsStateWithLifecycle()
    val gesturePrivateSpace by viewModel.privateSpace.collectAsStateWithLifecycle()
    var pickGestureIsLeft by remember { mutableStateOf<Boolean?>(null) }

    fun gestureLabel(action: String): String = when {
        action == "camera" -> "Camera"
        action == "settings" -> "System settings"
        action == "none" -> "Nothing"
        action.startsWith("app:") ->
            gestureApps.firstOrNull { it.key == action.removePrefix("app:") }?.label
                ?: "App (uninstalled)"
        else -> action
    }

    GestureRow("Swipe left", gestureLabel(gestureLeft)) { pickGestureIsLeft = true }
    GestureRow("Swipe right", gestureLabel(gestureRight)) { pickGestureIsLeft = false }

    pickGestureIsLeft?.let { isLeft ->
        GesturePickerSheet(
            isLeft = isLeft,
            apps = gestureApps,
            privateSpace = gesturePrivateSpace,
            onSelect = { action ->
                viewModel.setGesture(isLeft, action)
                pickGestureIsLeft = null
            },
            onDismiss = { pickGestureIsLeft = null },
        )
    }
}

/** What's visible (the airlock) and what Gantry can access (permissions). */
@Composable
private fun PrivacySettings(viewModel: HomeViewModel) {
    SettingsGroupHeader("Airlock", first = true)
    val showSealedAirlockApps by viewModel.showSealedAirlockApps.collectAsStateWithLifecycle()
    ToggleRow(
        text = "Show airlock apps while sealed",
        subtitle = "Tap unlocks and launches. Off whenever Android's " +
            "\"Hide private space when locked\" is on",
        checked = showSealedAirlockApps,
        onChange = { viewModel.setShowSealedAirlockApps(it) },
    )
    val airlockFocus by viewModel.airlockFocus.collectAsStateWithLifecycle()
    ToggleRow(
        text = "Airlock home screen",
        subtitle = "While open, show only the airlock's own apps, the folders you " +
            "put on the Airlock home, and an airlock corner app. The dock still " +
            "swaps to airlock copies",
        checked = airlockFocus,
        onChange = { viewModel.setAirlockFocus(it) },
    )
    SettingsGroupHeader("Permissions & access")
    PermissionsSettings()
}

/** The tour, and which Gantry this is. */
@Composable
private fun AboutSettings(onStartTour: () -> Unit) {
    val context = LocalContext.current
    PermissionRow(
        title = "How Gantry works",
        active = null,
        why = "Replay the guided tour of the launcher",
        onClick = onStartTour,
    )
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }
    version?.let {
        Text(
            text = "Gantry $it",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ClockConfig(viewModel: HomeViewModel) {
    val format by viewModel.clockFormat.collectAsStateWithLifecycle()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        listOf("system" to "System", "12" to "12-hour", "24" to "24-hour").forEach { (id, label) ->
            TagChip(
                text = label,
                selected = format == id,
                onClick = { viewModel.setClockFormat(id) },
            )
        }
    }
}

@Composable
private fun WeatherConfig(viewModel: HomeViewModel) {
    val settings by viewModel.weatherSettings.collectAsStateWithLifecycle()
    val search by viewModel.placeSearch.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    LifecycleResumeEffect(Unit) {
        hasLocationPermission =
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    Spacer(Modifier.height(4.dp))
    OptionLabel("Location")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TagChip(
            text = "Search a place",
            selected = !settings.useDeviceLocation,
            onClick = { viewModel.setWeatherUseDeviceLocation(false) },
        )
        TagChip(
            text = "Device location",
            selected = settings.useDeviceLocation,
            onClick = {
                viewModel.setWeatherUseDeviceLocation(true)
                if (!hasLocationPermission) {
                    requestLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
        )
    }
    Spacer(Modifier.height(8.dp))
    if (settings.useDeviceLocation) {
        if (hasLocationPermission) {
            Text(
                text = "Using the device's coarse position — rounded to ~1 km " +
                    "before any request leaves the phone.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            SettingsRow(text = "Grant coarse location access") {
                requestLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    } else {
        Text(
            text = settings.place?.let { "Location: $it" } ?: "No location set",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("City") },
            trailingIcon = {
                IconButton(onClick = { viewModel.searchPlace(query) }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchPlace(query) }),
            modifier = Modifier.fillMaxWidth(),
        )
        when (val s = search) {
            PlaceSearch.Idle -> {}
            PlaceSearch.Searching -> SearchStatus("Searching…")
            PlaceSearch.Failed -> SearchStatus("Search failed — check network")
            is PlaceSearch.Done ->
                if (s.results.isEmpty()) {
                    SearchStatus("No matches")
                } else {
                    s.results.forEach { place ->
                        Text(
                            text = place.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectPlace(place)
                                    query = ""
                                }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
        }
        if (settings.place != null) {
            SettingsRow(text = "Remove location") { viewModel.clearWeatherPlace() }
        }
    }
    RefreshPeriodConfig(minutes = settings.refreshMinutes) {
        viewModel.setWeatherRefreshMinutes(it)
    }
}

// Dropdown choices for how often a network widget refetches.
private val RefreshPeriods = listOf(
    5 to "5 minutes",
    15 to "15 minutes",
    30 to "30 minutes",
    60 to "1 hour",
    180 to "3 hours",
)

/** Picks how often a network widget refetches, styled like the ticker's pickers. */
@Composable
private fun RefreshPeriodConfig(minutes: Int, onSelect: (Int) -> Unit) {
    Spacer(Modifier.height(10.dp))
    DropdownField(
        label = "Refresh every",
        value = RefreshPeriods.firstOrNull { it.first == minutes }?.second ?: "$minutes minutes",
        options = RefreshPeriods.map { it.second },
    ) { picked ->
        RefreshPeriods.firstOrNull { it.second == picked }?.let { onSelect(it.first) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TickerConfig(viewModel: HomeViewModel) {
    val settings by viewModel.tickerSettings.collectAsStateWithLifecycle()

    Spacer(Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DropdownField(
            label = "Asset",
            value = settings.asset,
            options = TICKER_ASSETS,
            modifier = Modifier.weight(1f),
        ) {
            viewModel.setTickerCustomEnabled(false)
            viewModel.setTickerAsset(it)
        }
        DropdownField(
            label = "Currency",
            value = settings.currency,
            options = TICKER_CURRENCIES,
            modifier = Modifier.weight(1f),
        ) { viewModel.setTickerCurrency(it) }
    }
    Spacer(Modifier.height(6.dp))
    ToggleRow(
        text = "Custom override (any JSON API)",
        checked = settings.custom,
        onChange = { viewModel.setTickerCustomEnabled(it) },
    )
    if (settings.custom) {
        // Fokus+-style endpoint value: any HTTPS JSON API, one value out.
        Spacer(Modifier.height(6.dp))
        TickerField("Label", settings.customLabel) {
            viewModel.setTickerCustomField("label", it)
        }
        TickerField("HTTPS URL", settings.customUrl) {
            viewModel.setTickerCustomField("url", it)
        }
        TickerField("JSON path (e.g. bitcoin.usd)", settings.customPath) {
            viewModel.setTickerCustomField("path", it)
        }
        TickerField("Prefix (e.g. $)", settings.customPrefix) {
            viewModel.setTickerCustomField("prefix", it)
        }
        Text(
            text = "Fetches the URL and shows the value at the JSON path — " +
                "any API works: prices, server status, sensor readings…",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    RefreshPeriodConfig(minutes = settings.refreshMinutes) {
        viewModel.setTickerRefreshMinutes(it)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun TickerField(label: String, value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        singleLine = true,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    )
}

/** Font picker: collapsed row and menu items render their own face. */
@Composable
private fun FontDropdown(fontId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = FontOptions.firstOrNull { it.first == fontId }?.second ?: fontId
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RowShape)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RowShape)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GantryFonts[fontId] ?: FontFamily.Default,
                ),
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FontOptions.forEach { (id, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GantryFonts[id] ?: FontFamily.Default,
                            ),
                        )
                    },
                    trailingIcon = if (fontId == id) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = ProtectedGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else null,
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun GestureRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** Picks a swipe action: built-ins first, then any installed app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GesturePickerSheet(
    isLeft: Boolean,
    apps: List<AppEntry>,
    privateSpace: PrivateSpace?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = if (isLeft) "Swipe left opens" else "Swipe right opens",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "camera" to "Camera",
                    "settings" to "System settings",
                    "none" to "Nothing",
                ).forEach { (action, label) ->
                    item(key = action) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(action) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
                item(key = "divider") {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                }
                items(apps, key = { it.key }) { app ->
                    val isPrivate = app.user == privateSpace?.profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect("app:" + app.key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        PrivateRing(isPrivate = isPrivate) {
                            AppIcon(app = app, size = 34.dp)
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPrivate) ExposedAmber else Color.Unspecified,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Picks the global icon pack: system icons first, then installed packs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPackPickerSheet(
    packs: List<IconPackInfo>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Icon pack",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            IconPackRow(
                label = "System icons",
                icon = null,
                selected = selected == null,
                onClick = { onSelect(null) },
            )
            packs.forEach { pack ->
                IconPackRow(
                    label = pack.label,
                    icon = pack.icon,
                    selected = pack.packageName == selected,
                    onClick = { onSelect(pack.packageName) },
                )
            }
            if (packs.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No icon packs installed. Install one — e.g. Arcticons " +
                        "or Lawnicons from F-Droid — and it will show up here.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IconPackRow(
    label: String,
    icon: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(34.dp))
        } else {
            // Gantry's own glyph stands in for "no pack, stock icons".
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = RocketIcon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
            fontWeight = if (selected) FontWeight.SemiBold else null,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Compact dropdown selector styled like the rest of settings. */
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val TICKER_ASSETS =
    listOf("BTC", "ETH", "XMR", "SOL", "DOGE", "LTC", "ADA", "DOT", "XRP", "LINK")
private val TICKER_CURRENCIES = listOf("USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD")

@Composable
private fun NotificationsConfig(viewModel: HomeViewModel) {
    val context = LocalContext.current
    Text(
        text = "Waiting notifications collapse to one line on your home screen: " +
            "the apps' icons and the newest message. Tap it for the full list — " +
            "tap a notification to open it, swipe it aside to clear it. Apps with " +
            "a notification also show a dot on their icon.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(10.dp))
    OptionLabel("List opens from")
    val panelTop by viewModel.notificationPanelTop.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ScopeSegment(
            label = "Bottom",
            selected = !panelTop,
            modifier = Modifier.weight(1f),
        ) { viewModel.setNotificationPanelTop(false) }
        ScopeSegment(
            label = "Top",
            selected = panelTop,
            modifier = Modifier.weight(1f),
        ) { viewModel.setNotificationPanelTop(true) }
    }
    Spacer(Modifier.height(10.dp))
    OptionLabel("Stop the pop-up banners")
    Text(
        text = "The banner that pops in when a notification arrives is Android's, " +
            "not Gantry's. Set an app to Silent and its notifications still show " +
            "here — just without the pop-up or sound. Tap to open Android's " +
            "notification settings.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                // Not a public constant, but the standard notification-settings
                // screen on AOSP/GrapheneOS; fall back to app settings.
                runCatching {
                    context.startActivity(Intent("android.settings.NOTIFICATION_SETTINGS"))
                }.onFailure {
                    runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
            }
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun ReadAloudConfig(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val scope by viewModel.readAloudScope.collectAsStateWithLifecycle()
    val ttsAvailable by rememberRefreshedOnResume { viewModel.isTtsAvailable() }
    Text(
        text = "Adds a speaker toggle to the status row. Tap it to have new " +
            "messages read aloud until you tap it off — meant to arm once at " +
            "the start of a drive. Long-press the toggle to change what it reads.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(10.dp))
    OptionLabel("Reads")
    ScopeSegmentedToggle(scope = scope, onSelect = { viewModel.setReadAloudScope(it) })
    if (!ttsAvailable) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No text-to-speech engine is set — tap to choose one " +
                "(e.g. GrapheneOS Speech Services) in system settings.",
            style = MaterialTheme.typography.labelLarge,
            color = ExposedAmber,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    runCatching {
                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                    }.onFailure {
                        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                }
                .padding(vertical = 6.dp),
        )
    }

    Spacer(Modifier.height(14.dp))
    OptionLabel("Auto-arm on Bluetooth")
    Text(
        text = "Turn read-aloud on automatically when the phone connects to a " +
            "device below (e.g. your car), and off when it disconnects — so a " +
            "drive needs no touch.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(8.dp))

    // Permission is granted on the Permissions & access screen; here we only
    // read its state and, once granted, list the paired devices to pick from.
    val hasBtPermission by rememberRefreshedOnResume { viewModel.hasBluetoothPermission() }
    if (!hasBtPermission) {
        Text(
            text = "Grant Bluetooth under Settings → Privacy → Permissions & access to " +
                "choose devices.",
            style = MaterialTheme.typography.labelLarge,
            color = ExposedAmber,
        )
    } else {
        val autoArm by viewModel.autoArmDevices.collectAsStateWithLifecycle()
        // Bonded set can change (pairing) while this screen is open.
        val devices by rememberRefreshedOnResume { viewModel.bondedBtDevices() }
        if (devices.isEmpty()) {
            Text(
                text = "No paired Bluetooth devices. Pair your car in system " +
                    "settings, then it'll show here.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            devices.forEach { device ->
                ToggleRow(
                    text = device.name,
                    checked = device.address in autoArm,
                    onChange = { viewModel.toggleAutoArmDevice(device.address, it) },
                )
            }
        }
    }
}

@Composable
private fun ScopeSegmentedToggle(scope: ReadAloudScope, onSelect: (ReadAloudScope) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ScopeSegment(
            label = "Messages only",
            selected = scope == ReadAloudScope.MessagesOnly,
            modifier = Modifier.weight(1f),
        ) { onSelect(ReadAloudScope.MessagesOnly) }
        ScopeSegment(
            label = "All notifications",
            selected = scope == ReadAloudScope.All,
            modifier = Modifier.weight(1f),
        ) { onSelect(ReadAloudScope.All) }
    }
}

@Composable
private fun ScopeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun MediaConfig() {
    val context = LocalContext.current
    val hasAccess by rememberRefreshedOnResume {
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }
    if (!hasAccess) {
        Text(
            text = "Needs notification access — tap to grant",
            style = MaterialTheme.typography.labelLarge,
            color = ExposedAmber,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    } catch (_: Exception) {
                    }
                }
                .padding(vertical = 6.dp),
        )
    }
}

/** Picks the app a widget launches on tap; null restores the default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TapAppPickerSheet(
    widget: WidgetId,
    apps: List<AppEntry>,
    privateSpace: PrivateSpace?,
    onSelect: (AppEntry?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "${widget.label} · opens on tap",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                item {
                    Text(
                        text = "Default action",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp),
                    )
                }
                items(apps, key = { it.key }) { app ->
                    val isPrivate = app.user == privateSpace?.profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        PrivateRing(isPrivate = isPrivate) {
                            AppIcon(app = app, size = 34.dp)
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPrivate) ExposedAmber else Color.Unspecified,
                        )
                        if (isPrivate) {
                            Icon(
                                imageVector = AirlockIcon,
                                contentDescription = "Airlock app",
                                tint = ExposedAmber,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettings(viewModel: HomeViewModel, onAdjustBackground: () -> Unit) {
    val textMode by viewModel.textMode.collectAsStateWithLifecycle()
    val customTextColor by viewModel.customTextColor.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val fontId by viewModel.fontId.collectAsStateWithLifecycle()
    val backgroundVersion by viewModel.backgroundVersion.collectAsStateWithLifecycle()
    val backgroundColor by viewModel.backgroundColor.collectAsStateWithLifecycle()
    var showBackgroundPicker by remember { mutableStateOf(false) }
    val pickBackground = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.setBackgroundImage(it)
            // Drop straight into positioning the fresh image.
            onAdjustBackground()
        }
    }

    // ----- Highlight -----
    SettingsGroupHeader("Highlight color", first = true)
    var customHighlight by remember {
        mutableStateOf(
            accentColor != null && HighlightPresets.none { it.toArgb() == accentColor }
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HighlightPresets.forEachIndexed { index, preset ->
            val selected = !customHighlight &&
                if (index == 0) accentColor == null else accentColor == preset.toArgb()
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(preset)
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        shape = CircleShape,
                    )
                    .clickable {
                        customHighlight = false
                        viewModel.setAccentColor(if (index == 0) null else preset.toArgb())
                    },
            )
        }
        TagChip(
            text = "Custom",
            selected = customHighlight,
            onClick = { customHighlight = true },
        )
    }
    if (customHighlight) {
        Spacer(Modifier.height(10.dp))
        ColorPicker(
            color = accentColor?.let { Color(it) } ?: DefaultAccent,
            onChange = { viewModel.setAccentColor(it.toArgb()) },
        )
    }
    // ----- Icons -----
    SettingsGroupHeader("Icons")
    val iconPack by viewModel.iconPack.collectAsStateWithLifecycle()
    val iconPacks by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = viewModel.installedIconPacks()
    }
    var showIconPackPicker by remember { mutableStateOf(false) }
    GestureRow(
        label = "Icon pack",
        value = when {
            iconPack == null -> "System icons"
            else -> iconPacks.firstOrNull { it.packageName == iconPack }?.label
                ?: "Not installed"
        },
    ) { showIconPackPicker = true }
    if (showIconPackPicker) {
        IconPackPickerSheet(
            packs = iconPacks,
            selected = iconPack,
            onSelect = { pack ->
                viewModel.setIconPack(pack)
                showIconPackPicker = false
            },
            onDismiss = { showIconPackPicker = false },
        )
    }

    // ----- Text -----
    SettingsGroupHeader("Text")
    OptionLabel("Color")
    // Tighter spacing keeps all four on one line at the default size; at
    // larger fonts a chip moves down whole instead of breaking mid-word.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextMode.entries.forEach { mode ->
            TagChip(
                text = mode.name,
                selected = textMode == mode,
                onClick = { viewModel.setTextMode(mode) },
            )
        }
    }
    if (textMode == TextMode.Custom) {
        Spacer(Modifier.height(10.dp))
        ColorPicker(
            color = Color(customTextColor ?: 0xFFE8EDF2.toInt()),
            onChange = { viewModel.setCustomTextColor(it.toArgb()) },
        )
    }

    Spacer(Modifier.height(14.dp))
    OptionLabel("Size")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = fontScale,
            onValueChange = { viewModel.setFontScale(it) },
            valueRange = 0.8f..1.3f,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${(fontScale * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
    }

    Spacer(Modifier.height(14.dp))
    OptionLabel("Font")
    FontDropdown(fontId = fontId, onSelect = { viewModel.setFontId(it) })

    // ----- Background -----
    SettingsGroupHeader("Background")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        // Live preview of what's behind the launcher.
        when {
            backgroundVersion > 0 -> {
                val context = LocalContext.current
                val thumb by produceState<ImageBitmap?>(null, backgroundVersion) {
                    value = withContext(Dispatchers.IO) {
                        decodeBackground(HomeViewModel.backgroundFile(context), targetWidth = 200)
                    }
                }
                thumb?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            backgroundColor != null -> Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(backgroundColor!!)),
            )
        }
        Text(
            text = when {
                backgroundVersion > 0 -> "Using: custom image"
                backgroundColor != null -> "Using: solid color"
                else -> "Using: system wallpaper"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    SettingsRow(text = "Choose background image") {
        pickBackground.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    if (backgroundVersion > 0) {
        SettingsRow(text = "Adjust image position", onClick = onAdjustBackground)
    }
    SettingsRow(text = if (showBackgroundPicker) "Hide color picker" else "Use a solid color") {
        showBackgroundPicker = !showBackgroundPicker
    }
    if (showBackgroundPicker) {
        ColorPicker(
            color = Color(backgroundColor ?: 0xFF101418.toInt()),
            onChange = { viewModel.setBackgroundColor(it.toArgb()) },
        )
        Spacer(Modifier.height(8.dp))
    }
    if (backgroundVersion > 0 || backgroundColor != null) {
        SettingsRow(text = "Use system wallpaper") {
            viewModel.clearBackgroundImage()
            showBackgroundPicker = false
        }
    }

    // ----- System bar -----
    SettingsGroupHeader("System bar")
    val showStatusBar by viewModel.showStatusBar.collectAsStateWithLifecycle()
    ToggleRow(
        text = "Show the system status bar",
        checked = showStatusBar,
        onChange = { viewModel.setShowStatusBar(it) },
    )
}

@Composable
private fun ToggleRow(
    text: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        // Top-align so the switch tracks the title line, not the middle of a
        // multi-line label.
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Full-screen pan/pinch positioning for the background image. Works on the
 * kept original (background_src.jpg) and saves the visible crop.
 */
@Composable
private fun BackgroundEditor(viewModel: HomeViewModel, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val context = LocalContext.current
    val backgroundVersion by viewModel.backgroundVersion.collectAsStateWithLifecycle()
    val source by produceState<ImageBitmap?>(initialValue = null, backgroundVersion) {
        value = withContext(Dispatchers.IO) {
            // Backgrounds picked before the original-copy existed only have
            // the displayed crop; adjust from that.
            decodeBackground(HomeViewModel.backgroundSourceFile(context), targetWidth = 1600)
                ?: decodeBackground(HomeViewModel.backgroundFile(context), targetWidth = 1600)
        }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewW = constraints.maxWidth
        val viewH = constraints.maxHeight
        val image = source
        if (image == null) {
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val baseScale = max(
                viewW.toFloat() / image.width,
                viewH.toFloat() / image.height,
            )

            fun clampOffset(candidate: Offset, z: Float): Offset {
                val maxX = ((image.width * baseScale * z) - viewW).coerceAtLeast(0f) / 2f
                val maxY = ((image.height * baseScale * z) - viewH).coerceAtLeast(0f) / 2f
                return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
            }

            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(image) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                            offset = clampOffset(offset + pan, zoom)
                        }
                    }
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            Text(
                text = "Drag to position · pinch to zoom",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TagChip(text = "Cancel", selected = false, onClick = onDone)
                TagChip(
                    text = "Save",
                    selected = true,
                    onClick = {
                        viewModel.saveAdjustedBackground(
                            renderBackgroundCrop(image, viewW, viewH, baseScale, zoom, offset)
                        )
                        onDone()
                    },
                )
            }
        }
    }
}

/** Replicates the editor's on-screen transform into an offline bitmap. */
private fun renderBackgroundCrop(
    image: ImageBitmap,
    viewW: Int,
    viewH: Int,
    baseScale: Float,
    zoom: Float,
    offset: Offset,
): android.graphics.Bitmap {
    val src = image.asAndroidBitmap()
    val out = android.graphics.Bitmap.createBitmap(
        viewW, viewH, android.graphics.Bitmap.Config.ARGB_8888
    )
    val matrix = android.graphics.Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(baseScale * zoom, baseScale * zoom)
        postTranslate(viewW / 2f + offset.x, viewH / 2f + offset.y)
    }
    android.graphics.Canvas(out).drawBitmap(
        src, matrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    )
    return out
}

private val HighlightPresets = listOf(
    DefaultAccent,          // flame amber (default)
    Color(0xFFE2C862),      // gold
    Color(0xFF7FC8C8),      // teal
    Color(0xFFB8A7D9),      // lavender
)

private val ColorPresets = listOf(
    Color(0xFFE8EDF2), Color(0xFF15191E), Color(0xFFF2A65A), Color(0xFF9CC79C),
    Color(0xFF8FB8DE), Color(0xFFD9A7C7), Color(0xFFE2C862), Color(0xFFB0BEC9),
)

/** Preset swatches plus RGB sliders with a live preview. */
@Composable
private fun ColorPicker(color: Color, onChange: (Color) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(preset)
                        .border(
                            width = if (preset == color) 2.dp else 1.dp,
                            color = if (preset == color) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onChange(preset) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp),
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                ChannelSlider("R", color.red) { onChange(color.copy(red = it)) }
                ChannelSlider("G", color.green) { onChange(color.copy(green = it)) }
                ChannelSlider("B", color.blue) { onChange(color.copy(blue = it)) }
            }
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(16.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        )
    }
}

@Composable
private fun PermissionsSettings() {
    val context = LocalContext.current
    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    val appDetailsIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    // Re-checked every resume so granting something in system settings
    // and coming back shows the new state immediately.
    val isDefaultLauncher by rememberRefreshedOnResume {
        runCatching {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) == true
        }.getOrDefault(false)
    }
    val hasNetwork by rememberRefreshedOnResume {
        context.checkSelfPermission(android.Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasNotificationAccess by rememberRefreshedOnResume {
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }
    // Runtime permission (needs the request launcher), unlike the others which
    // are managed in system settings.
    var hasBluetooth by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    LifecycleResumeEffect(Unit) {
        hasBluetooth =
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    val requestBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasBluetooth = granted }

    PermissionRow(
        title = "Default launcher",
        active = isDefaultLauncher,
        why = "Lets Gantry act as your home screen and see, open, and " +
            "seal the airlock (Android's private space). Android hides " +
            "that profile from every other app.",
        onClick = { open(Intent(Settings.ACTION_HOME_SETTINGS)) },
    )
    SettingsDivider()
    PermissionRow(
        title = "Network",
        active = hasNetwork,
        why = "Used only for the weather (Open-Meteo) and BTC (Kraken) " +
            "widgets. Revoke it and Gantry runs fully offline.",
        onClick = { open(appDetailsIntent) },
    )
    SettingsDivider()
    PermissionRow(
        title = "Media & notifications (notification access)",
        active = hasNotificationAccess,
        why = "Shows the mini player, per-app dots, and a one-line preview " +
            "of each app's latest notification. Read for display only — " +
            "nothing is stored or sent anywhere.",
        onClick = { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
    )
    SettingsDivider()
    PermissionRow(
        title = "Bluetooth (read-aloud auto-arm)",
        active = hasBluetooth,
        why = "Optional. Lets read-aloud arm automatically when you connect to " +
            "a chosen Bluetooth device (e.g. your car), and lists your paired " +
            "devices to pick from. Connect events only — no scanning or location.",
        onClick = {
            if (hasBluetooth) open(appDetailsIntent)
            else requestBluetooth.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        },
    )
    SettingsDivider()
    PermissionRow(
        title = "Other access",
        active = null,
        why = "The system uninstall dialog and launcher app visibility. " +
            "Install-time permissions with no data access.",
        onClick = { open(appDetailsIntent) },
    )
}

@Composable
private fun PermissionRow(
    title: String,
    active: Boolean?,
    why: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = why,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (active != null) {
            Text(
                text = if (active) "Active" else "Off",
                style = MaterialTheme.typography.labelLarge,
                color = if (active) ProtectedGreen else ExposedAmber,
            )
        }
    }
}

private val ProtectedGreen = Color(0xFF9CC79C)

/** User-selectable highlight for airlock/exposure accents. */
private val ExposedAmber: Color
    @Composable get() = LocalAccentColor.current

/**
 * Notifications signal urgency, not a user-chosen accent — a fixed, high-
 * contrast mark keeps them from blending with the airlock/private accent and
 * from vanishing when the accent is pale on a light wallpaper.
 */
private val NotificationMark = Color(0xFFE5484D)

/** Battery outline with live fill level and a charging bolt overlay. */
/** How long the fixed-node dot stays on the tailnet glyph after a restart. */
private const val TAILNET_FIX_NOTE_MS = 5 * 60_000L

/**
 * Tailnet health in the header, by colour alone (a word here wrapped down
 * the screen once the status row ran out of width): content colour when
 * healthy, the highlight colour when there's a problem (pulsing while
 * fixing), dimmed when Tailscale is off or there's no internet. For an hour
 * after a restart, the top node wears the highlight colour: "fixed".
 * Tap connects, restarts, or re-checks; long-press opens Tailscale.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TailnetIndicator(
    state: TailnetState,
    preferredIsTailscale: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val highlight = ExposedAmber
    // Tailscale keeps its mesh; any other VPN (or none, with another app
    // preferred) is a shield.
    val glyph = when {
        state.status == TailnetStatus.Connected -> ShieldIcon
        state.status == TailnetStatus.Off && !preferredIsTailscale -> ShieldIcon
        else -> TailnetIcon
    }
    // Timed, not just computed: nothing else may redraw the header when the
    // window ends, so the dot clears itself.
    val fixedAt = state.lastFixAt
    val healthy = state.status == TailnetStatus.Healthy
    val recentlyFixed by produceState(initialValue = false, fixedAt, healthy) {
        val remaining = fixedAt?.let { it + TAILNET_FIX_NOTE_MS - System.currentTimeMillis() } ?: 0L
        if (healthy && remaining > 0) {
            value = true
            delay(remaining)
        }
        value = false
    }
    val color = when (state.status) {
        TailnetStatus.Healthy, TailnetStatus.Connected -> MaterialTheme.colorScheme.onBackground
        TailnetStatus.Down, TailnetStatus.Fixing -> highlight
        else -> MaterialTheme.colorScheme.secondary
    }
    val vpnName = state.vpnPackage?.let { TailnetMonitor.KNOWN_VPNS[it] } ?: "VPN"
    val description = when (state.status) {
        TailnetStatus.Healthy -> if (recentlyFixed) "Tailnet healthy, recently fixed" else "Tailnet healthy"
        TailnetStatus.Connected -> "$vpnName connected, tap to disconnect"
        TailnetStatus.Down -> "Tailnet down, tap to fix"
        TailnetStatus.Fixing -> "Tailnet reconnecting"
        TailnetStatus.Off -> "No VPN, tap to connect"
        TailnetStatus.Offline -> "No internet"
        else -> "Tailnet checking"
    }
    // Fixing breathes so the header shows work is under way.
    val alpha = if (state.status == TailnetStatus.Fixing) {
        rememberInfiniteTransition(label = "tailnetFixing").animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "tailnetAlpha",
        ).value
    } else {
        1f
    }
    Box(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description }
            .graphicsLayer { this.alpha = alpha },
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        if (recentlyFixed) {
            // Recolour the top node (viewport 12,6 r3 of 24) in place.
            Canvas(modifier = Modifier.size(18.dp)) {
                drawCircle(
                    color = highlight,
                    radius = size.width * 3.3f / 24f,
                    center = Offset(size.width / 2f, size.height * 6f / 24f),
                )
            }
        }
    }
}

/** VPN widget settings: preferred VPN, then Tailscale's check host and auto-fix. */
@Composable
private fun TailnetConfig(viewModel: HomeViewModel) {
    val settings by viewModel.tailnetSettings.collectAsStateWithLifecycle()
    val state by viewModel.tailnet.collectAsStateWithLifecycle()
    val vpns = remember { viewModel.installedVpns() }
    Text(
        text = "Shows whether a VPN is connected: white when it is, dimmed when not. " +
            "Tap with nothing connected to bring back your VPN. Tap a connected " +
            "VPN other than Tailscale to disconnect it. Long-press opens the VPN app.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
    if (vpns.isNotEmpty()) {
        val lastUsedLabel = "Last used" +
            (settings.lastUsed?.let { TailnetMonitor.KNOWN_VPNS[it] }?.let { " ($it)" } ?: "")
        DropdownField(
            label = "Tap connects",
            value = if (settings.preferred == TailnetSettings.LAST_USED) lastUsedLabel
            else TailnetMonitor.KNOWN_VPNS[settings.preferred] ?: "Tailscale",
            options = listOf(lastUsedLabel) + vpns.map { it.second },
        ) { label ->
            if (label == lastUsedLabel) {
                viewModel.setVpnPreferred(TailnetSettings.LAST_USED)
            } else {
                vpns.firstOrNull { it.second == label }?.let { viewModel.setVpnPreferred(it.first) }
            }
        }
        Text(
            text = "Only Tailscale can be connected straight from Gantry; other apps " +
                "open so you can connect there.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
    }
    if (vpns.none { it.first == TailnetMonitor.TAILSCALE }) return
    OptionLabel("Tailscale health check")
    Text(
        text = "Optional, and off until you enter a host. While Tailscale is the " +
            "connected VPN, checks a machine that only answers over your tailnet, " +
            "and that DNS resolves. If either fails, the icon turns your highlight " +
            "colour; tap it to restart Tailscale. A highlighted top node means it " +
            "was fixed in the last 5 minutes.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    TickerField("Check host (tailnet address)", settings.host) { viewModel.setTailnetHost(it) }
    TickerField("Port", settings.port.toString()) { text ->
        text.toIntOrNull()?.let { viewModel.setTailnetPort(it) }
    }
    Text(
        text = "On port 22 the SSH greeting must come back, so the check can't be " +
            "fooled by another machine on the same address range. Nothing is sent.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    ToggleRow(
        text = "Fix automatically",
        subtitle = "Restart Tailscale when the check fails twice while it's connected. " +
            "At most every 10 minutes, backing off if it doesn't help. Checks every " +
            "5 minutes in the background.",
        checked = settings.autoFix,
        onChange = { viewModel.setTailnetAutoFix(it) },
    )
    val status = when (state.status) {
        TailnetStatus.Healthy -> "Healthy"
        TailnetStatus.Down -> "Down"
        TailnetStatus.Fixing -> "Restarting Tailscale"
        TailnetStatus.Off -> "No VPN connected"
        TailnetStatus.Connected ->
            (state.vpnPackage?.let { TailnetMonitor.KNOWN_VPNS[it] } ?: "Another VPN") +
                " connected, not checked"
        TailnetStatus.Offline -> "No internet"
        TailnetStatus.Checking -> "Checking"
        TailnetStatus.Unavailable -> "Unavailable"
    }
    val lastFix = state.lastFixAt?.let {
        val time = android.text.format.DateUtils.getRelativeTimeSpanString(
            it, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS,
        )
        " · last fix $time" + if (state.lastFixAuto) " (automatic)" else ""
    }.orEmpty()
    Text(
        text = status + lastFix,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun BatteryIndicator(state: BatteryState?, modifier: Modifier = Modifier) {
    if (state == null) return
    val outline = LocalContentColor.current
    val fill = when {
        state.charging -> ProtectedGreen
        state.percent <= 20 -> MaterialTheme.colorScheme.error
        else -> outline
    }
    val boltColor = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 25.dp, height = 13.dp)) {
            val stroke = 1.4.dp.toPx()
            val bodyWidth = size.width * 0.88f
            // Outline
            drawRoundRect(
                color = outline,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(bodyWidth - stroke, size.height - stroke),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = stroke),
            )
            // Terminal nub
            drawRoundRect(
                color = outline,
                topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height * 0.3f),
                size = Size(size.width - bodyWidth - 1.dp.toPx(), size.height * 0.4f),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
            // Fill level
            val pad = stroke + 1.2.dp.toPx()
            val fillWidth = (bodyWidth - 2 * pad) * (state.percent / 100f)
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(pad, pad),
                    size = Size(fillWidth, size.height - 2 * pad),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
            // Charging bolt over the body
            if (state.charging) {
                val w = bodyWidth
                val h = size.height
                val bolt = Path().apply {
                    moveTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.34f, h * 0.56f)
                    lineTo(w * 0.5f, h * 0.56f)
                    lineTo(w * 0.44f, h * 0.92f)
                    lineTo(w * 0.66f, h * 0.44f)
                    lineTo(w * 0.5f, h * 0.44f)
                    close()
                }
                drawPath(bolt, color = boltColor)
            }
        }
        Text(
            text = "${state.percent}%",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Floating bar of icon-only shortcuts at the bottom of the home screen. The
 * center handle opens the app drawer on tap and toggles the private space on
 * long-press; it wears an amber ring while the private space is unlocked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.Dock(
    apps: List<AppEntry>,
    rightKey: String?,
    airlockCopies: Map<String, AppEntry>,
    privateSpace: PrivateSpace?,
    notifications: Map<String, NotifInfo>,
    drawerOpen: Boolean,
    leftHanded: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onCloseDrawer: () -> Unit,
    onTogglePrivateSpace: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)
            // Percent-radius keeps the pill a stadium at any height.
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            // Absorb taps on the pill itself (gaps between icons) so they don't
            // fall through to the app list behind the dock.
            .pointerInput(Unit) { detectTapGestures {} }
            // The whole pill is the swipe-up-to-open target — the 44dp rocket
            // alone was too tight a bullseye. Consuming cancels icon taps so
            // a swipe over a shortcut never launches it.
            .pointerInput(drawerOpen) {
                // Swipe up opens; with the drawer open, swipe down closes —
                // the whole pill either way, like opening always was.
                if (!drawerOpen) {
                    detectSwipeUp(thresholdDp = 40, consume = true) { onOpenDrawer() }
                } else {
                    detectSwipeDown(thresholdDp = 40) { onCloseDrawer() }
                }
            }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Two shortcuts max, one per side, so the rocket is always centered.
        // While the drawer is open the slots trade shortcuts for drawer
        // controls — search and settings — matching the handle's own morph.
        // Sides come from dock order (first left, second right) unless the
        // dock-side pref pins an app to the right slot.
        val shown = apps.take(2)
        val rightApp = shown.firstOrNull { it.key == rightKey } ?: shown.getOrNull(1)
        val leftApp = shown.firstOrNull { it !== rightApp }
        // Controls mirror with handedness (like the alphabet rail), so both
        // hands get the same relative layout.
        val searchOnLeft = !leftHanded
        if (shown.isNotEmpty() || drawerOpen) {
            DockMorphSlot(
                drawerOpen = drawerOpen,
                icon = if (searchOnLeft) Icons.Outlined.Search else Icons.Outlined.Settings,
                contentDescription = if (searchOnLeft) "Search apps" else "Gantry settings",
                onClick = if (searchOnLeft) onSearchClick else onSettingsClick,
            ) {
                DockSlot(
                    app = leftApp,
                    airlockCopy = leftApp?.let { airlockCopies[it.key] },
                    privateSpace = privateSpace,
                    notifications = notifications,
                    onLaunch = onLaunch,
                    onLongClick = onLongClick,
                )
            }
        }
        // Drawer handle, centered between the shortcuts. Resting state is the
        // rocket ("swipe up to launch"); an open airlock swaps in the amber
        // hatch. Opening is swipe-only; a tap only ever closes.
        val unlocked = privateSpace?.unlocked == true
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                .then(
                    if (unlocked) Modifier.border(2.dp, ExposedAmber, CircleShape) else Modifier
                )
                .combinedClickable(
                    onClick = { if (drawerOpen) onCloseDrawer() },
                    onLongClick = if (privateSpace != null) onTogglePrivateSpace else null,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    drawerOpen -> RocketLitIcon
                    unlocked -> AirlockIcon
                    else -> RocketLitIcon
                },
                contentDescription = when {
                    drawerOpen -> "Close app list"
                    unlocked -> "All apps — airlock open, swipe up"
                    else -> "All apps — swipe up"
                },
                // The lit rocket carries its own colours; only the hatch tints.
                tint = if (unlocked && !drawerOpen) ExposedAmber else Color.Unspecified,
                // A rocket pointing down to close the app list. Sized to
                // fill its circle like the app icons fill theirs.
                modifier = Modifier
                    .size(if (unlocked && !drawerOpen) 33.dp else 38.dp)
                    .then(if (drawerOpen) Modifier.rotate(180f) else Modifier),
            )
        }
        if (shown.isNotEmpty() || drawerOpen) {
            DockMorphSlot(
                drawerOpen = drawerOpen,
                icon = if (searchOnLeft) Icons.Outlined.Settings else Icons.Outlined.Search,
                contentDescription = if (searchOnLeft) "Gantry settings" else "Search apps",
                onClick = if (searchOnLeft) onSettingsClick else onSearchClick,
            ) {
                DockSlot(
                    app = rightApp,
                    airlockCopy = rightApp?.let { airlockCopies[it.key] },
                    privateSpace = privateSpace,
                    notifications = notifications,
                    onLaunch = onLaunch,
                    onLongClick = onLongClick,
                )
            }
        }
    }
}

/**
 * A dock side slot that crossfades between its home shortcut and a drawer
 * control, styled like the center handle so the open-drawer dock reads as
 * one set of controls.
 */
@Composable
private fun DockMorphSlot(
    drawerOpen: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    appSlot: @Composable () -> Unit,
) {
    Crossfade(targetState = drawerOpen, label = "dockSlot") { open ->
        if (open) {
            // Match DockSlot's PrivateRing footprint (3dp all around) so the
            // dock pill keeps its exact size while the slots morph.
            Box(modifier = Modifier.padding(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else {
            appSlot()
        }
    }
}

@Composable
private fun DockSlot(
    app: AppEntry?,
    airlockCopy: AppEntry?,
    privateSpace: PrivateSpace?,
    notifications: Map<String, NotifInfo>,
    onLaunch: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    if (app == null) {
        Spacer(Modifier.size(44.dp))
    } else {
        // Launch (and show) the airlock copy while it's open; the menu stays
        // on the pinned app, which is what the Dock tag actually holds.
        val target = airlockCopy ?: app
        DockIcon(
            app = target,
            isPrivate = target.user == privateSpace?.profile,
            notificationCount = notifications[target.notificationKey()]?.count ?: 0,
            onClick = { onLaunch(target) },
            onLongClick = { onLongClick(app) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIcon(
    app: AppEntry,
    isPrivate: Boolean,
    notificationCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    PrivateRing(isPrivate = isPrivate) {
        Box {
            AppIcon(
                app = app,
                size = 44.dp,
                modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            )
            if (notificationCount > 0) {
                NotificationDot(Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

/**
 * Horizontal swipe-to-clear: the row follows the finger and fades, committing
 * past a third of its width in either direction; short of that it springs
 * back. Replaces the notification ✕ buttons.
 */
@Composable
private fun SwipeToClear(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    // The gesture loop below runs on pointerInput(Unit) and outlives
    // recompositions; without this, a row reusing this slot after a
    // dismissal would swipe with the PREVIOUS row's onClear — animating,
    // then clearing a notification that's already gone.
    val clear by rememberUpdatedState(onClear)
    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val value = offset.value
                        if (width > 0 && abs(value) > width / 3f) {
                            scope.launch {
                                offset.animateTo(if (value > 0) width.toFloat() else -width.toFloat())
                                clear()
                                offset.snapTo(0f)
                            }
                        } else {
                            scope.launch { offset.animateTo(0f) }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { change, dragAmount ->
                    // Consuming keeps the home screen's own side-swipe
                    // gestures (camera/settings) out of the fight.
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + dragAmount) }
                }
            }
            .graphicsLayer {
                translationX = offset.value
                alpha = if (width > 0) 1f - abs(offset.value) / width else 1f
            },
    ) {
        content()
    }
}

/** Material of every home card: notification strip and the shared panel. */
private val PanelColor = Color(0xF20E0F12)

/** Most app icons the strip shows before collapsing the rest into "+N". */
private const val STRIP_MAX_ICONS = 5

/**
 * Home notification strip: one line however much is waiting — the notifying
 * apps' icons, newest first, then the newest message. Tapping the strip opens
 * the full list; tapping an icon opens it with that app on top.
 */
@Composable
private fun NotificationStrip(
    notifications: Map<String, NotifInfo>,
    appFor: (String) -> AppEntry?,
    onOpen: (firstPkg: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = notifications.entries.sortedByDescending { it.value.latestPost }
    val newest = entries.firstOrNull()?.value?.items?.firstOrNull()
    val shown = entries.take(STRIP_MAX_ICONS)
    val extra = entries.size - shown.size
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PanelColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen(null) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shown.forEach { (pkgKey, info) ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpen(pkgKey) }
                        .padding(5.dp),
                ) {
                    val app = appFor(pkgKey)
                    if (app != null) {
                        AppIcon(app = app, size = 30.dp)
                    } else {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                        )
                    }
                    if (info.count > 1) {
                        Text(
                            text = info.count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(NotificationMark)
                                .padding(horizontal = 5.dp),
                        )
                    }
                }
            }
            if (extra > 0) {
                Text(
                    text = "+$extra",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                text = newest?.preview.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp),
            )
        }
    }
}

/**
 * Every waiting notification, one section per app — the notification
 * panel's body. [firstPkg] (an icon tapped on the strip) goes on top; the
 * rest follow newest-first.
 */
@Composable
private fun NotificationList(
    notifications: Map<String, NotifInfo>,
    firstPkg: String?,
    appFor: (String) -> AppEntry?,
    onOpen: (itemKey: String, pkgKey: String) -> Unit,
    onDismiss: (List<String>) -> Unit,
    onDismissApp: (String) -> Unit,
    onAction: (itemKey: String, actionIndex: Int, replyText: String?) -> Unit,
) {
    val entries = notifications.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, NotifInfo>> { it.key == firstPkg }
                .thenByDescending { it.value.latestPost },
        )
    entries.forEachIndexed { index, (pkgKey, info) ->
        // Keyed so a dismissed row's swipe/expand state can't be inherited
        // by the row that slides into its slot.
        key(pkgKey) {
            NotificationAppRow(
                app = appFor(pkgKey),
                fallbackName = pkgKey.substringAfter('/'),
                info = info,
                onOpen = { itemKey -> onOpen(itemKey, pkgKey) },
                onDismiss = onDismiss,
                onDismissApp = { onDismissApp(pkgKey) },
                onAction = onAction,
            )
        }
        if (index < entries.lastIndex) {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** What the shared home panel is showing. */
private sealed interface HomeSheet {
    data class Folder(val tag: String) : HomeSheet
    data class Notifications(val firstPkg: String?) : HomeSheet
}

/** Holds the last shown sheet so the panel can animate out with content. */
private class Retained<T>(var value: T?)

/**
 * The one place home content opens: a dark card rising from just above the
 * dock, over a dimmed screen — same spot whatever opened it, within thumb
 * reach. Tap outside, press back, or drag the handle away to close.
 * [fromTop] sheets drop from under the status bar instead (the notification
 * list, when the user prefers it there); their handle sits at the bottom.
 */
@Composable
private fun <T : Any> BoxScope.HomePanel(
    sheet: T?,
    onDismiss: () -> Unit,
    fromTop: (T) -> Boolean = { false },
    header: @Composable RowScope.(T) -> Unit,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    val retained = remember { Retained<T>(null) }
    if (sheet != null) retained.value = sheet
    val top = retained.value?.let(fromTop) ?: false
    BackHandler(enabled = sheet != null, onBack = onDismiss)
    val dismiss by rememberUpdatedState(onDismiss)

    AnimatedVisibility(visible = sheet != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                // Swallow everything so home gestures (drawer swipe, side
                // swipes) can't fire through the scrim; a tap closes.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        dismiss()
                    }
                },
        )
    }
    AnimatedVisibility(
        visible = sheet != null,
        enter = slideInVertically(initialOffsetY = { if (top) -it / 3 else it / 3 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { if (top) -it / 3 else it / 3 }) + fadeOut(),
        modifier = if (top) {
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                // Bottom edge sits one row above the dock pill, where the corner
                // shortcut lives — the same line every panel opens from.
                .padding(start = 16.dp, end = 16.dp, bottom = 106.dp)
        },
    ) {
        val shown = retained.value ?: return@AnimatedVisibility
        // Distance dragged toward closing: down for a bottom panel, up for a top one.
        val drag = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
        val closeDirection = if (top) -1f else 1f
        val dragToClose = Modifier.pointerInput(top) {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (drag.value > 96.dp.toPx()) {
                        dismiss()
                        scope.launch { drag.snapTo(0f) }
                    } else {
                        scope.launch { drag.animateTo(0f) }
                    }
                },
                onDragCancel = { scope.launch { drag.animateTo(0f) } },
            ) { change, amount ->
                change.consume()
                scope.launch {
                    drag.snapTo((drag.value + amount * closeDirection).coerceAtLeast(0f))
                }
            }
        }
        val handle = @Composable { modifier: Modifier ->
            Box(
                modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
            )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PanelColor,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .graphicsLayer { translationY = drag.value * closeDirection },
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column {
                    // Header (with the handle on a bottom panel) is a grip too.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(dragToClose),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (!top) handle(Modifier.padding(top = 10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                                .defaultMinSize(minHeight = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            header(shown)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = if (top) 4.dp else 12.dp),
                    ) {
                        content(shown)
                    }
                    if (top) {
                        // A top panel closes upward: grip at its bottom edge.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(dragToClose)
                                .padding(top = 6.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            handle(Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAppRow(
    app: AppEntry?,
    fallbackName: String,
    info: NotifInfo,
    onOpen: (String) -> Unit,
    onDismiss: (List<String>) -> Unit,
    onDismissApp: () -> Unit,
    onAction: (itemKey: String, actionIndex: Int, replyText: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Details show by default — collapsing was constant busywork; the chevron
    // now exists to tuck a noisy app away, not to reveal the content.
    var expanded by remember { mutableStateOf(true) }
    val name = app?.label ?: fallbackName
    val expandable = info.count > 1 ||
        info.items.any { it.actions.isNotEmpty() || it.copies > 1 }
    Column(modifier = modifier.padding(vertical = 6.dp, horizontal = 12.dp)) {
        SwipeToClear(onClear = onDismissApp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val first = info.items.firstOrNull()
                        if (first != null) onOpen(first.key)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (app != null) {
                    AppIcon(app = app, size = 34.dp)
                } else {
                    Spacer(Modifier.size(34.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (info.count > 1) {
                            Text(
                                text = info.count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = NotificationMark,
                            )
                        }
                    }
                    // Collapsed: a one-line summary. Expanded: the detail rows
                    // below carry the text, so don't repeat it here.
                    if (!expanded) {
                        info.preview?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (expandable) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { expanded = !expanded }
                            .padding(4.dp),
                    )
                }
            }
        }
        // Expanded: every notification for this app, with its actions.
        if (expanded) {
            info.items.forEach { notif ->
                key(notif.key) {
                    NotificationItemDetail(
                        notif = notif,
                        onOpen = { onOpen(notif.key) },
                        onDismiss = { onDismiss(notif.keys) },
                        onAction = { index, reply -> onAction(notif.key, index, reply) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemDetail(
    notif: NotifItem,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onAction: (actionIndex: Int, replyText: String?) -> Unit,
) {
    var replyingTo by remember { mutableStateOf<Int?>(null) }
    Column(
        // Flush with the app icon, not the label — the indent looked tidy
        // but cost 34dp: cramped text and action chips wrapping to two lines.
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
    ) {
        SwipeToClear(onClear = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpen)
                    .padding(vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    notif.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (notif.copies > 1) {
                        // The app repeated this identical message N times.
                        Text(
                            text = "×${notif.copies}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ExposedAmber,
                        )
                    }
                }
                notif.text?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (notif.actions.isNotEmpty()) {
            // One line, always: overflow scrolls sideways instead of wrapping.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                notif.actions.forEach { action ->
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = ExposedAmber,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (action.isReply) {
                                    replyingTo = if (replyingTo == action.index) null else action.index
                                } else {
                                    onAction(action.index, null)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            replyingTo?.let { index ->
                var reply by remember(index) { mutableStateOf("") }
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(ExposedAmber),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                if (reply.isEmpty()) {
                                    Text(
                                        text = "Reply…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.4f),
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Send",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (reply.isBlank()) Color.White.copy(alpha = 0.3f) else ExposedAmber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = reply.isNotBlank()) {
                                onAction(index, reply)
                                replyingTo = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private sealed interface HomeItem {
    val token: String
    data class AppItem(val app: AppEntry) : HomeItem {
        override val token: String get() = app.key
    }
    data class FolderItem(val tag: AppTag, val colorIndex: Int) : HomeItem {
        override val token: String get() = "folder:${tag.name}"
    }
}

/** Combined home order: apps and folders interleaved, sorted by saved tokens. */
private fun buildHomeItems(
    homeApps: List<AppEntry>,
    folderTags: List<AppTag>,
    order: List<String>,
): List<HomeItem> {
    val items = homeApps.map { HomeItem.AppItem(it) } +
        folderTags.mapIndexed { i, t -> HomeItem.FolderItem(t, i) }
    val pos = order.withIndex().associate { (i, tok) -> tok to i }
    return items.sortedBy { pos[it.token] ?: Int.MAX_VALUE }
}

/** Long-press menu for a home folder: color, reorder, rename, remove from home. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FolderMenuSheet(
    tag: AppTag,
    colorIndex: Int,
    onSetColor: (Int?) -> Unit,
    onReorderHome: () -> Unit,
    onReorderApps: () -> Unit,
    onRename: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onDismiss: () -> Unit,
    /** Overrides the remove row's text (the Airlock home says so). */
    removeLabel: String? = null,
) {
    var showCustom by remember {
        mutableStateOf(
            tag.folderColor != null && FolderPalette.none { it.toArgb() == tag.folderColor }
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FolderIcon(
                    colorIndex = colorIndex,
                    color = tag.folderColor?.let { Color(it) },
                    size = 40.dp,
                )
                Text(text = tag.name, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            OptionLabel("Color")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Auto (index-based default) + explicit palette choices.
                val autoSelected = tag.folderColor == null
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FolderPalette[colorIndex % FolderPalette.size])
                        .border(
                            width = if (autoSelected) 2.5.dp else 1.dp,
                            color = if (autoSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onSetColor(null) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                FolderPalette.forEach { preset ->
                    val selected = tag.folderColor == preset.toArgb()
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                shape = CircleShape,
                            )
                            .clickable { onSetColor(preset.toArgb()) },
                    )
                }
                // Custom: any color not matching a preset (or the palette Auto).
                val isCustom = tag.folderColor != null &&
                    FolderPalette.none { it.toArgb() == tag.folderColor }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCustom) Color(tag.folderColor!!)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                        )
                        .border(
                            width = if (isCustom || showCustom) 2.5.dp else 1.dp,
                            color = if (isCustom || showCustom) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { showCustom = !showCustom },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isCustom) {
                        Text("+", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    }
                }
            }
            if (showCustom) {
                Spacer(Modifier.height(12.dp))
                ColorPicker(
                    color = tag.folderColor?.let { Color(it) } ?: DefaultAccent,
                    onChange = { onSetColor(it.toArgb()) },
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            MenuRow(
                icon = Icons.Outlined.KeyboardArrowUp,
                text = "Reorder home",
                onClick = onReorderHome,
            )
            MenuRow(
                icon = Icons.Outlined.KeyboardArrowUp,
                text = "Reorder apps in folder",
                onClick = onReorderApps,
            )
            MenuRow(icon = Icons.Outlined.Edit, text = "Rename", onClick = onRename)
            MenuRow(
                icon = Icons.Outlined.Home,
                text = removeLabel ?: "Remove from home",
                onClick = onRemoveFromHome,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Home-screen folder: a tag rendered as one row. Tapping opens a popup of its
 * apps anchored at the row; long-press opens its menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    tag: AppTag,
    colorIndex: Int,
    apps: List<AppEntry>,
    privateSpace: PrivateSpace?,
    notifications: Map<String, NotifInfo>,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Aggregate: total notifications across the apps this folder holds.
    val folderCount = apps.sumOf { notifications[it.notificationKey()]?.count ?: 0 }
    Row(
        // Wrap content, not the row: blank home-screen space next to a
        // folder must not open it (same as AppRow's home behavior).
        modifier = Modifier
            .combinedClickable(onClick = onOpen, onLongClick = onLongClick)
            .padding(
                start = HomeRowTouchExtension,
                end = HomeRowTouchExtension * 2,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Match AppRow's ring footprint so labels stay in column.
        Box(modifier = Modifier.padding(3.dp)) {
            FolderIcon(
                colorIndex = colorIndex,
                color = tag.folderColor?.let { Color(it) },
                size = 46.dp,
            )
            if (folderCount > 0) {
                NotificationDot(Modifier.align(Alignment.TopEnd))
            }
        }
        Text(text = tag.name, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A folder's apps, shown in the shared [HomePanel]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderPanelContent(
    tag: AppTag,
    apps: List<AppEntry>,
    privateSpace: PrivateSpace?,
    notifications: Map<String, NotifInfo>,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: (AppEntry) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        if (apps.isEmpty()) {
            Text(
                text = "Empty — tag apps with \"${tag.name}\"",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(8.dp),
            )
        } else {
            // Vertical list: icon + label per row, with badges.
            apps.forEach { app ->
                val isPrivate = app.user == privateSpace?.profile
                val count = notifications[app.notificationKey()]?.count ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .combinedClickable(
                            onClick = { onLaunch(app) },
                            onLongClick = { onAppMenu(app) },
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PrivateRing(isPrivate = isPrivate) {
                        Box {
                            AppIcon(app = app, size = 38.dp)
                            if (count > 0) {
                                NotificationDot(Modifier.align(Alignment.TopEnd))
                            }
                        }
                    }
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isPrivate) ExposedAmber else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Folder tile: a subtly tinted container (not a saturated app-like squircle),
 * with the stacked-container glyph in the folder's full color doing the read.
 * Keeps per-folder color as a differentiator while staying in the monochrome
 * palette so folders don't outshout real app icons.
 */
@Composable
private fun FolderIcon(colorIndex: Int, color: Color?, size: Dp) {
    val tint = color ?: FolderPalette[
        ((colorIndex % FolderPalette.size) + FolderPalette.size) % FolderPalette.size
    ]
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            .background(tint.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = remember(tint) { containerStackIcon(tint) },
            contentDescription = null,
            modifier = Modifier.size(size * 0.64f),
        )
    }
}

/** Default folder colors, assigned by name so folders differ at a glance. */
private val FolderPalette = listOf(
    Color(0xFFE07A5F), Color(0xFF6C8EBF), Color(0xFF81B29A),
    Color(0xFFE2B85F), Color(0xFF9A7CB8), Color(0xFFC77D9A),
    Color(0xFF5FA8A0), Color(0xFFB08968),
)

/** Amber ring marking a private-space app; replaces the old lock overlay. */
@Composable
private fun PrivateRing(isPrivate: Boolean, content: @Composable () -> Unit) {
    Box(
        // Same footprint with or without the ring so labels align across
        // normal and airlock rows in mixed lists.
        modifier = if (isPrivate) {
            Modifier
                .border(1.5.dp, ExposedAmber.copy(alpha = 0.9f), CircleShape)
                .padding(3.dp)
        } else {
            Modifier.padding(3.dp)
        },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Renders an app icon honoring its user-selected style. */
@Composable
private fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    if (app.shortcutId == null) {
        AppIconImage(app, size, modifier)
        return
    }
    // A pinned shortcut's label and icon are whatever the pinning app chose,
    // so it could pass for a real app; the corner arrow says it isn't one.
    Box(modifier = modifier) {
        AppIconImage(app, size)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.4f)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Shortcut",
                tint = Color(0xFF14181F),
                modifier = Modifier
                    .size(size * 0.28f)
                    .rotate(-45f),
            )
        }
    }
}

@Composable
private fun AppIconImage(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    when {
        app.iconStyle == IconStyle.Monochrome && app.monoIcon != null -> Image(
            bitmap = app.monoIcon,
            contentDescription = app.label,
            colorFilter = ColorFilter.tint(LocalContentColor.current),
            modifier = modifier.size(size),
        )
        app.iconStyle == IconStyle.Flat && app.flatIcon != null -> Image(
            bitmap = app.flatIcon,
            contentDescription = app.label,
            modifier = modifier.size(size),
        )
        app.iconStyle == IconStyle.Letter -> Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        else -> Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = modifier.size(size),
        )
    }
}

/**
 * Full app list shown on swipe up. Tag chips at the top filter the list;
 * long-press a row for the tag/app-info/uninstall menu. The alphabet rail on
 * the right jumps to the first app starting with that letter.
 */
@Composable
private fun AppDrawer(
    apps: List<AppEntry>,
    tags: List<AppTag>,
    notifications: Map<String, NotifInfo>,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    onAddTag: () -> Unit,
    onRenameTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onReorder: (String, List<String>) -> Unit,
    onReorderHome: () -> Unit,
    homeFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    /** Folders on the Airlock home; null when there's no airlock. */
    airlockHomeFolders: Set<String>?,
    onToggleAirlockFolder: (String) -> Unit,
    onSetFolderColor: (String, Int?) -> Unit,
    searchFocus: FocusRequester,
    searchVisible: Boolean,
    leftHanded: Boolean,
    privateSpace: PrivateSpace?,
    onTogglePrivateSpace: () -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Pulling down past the top of the list closes the drawer — not while
    // searching, where a downward drag is about the results.
    val pull = rememberPullToClose(enabled = !searchVisible, onClose = onClose)
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var tagMenuFor by remember { mutableStateOf<String?>(null) }
    var renameTagFor by remember { mutableStateOf<String?>(null) }
    var reorderTagFor by remember { mutableStateOf<String?>(null) }

    var scrubLetter by remember { mutableStateOf<Char?>(null) }

    // The pill enters composition the same frame this flips true; focus once
    // it lands (runCatching: a close racing the toggle must not crash the
    // launcher). Toggling it away clears the query so the list unfilters.
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            runCatching { searchFocus.requestFocus() }
            keyboard?.show()
        } else {
            query = ""
            keyboard?.hide()
        }
    }

    // Private-space apps sit alphabetically alongside everything else; the
    // virtual "Private" chip filters down to just them.
    val visibleApps = remember(apps, tags, selectedTag, query, privateSpace) {
        val base = if (selectedTag == PRIVATE_FILTER) {
            if (privateSpace == null) emptyList()
            else apps.filter { it.user == privateSpace.profile }
        } else {
            val tag = tags.firstOrNull { it.name == selectedTag }
            if (tag == null) apps else orderedApps(apps, tag)
        }
        if (query.isBlank()) base
        else base.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val letters = remember(visibleApps) { visibleApps.map { it.indexLetter }.distinct() }
    // The A-Z rail only makes sense over an alphabetical list. A tag with an
    // explicit drag order isn't alphabetical (the rail would read "P G C S"),
    // and a short list doesn't warrant a stub rail.
    val showRail = remember(selectedTag, tags, letters) {
        val customOrdered = selectedTag != null && selectedTag != PRIVATE_FILTER &&
            tags.firstOrNull { it.name == selectedTag }?.order?.isNotEmpty() == true
        !customOrdered && letters.size >= 8
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = pull.offset }
            .nestedScroll(pull.connection)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 32.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Search is on demand: hidden until the dock's magnifier summons
            // it, so the list gets the vertical space back the rest of the time.
            AnimatedVisibility(visible = searchVisible) {
                SearchPill(
                    query = query,
                    onQueryChange = { query = it },
                    // Enter launches the first (best) match.
                    onGo = { visibleApps.firstOrNull()?.let(onLaunch) },
                    focusRequester = searchFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 12.dp),
                )
            }

            // Tag chips: All, each tag, and "+" to create a new one.
            // Long-press a tag chip to rename or delete it.
            val chipFade = MaterialTheme.colorScheme.surface
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Trailing fade hints the row scrolls past the right edge.
                    .drawWithContent {
                        drawContent()
                        val fade = 24.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, chipFade),
                                startX = size.width - fade,
                                endX = size.width,
                            ),
                        )
                    }
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(
                    text = "All",
                    selected = selectedTag == null,
                    onClick = { selectedTag = null },
                )
                // A concealed (system-hidden, sealed) space gets no chip; the
                // rocket long-press still opens it for whoever knows.
                privateSpace?.takeUnless { it.concealed }?.let { ps ->
                    PrivateTagChip(
                        unlocked = ps.unlocked,
                        selected = selectedTag == PRIVATE_FILTER,
                        onClick = {
                            if (!ps.unlocked) {
                                // Kick off the system unlock; select the filter so
                                // the apps appear as soon as the profile starts.
                                onTogglePrivateSpace()
                                selectedTag = PRIVATE_FILTER
                            } else {
                                selectedTag =
                                    if (selectedTag == PRIVATE_FILTER) null else PRIVATE_FILTER
                            }
                        },
                        onLongClick = { if (ps.unlocked) onTogglePrivateSpace() },
                    )
                }
                tags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        selected = selectedTag == tag.name,
                        onClick = {
                            selectedTag = if (selectedTag == tag.name) null else tag.name
                        },
                        // Reserved tags can be reordered, just not renamed/deleted.
                        onLongClick = {
                            // Reserved tags only offer reorder; skip the
                            // one-item menu. Home reorders apps+folders
                            // together; Dock is apps-only.
                            when (tag.name) {
                                TagsRepository.HOME -> onReorderHome()
                                // Corner holds one app, but reorder is still
                                // the least-wrong long-press for a reserved
                                // tag (no rename/delete/folder).
                                TagsRepository.DOCK,
                                TagsRepository.CORNER,
                                -> reorderTagFor = tag.name
                                else -> tagMenuFor = tag.name
                            }
                        },
                    )
                }
                TagChip(text = "+", selected = false, onClick = onAddTag)
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                // The whole edge strip is the touch surface so a near-miss
                // scrubs instead of launching the app row next to it; letters
                // mirror sides in left-handed mode.
                val rail: @Composable () -> Unit = {
                    AlphabetRail(
                        letters = letters,
                        activeLetter = scrubLetter,
                        alignEnd = !leftHanded,
                        modifier = Modifier
                            .width(64.dp)
                            .fillMaxHeight(),
                        onScrub = { letter ->
                            scrubLetter = letter
                            if (letter != null) {
                                val target =
                                    visibleApps.indexOfFirst { it.indexLetter == letter }
                                if (target >= 0) {
                                    scope.launch {
                                        // Land the section upper-middle, near the
                                        // thumb and magnifier, not at the top edge.
                                        val viewport =
                                            listState.layoutInfo.viewportSize.height
                                        listState.scrollToItem(
                                            index = target,
                                            scrollOffset = -(viewport * 0.35f).toInt(),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
                if (leftHanded && showRail) rail()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    // Clear the floating dock at the bottom.
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(visibleApps, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            onClick = { onLaunch(app) },
                            onLongClick = { onAppMenu(app) },
                            isPrivate = app.user == privateSpace?.profile,
                            hasNotification = app.notificationKey() in notifications,
                        )
                    }
                    if (visibleApps.isEmpty() && query.isNotBlank()) {
                        item(key = "no-matches") {
                            SearchStatus("No apps match “${query.trim()}”")
                        }
                    }
                    if (selectedTag == PRIVATE_FILTER && privateSpace?.unlocked == false) {
                        item(key = "private-space-locked-note") {
                            Text(
                                text = "The airlock is sealed — contained apps are stopped " +
                                    "and hidden. Tap the Airlock chip to open it.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, end = 16.dp),
                            )
                        }
                    }
                }
                if (!leftHanded && showRail) rail()
            }
        }

        // Magnifier bubble while scrubbing the alphabet rail.
        scrubLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    tagMenuFor?.let { tag ->
        TagMenuSheet(
            tag = tag,
            isReserved = tag in RESERVED_TAGS,
            isFolder = tag in homeFolders,
            isAirlockFolder = airlockHomeFolders?.let { tag in it },
            folderColor = tags.firstOrNull { it.name == tag }?.folderColor,
            onSetFolderColor = { onSetFolderColor(tag, it) },
            onToggleFolder = {
                onToggleFolder(tag)
                tagMenuFor = null
            },
            onToggleAirlockFolder = {
                onToggleAirlockFolder(tag)
                tagMenuFor = null
            },
            onReorder = {
                reorderTagFor = tag
                tagMenuFor = null
            },
            onRename = {
                renameTagFor = tag
                tagMenuFor = null
            },
            onDelete = {
                onDeleteTag(tag)
                if (selectedTag == tag) selectedTag = null
                tagMenuFor = null
            },
            onDismiss = { tagMenuFor = null },
        )
    }

    reorderTagFor?.let { tagName ->
        val tag = tags.firstOrNull { it.name == tagName }
        if (tag == null) {
            reorderTagFor = null
        } else {
            ReorderScreen(
                title = "Reorder · $tagName",
                items = orderedApps(apps, tag).map { app ->
                    ReorderItem(app.key, app.label) { AppIcon(app = app, size = 38.dp) }
                },
                onCommit = { keys -> onReorder(tagName, keys) },
                onDismiss = { reorderTagFor = null },
            )
        }
    }

    renameTagFor?.let { oldName ->
        TagNameDialog(
            title = "Rename tag",
            initialName = oldName,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                onRenameTag(oldName, newName)
                if (selectedTag == oldName) selectedTag = newName.trim()
                renameTagFor = null
            },
            onDismiss = { renameTagFor = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagMenuSheet(
    tag: String,
    isReserved: Boolean,
    isFolder: Boolean,
    /** On the Airlock home; null when there's no airlock (row hidden). */
    isAirlockFolder: Boolean?,
    folderColor: Int?,
    onReorder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFolder: () -> Unit,
    onToggleAirlockFolder: () -> Unit,
    onSetFolderColor: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = tag,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            MenuRow(icon = Icons.Outlined.KeyboardArrowUp, text = "Reorder apps", onClick = onReorder)
            if (!isReserved) {
                MenuRow(
                    icon = Icons.Outlined.Home,
                    text = if (isFolder) "Remove folder from home"
                    else "Show as folder on home",
                    onClick = onToggleFolder,
                )
                isAirlockFolder?.let { onAirlock ->
                    MenuRow(
                        icon = Icons.Outlined.Lock,
                        text = if (onAirlock) "Remove folder from Airlock home"
                        else "Show as folder on Airlock home",
                        onClick = onToggleAirlockFolder,
                    )
                }
                if (isFolder || isAirlockFolder == true) {
                    Spacer(Modifier.height(4.dp))
                    OptionLabel("Folder color")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HighlightPresets.forEachIndexed { index, preset ->
                            val selected = if (index == 0) folderColor == null
                            else folderColor == preset.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(preset)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        onSetFolderColor(if (index == 0) null else preset.toArgb())
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                MenuRow(icon = Icons.Outlined.Edit, text = "Rename", onClick = onRename)
                MenuRow(
                    icon = Icons.Outlined.Delete,
                    text = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class ReorderItem(
    val key: String,
    val label: String,
    val leading: @Composable () -> Unit,
)

/** Full-screen drag-to-reorder over a mixed list of apps and folders. */
@Composable
private fun ReorderScreen(
    title: String,
    items: List<ReorderItem>,
    onCommit: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var order by remember(items) { mutableStateOf(items) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val pitchPx = with(LocalDensity.current) { 60.dp.toPx() }

    fun commit() = onCommit(order.map { it.key })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { commit(); onDismiss() }) {
                    Icon(Icons.Filled.Check, contentDescription = "Done", tint = ProtectedGreen)
                }
            }
            Text(
                text = "Drag the handle to rearrange",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                order.forEach { item ->
                    key(item.key) {
                        val dragging = item.key == draggingKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                                .then(
                                    if (dragging) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                            RoundedCornerShape(12.dp),
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item.leading()
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .pointerInput(item.key) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggingKey = item.key
                                                dragOffset = 0f
                                            },
                                            onDragEnd = {
                                                draggingKey = null
                                                dragOffset = 0f
                                                commit()
                                            },
                                            onDragCancel = {
                                                draggingKey = null
                                                dragOffset = 0f
                                            },
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val current = order.indexOfFirst { it.key == item.key }
                                            val shift = (dragOffset / pitchPx).roundToInt()
                                            val target =
                                                (current + shift).coerceIn(0, order.lastIndex)
                                            if (target != current) {
                                                order = order.toMutableList()
                                                    .apply { add(target, removeAt(current)) }
                                                dragOffset -= (target - current) * pitchPx
                                            }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Rounded search field styled to match the floating dock. */
@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { onQueryChange("") },
            )
        }
    }
}

/**
 * Virtual tag for airlock (private space) apps. Sealed: tap starts the
 * system unlock. Open: tap filters, long-press seals. The hatch glyph goes
 * amber while open.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivateTagChip(
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val textColor =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = AirlockIcon,
            contentDescription = null,
            tint = if (unlocked) ExposedAmber else textColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Airlock",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val textColor =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = textColor,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(PillShape)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** Long-press menu: identity header, tag membership, icon style, actions. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppMenuSheet(
    app: AppEntry,
    tags: List<AppTag>,
    globalPack: String?,
    hasIconPacks: Boolean,
    onToggleTag: (String) -> Unit,
    onNewTag: () -> Unit,
    dockSideRight: Boolean?,
    onSetDockSide: (Boolean) -> Unit,
    onSetIcon: (IconStyle) -> Unit,
    onSetIconSource: (String?) -> Unit,
    onChoosePackIcon: () -> Unit,
    onRename: () -> Unit,
    onReorderHome: (() -> Unit)?,
    onAppInfo: () -> Unit,
    onRemoveShortcut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon(app = app, size = 48.dp)
                Column {
                    Text(text = app.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = app.componentName.packageName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Tags")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        selected = app.key in tag.appKeys,
                        onClick = { onToggleTag(tag.name) },
                    )
                }
                TagChip(text = "+", selected = false, onClick = onNewTag)
            }

            // Only for apps currently occupying a dock slot.
            dockSideRight?.let { right ->
                Spacer(Modifier.height(16.dp))
                SectionLabel("Dock side")
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip(
                        text = "Left of rocket",
                        selected = !right,
                        onClick = { onSetDockSide(false) },
                    )
                    TagChip(
                        text = "Right of rocket",
                        selected = right,
                        onClick = { onSetDockSide(true) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Icon")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(
                    text = "App icon",
                    selected = app.iconStyle == IconStyle.Default && app.iconSource == null,
                    onClick = {
                        onSetIcon(IconStyle.Default)
                        onSetIconSource(null)
                    },
                )
                if (globalPack != null) {
                    // With a global pack active, "App icon" means the themed
                    // one; this forces the untouched system icon instead.
                    TagChip(
                        text = "Original",
                        selected = app.iconStyle == IconStyle.Default &&
                            app.iconSource == IconPackRepository.SOURCE_SYSTEM,
                        onClick = {
                            onSetIcon(IconStyle.Default)
                            onSetIconSource(IconPackRepository.SOURCE_SYSTEM)
                        },
                    )
                }
                if (app.monoIcon != null) {
                    TagChip(
                        text = "Mono",
                        selected = app.iconStyle == IconStyle.Monochrome,
                        onClick = { onSetIcon(IconStyle.Monochrome) },
                    )
                }
                if (app.flatIcon != null) {
                    TagChip(
                        text = "Flat",
                        selected = app.iconStyle == IconStyle.Flat,
                        onClick = { onSetIcon(IconStyle.Flat) },
                    )
                }
                TagChip(
                    text = "Letter",
                    selected = app.iconStyle == IconStyle.Letter,
                    onClick = { onSetIcon(IconStyle.Letter) },
                )
                if (hasIconPacks) {
                    TagChip(
                        text = "From pack…",
                        selected = app.iconStyle == IconStyle.Default &&
                            app.iconSource
                                ?.startsWith(IconPackRepository.SOURCE_PACK_PREFIX) == true,
                        onClick = onChoosePackIcon,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            // Only offered when the menu was opened from a home row.
            onReorderHome?.let {
                MenuRow(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    text = "Reorder home",
                    onClick = it,
                )
            }
            MenuRow(icon = Icons.Outlined.Edit, text = "Rename", onClick = onRename)
            if (app.shortcutId != null) {
                // Pinned shortcuts have no component to inspect or uninstall.
                MenuRow(
                    icon = Icons.Outlined.Delete,
                    text = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onRemoveShortcut,
                )
            } else {
                MenuRow(icon = Icons.Outlined.Info, text = "App info", onClick = onAppInfo)
                MenuRow(
                    icon = Icons.Outlined.Delete,
                    text = "Uninstall",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DELETE,
                                    Uri.fromParts("package", app.componentName.packageName, null),
                                ).putExtra("android.intent.extra.USER", app.user)
                            )
                        }
                        onDismiss()
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Grid of every icon a pack ships, for a per-app manual pick. Bitmaps load
 * lazily per visible cell; packs can carry thousands of drawables.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PackIconPickerSheet(
    app: AppEntry,
    packs: List<IconPackInfo>,
    initialPack: String?,
    loadDrawables: suspend (String) -> List<String>,
    loadIcon: suspend (String, String) -> ImageBitmap?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var packPackage by remember {
        mutableStateOf(initialPack ?: packs.firstOrNull()?.packageName)
    }
    var query by remember { mutableStateOf("") }
    val names by produceState(initialValue = emptyList<String>(), packPackage) {
        value = packPackage?.let { loadDrawables(it) } ?: emptyList()
    }
    val filtered = remember(names, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) names else names.filter { q in it }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxHeight(0.85f),
        ) {
            Text(
                text = "Icon for ${app.label}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (packs.size > 1) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    packs.forEach { pack ->
                        TagChip(
                            text = pack.label,
                            selected = pack.packageName == packPackage,
                            onClick = { packPackage = pack.packageName },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search icons") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (names.isEmpty()) {
                Text(
                    text = "This pack lists no icons.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(filtered, key = { it }) { name ->
                    val pack = packPackage
                    val bitmap by produceState<ImageBitmap?>(null, pack, name) {
                        value = pack?.let { loadIcon(it, name) }
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                pack?.let {
                                    onSelect(IconPackRepository.packSource(it, name))
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = name,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun RenameAppDialog(
    app: AppEntry,
    onConfirm: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(app.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename app") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
                if (app.label != app.baseLabel) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Original: ${app.baseLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun SearchStatus(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun TagNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Vertical A–Z index; tap or drag to jump. While scrubbing, [onScrub] gets
 * the letter under the finger (null when released) so the drawer can show
 * the magnifier bubble; the active letter also highlights in the rail.
 */
@Composable
private fun AlphabetRail(
    letters: List<Char>,
    activeLetter: Char?,
    alignEnd: Boolean,
    onScrub: (Char?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Letter-column bounds within the strip; touches anywhere in the strip
    // map onto it (clamped past the ends).
    var lettersTop by remember { mutableFloatStateOf(0f) }
    var lettersHeight by remember { mutableIntStateOf(0) }

    fun letterAt(y: Float): Char? {
        if (lettersHeight == 0 || letters.isEmpty()) return null
        val fraction = (y - lettersTop) / lettersHeight
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
        return letters[index]
    }

    Box(
        modifier = modifier
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    letterAt(offset.y)?.let(onScrub)
                    onScrub(null)
                }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> letterAt(offset.y)?.let(onScrub) },
                    onDragEnd = { onScrub(null) },
                    onDragCancel = { onScrub(null) },
                ) { change, _ -> letterAt(change.position.y)?.let(onScrub) }
            },
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(
                    start = if (alignEnd) 0.dp else 28.dp,
                    end = if (alignEnd) 28.dp else 0.dp,
                )
                .onGloballyPositioned {
                    lettersTop = it.positionInParent().y
                    lettersHeight = it.size.height
                },
        ) {
            letters.forEach { letter ->
                val active = letter == activeLetter
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else null,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isPrivate: Boolean = false,
    hasNotification: Boolean = false,
    iconSize: Dp = 40.dp,
    // Home rows sit over open wallpaper, so their hit area hugs the
    // icon+label; drawer rows are a plain list and stay full width.
    fullWidthTouch: Boolean = true,
    // Extra horizontal slack inside the tap target (home rows only); the
    // trailing side gets double, matching HomeRowTouchExtension's policy.
    touchPadding: Dp = 0.dp,
) {
    Row(
        modifier = Modifier
            .then(if (fullWidthTouch) Modifier.fillMaxWidth() else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = touchPadding, end = touchPadding * 2, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrivateRing(isPrivate = isPrivate) {
            Box {
                AppIcon(app = app, size = iconSize)
                if (hasNotification) {
                    NotificationDot(Modifier.align(Alignment.TopEnd))
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPrivate) ExposedAmber else Color.Unspecified,
            )
            if (isPrivate) {
                // Mirrors the Airlock chip's hatch glyph.
                Icon(
                    imageVector = AirlockIcon,
                    contentDescription = "Airlock app",
                    tint = ExposedAmber,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** A small amber dot marking an app with a pending notification. */
@Composable
private fun NotificationDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 2.dp, y = (-2).dp)
            .size(11.dp)
            .clip(CircleShape)
            .background(NotificationMark)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
    )
}

/** Per-profile notification key matching MediaNotificationListener's map. */
private fun AppEntry.notificationKey(): String =
    "${user.hashCode()}/${componentName.packageName}"

/** First letter used for the drawer's alphabet index ('#' for non-letters). */
private val AppEntry.indexLetter: Char
    get() {
        val first = label.firstOrNull()?.uppercaseChar() ?: '#'
        return if (first.isLetter()) first else '#'
    }

private val RESERVED_TAGS =
    setOf(TagsRepository.HOME, TagsRepository.DOCK, TagsRepository.CORNER)

/** Sentinel for the virtual private-space tag ("::" is illegal in tag UI). */
private const val PRIVATE_FILTER = "::private::"


// Light enough that a short flick right above the dock registers, but still
// clear of vertical wander during the (90dp) side-swipe gestures. Note the
// ~12dp of touch slop spent before travel starts counting.
private const val SWIPE_UP_THRESHOLD_DP = 48
private const val SWIPE_SIDE_THRESHOLD_DP = 90

// A quick flick reads as intent regardless of how far it traveled; a slow
// drag needs the distance. Comfortably above scroll-settle speeds.
private const val FLICK_VELOCITY_DP_PER_S = 500

/**
 * Swipe-up-to-open: fires the moment [thresholdDp] of upward travel is
 * crossed (mid-drag, not waiting for finger-up), or on release after an
 * upward flick past [FLICK_VELOCITY_DP_PER_S] — natural flicks are fast and
 * short, and shouldn't have to cover a drag's distance. [consume] eats the
 * moves so clickables underneath cancel instead of firing.
 */
/** Mirror of [detectSwipeUp]: fires once past [thresholdDp] down, or on a downward flick. */
private suspend fun PointerInputScope.detectSwipeDown(
    thresholdDp: Int,
    onSwipeDown: () -> Unit,
) {
    var total = 0f
    var fired = false
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = {
            total = 0f
            fired = false
            tracker.resetTracking()
        },
        onDragEnd = {
            val flung = tracker.calculateVelocity().y > FLICK_VELOCITY_DP_PER_S.dp.toPx()
            if (!fired && flung && total > 0f) onSwipeDown()
        },
    ) { change, dragAmount ->
        tracker.addPosition(change.uptimeMillis, change.position)
        // Consumed so a swipe starting on search/settings never taps them.
        change.consume()
        total += dragAmount
        if (!fired && total > thresholdDp.dp.toPx()) {
            fired = true
            onSwipeDown()
        }
    }
}

private suspend fun PointerInputScope.detectSwipeUp(
    thresholdDp: Int,
    consume: Boolean = false,
    onSwipeUp: () -> Unit,
) {
    var total = 0f
    var fired = false
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = {
            total = 0f
            fired = false
            tracker.resetTracking()
        },
        onDragEnd = {
            val flung = tracker.calculateVelocity().y < -FLICK_VELOCITY_DP_PER_S.dp.toPx()
            if (!fired && flung && total < 0f) onSwipeUp()
        },
    ) { change, dragAmount ->
        tracker.addPosition(change.uptimeMillis, change.position)
        if (consume) change.consume()
        total += dragAmount
        if (!fired && total < -thresholdDp.dp.toPx()) {
            fired = true
            onSwipeUp()
        }
    }
}
