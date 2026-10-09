package com.mesmerverse.gantry.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Xml
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/** An installed third-party icon pack (a resources-only APK). */
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/**
 * Reads standard icon packs: APKs that answer the launcher-theme intents and
 * ship an appfilter.xml mapping components to drawable names. Gantry only
 * reads their resources — pack code never runs. Discovery works without
 * QUERY_ALL_PACKAGES because the theme actions are declared in <queries>.
 */
class IconPackRepository(private val context: Context) {

    private class LoadedPack(
        val resources: Resources,
        /** "package/fully.qualified.Activity" → drawable resource name. */
        val components: Map<String, String>,
        /** Every drawable name the pack lists, for the manual picker. */
        val drawableNames: List<String>,
    )

    private val mutex = Mutex()
    private val packCache = mutableMapOf<String, LoadedPack?>()
    private val bitmapCache = mutableMapOf<String, ImageBitmap?>()

    private val iconSizePx: Int
        get() = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()

    /** Icon packs currently installed, deduplicated across theme actions. */
    suspend fun installedPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        THEME_ACTIONS
            .flatMap { action ->
                pm.queryIntentActivities(Intent(action), PackageManager.ResolveInfoFlags.of(0))
            }
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { appInfo ->
                IconPackInfo(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    icon = runCatching {
                        appInfo.loadIcon(pm).toBitmap(iconSizePx, iconSizePx).asImageBitmap()
                    }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** The pack's themed icon for a component, or null when the pack doesn't map it. */
    suspend fun iconFor(packPackage: String, componentName: ComponentName): ImageBitmap? {
        val pack = loadPack(packPackage) ?: return null
        val flat = componentName.flattenToString()
        val drawableName = pack.components[flat]
            // Fall back to any entry for the same package: packs often map only
            // the main activity while apps expose several launcher aliases.
            ?: pack.components.entries
                .firstOrNull { it.key.startsWith("${componentName.packageName}/") }
                ?.value
            ?: return null
        return iconByName(packPackage, drawableName)
    }

    /** A specific drawable from a pack, for per-app manual picks. */
    suspend fun iconByName(packPackage: String, drawableName: String): ImageBitmap? {
        val cacheKey = "$packPackage:$drawableName"
        mutex.withLock { if (bitmapCache.containsKey(cacheKey)) return bitmapCache[cacheKey] }
        val bitmap = withContext(Dispatchers.IO) {
            val pack = loadPack(packPackage) ?: return@withContext null
            runCatching {
                val id = pack.resources.getIdentifier(drawableName, "drawable", packPackage)
                if (id == 0) null
                else pack.resources.getDrawable(id, null)
                    .toBitmap(iconSizePx, iconSizePx).asImageBitmap()
            }.getOrNull()
        }
        mutex.withLock { bitmapCache[cacheKey] = bitmap }
        return bitmap
    }

    /**
     * Re-bases each app's icon bitmap by resolution order: per-app manual
     * pick, then the global pack's component mapping, then the untouched
     * system icon. Unresolvable sources (pack uninstalled, drawable gone)
     * silently fall back to the system icon; the stored preference survives,
     * so reinstalling the pack restores the look.
     */
    suspend fun applyPacks(
        apps: List<AppEntry>,
        globalPack: String?,
        styles: Map<String, AppStyle>,
    ): List<AppEntry> {
        if (globalPack == null && styles.values.none { it.iconSource != null }) return apps
        return withContext(Dispatchers.Default) {
            apps.map { app ->
                val source = styles[app.key]?.iconSource
                when {
                    source == SOURCE_SYSTEM -> app.copy(iconSource = source)
                    source != null && source.startsWith(SOURCE_PACK_PREFIX) -> {
                        val (pack, drawable) = parsePackSource(source)
                            ?: return@map app.copy(iconSource = source)
                        iconByName(pack, drawable)
                            ?.let { app.copy(icon = it, iconSource = source) }
                            ?: app.copy(iconSource = source)
                    }
                    globalPack != null ->
                        iconFor(globalPack, app.componentName)
                            ?.let { app.copy(icon = it) }
                            ?: app
                    else -> app
                }
            }
        }
    }

    /** All drawable names a pack offers, for the picker grid. */
    suspend fun drawableNames(packPackage: String): List<String> =
        loadPack(packPackage)?.drawableNames ?: emptyList()

    /** Drop parsed packs and rendered bitmaps (call when packages change). */
    suspend fun clearCache() = mutex.withLock {
        packCache.clear()
        bitmapCache.clear()
    }

    private suspend fun loadPack(packPackage: String): LoadedPack? {
        mutex.withLock { if (packCache.containsKey(packPackage)) return packCache[packPackage] }
        val loaded = withContext(Dispatchers.IO) { parsePack(packPackage) }
        mutex.withLock { packCache[packPackage] = loaded }
        return loaded
    }

    /** Null when the pack is uninstalled or unreadable — callers fall back to system icons. */
    private fun parsePack(packPackage: String): LoadedPack? = runCatching {
        val resources = context.packageManager.getResourcesForApplication(packPackage)
        val components = mutableMapOf<String, String>()
        val fromAppfilter = linkedSetOf<String>()

        parseXml(resources, packPackage, "appfilter") { parser ->
            if (parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                    ?.takeIf { it.length <= MAX_COMPONENT_LENGTH } ?: return@parseXml
                val drawable = parser.getAttributeValue(null, "drawable")
                    ?.takeIf { it.isResourceName() } ?: return@parseXml
                // Component attrs look like "ComponentInfo{pkg/cls}".
                val flat = component.substringAfter('{', "").substringBefore('}')
                if (flat.contains('/')) components.putIfAbsent(flat, drawable)
                fromAppfilter.add(drawable)
            }
        }

        // drawable.xml is the pack's curated icon catalog; fall back to the
        // appfilter's drawable set when a pack doesn't ship one.
        val fromCatalog = linkedSetOf<String>()
        parseXml(resources, packPackage, "drawable") { parser ->
            if (parser.name == "item") {
                parser.getAttributeValue(null, "drawable")
                    ?.takeIf { it.isResourceName() }
                    ?.let { fromCatalog.add(it) }
            }
        }

        LoadedPack(
            resources = resources,
            components = components,
            drawableNames = (fromCatalog.ifEmpty { fromAppfilter }).sorted(),
        )
    }.getOrNull()

    // A pack is another app's data: bound what one file can make us do.
    // Real packs top out around 20k entries; past the cap, keep what we have.
    private fun forEachStartTag(
        parser: XmlPullParser,
        onStartTag: (XmlPullParser) -> Unit,
    ) {
        var event = parser.eventType
        var tags = 0
        while (event != XmlPullParser.END_DOCUMENT && tags < MAX_XML_TAGS) {
            if (event == XmlPullParser.START_TAG) {
                tags++
                onStartTag(parser)
            }
            event = parser.next()
        }
    }

    /** Plausible Android resource name — anything else can't resolve anyway. */
    private fun String.isResourceName(): Boolean =
        length in 1..MAX_DRAWABLE_NAME_LENGTH && all { it.isLetterOrDigit() || it == '_' }

    private fun parseXml(
        resources: Resources,
        packPackage: String,
        name: String,
        onStartTag: (XmlPullParser) -> Unit,
    ) {
        runCatching {
            val id = resources.getIdentifier(name, "xml", packPackage)
            if (id != 0) {
                resources.getXml(id).use { forEachStartTag(it, onStartTag) }
            } else {
                val bytes = resources.assets.open("$name.xml").use { stream ->
                    stream.readNBytes(MAX_ASSET_XML_BYTES + 1)
                }
                if (bytes.size > MAX_ASSET_XML_BYTES) return@runCatching
                val parser = Xml.newPullParser()
                // No DTD processing: no entity declarations, no expansion.
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                parser.setInput(bytes.inputStream(), null)
                forEachStartTag(parser, onStartTag)
            }
        }
    }

    companion object {
        private const val MAX_XML_TAGS = 100_000
        private const val MAX_ASSET_XML_BYTES = 8 * 1024 * 1024
        private const val MAX_COMPONENT_LENGTH = 512
        private const val MAX_DRAWABLE_NAME_LENGTH = 128

        /** Prefix for a per-app manual pick: "pack:<package>:<drawable>". */
        const val SOURCE_PACK_PREFIX = "pack:"

        /** Per-app override forcing the untouched system icon. */
        const val SOURCE_SYSTEM = "system"

        fun packSource(packPackage: String, drawableName: String) =
            "$SOURCE_PACK_PREFIX$packPackage:$drawableName"

        /** Parses "pack:<package>:<drawable>" into (package, drawable). */
        fun parsePackSource(source: String): Pair<String, String>? {
            val parts = source.removePrefix(SOURCE_PACK_PREFIX).split(':', limit = 2)
            return if (parts.size == 2) parts[0] to parts[1] else null
        }

        private const val ICON_SIZE_DP = 48

        private val THEME_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.anddoes.launcher.THEME",
        )
    }
}
