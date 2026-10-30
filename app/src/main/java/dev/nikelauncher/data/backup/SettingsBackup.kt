// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Base64
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.ExportResult
import dev.nikelauncher.data.Prefs
import dev.nikelauncher.data.stripPrivateSpaceFromExport
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetProvider
import dev.nikelauncher.widget.backupValues
import dev.nikelauncher.widget.restoreBackup
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** What a restore leaves for the user: how many widgets still need setting up. */
data class RestoreReport(val widgetsToConfirm: Int)

/**
 * Portable launcher state: every setting, the widgets with their clock options, and the files the
 * settings point at (a font, icons). Android's own grants and other apps' data stay with Android.
 */
class SettingsBackup(private val context: Context, private val prefs: Prefs) {
    private val manager get() = AppWidgetManager.getInstance(context)
    private val host get() = (context.applicationContext as NikeApp).widgetHost
    private val clock get() = ClockWidgetProvider.componentName(context)

    suspend fun export(): ExportResult = withContext(Dispatchers.IO) {
        operations.withLock {
            val stored = prefs.storedValues()
            val values = linkedMapOf<String, Any>()
            var omitted = false
            stored.forEach { (name, raw) ->
                if (name == BackupValues.PENDING_WIDGETS) return@forEach
                val value = stripPrivateSpaceFromExport(name, raw, true)
                if (value != raw) omitted = true
                if (value != null) values[name] = value
            }
            val pending = pendingWidgetsFromJson(stored[BackupValues.PENDING_WIDGETS] as? String).associateBy { it.id }
            // A widget from another profile is left out like that profile's apps are.
            val foreign = BackupValues.widgetIds(values).filter { id ->
                manager.getAppWidgetInfo(id)?.profile?.let { it != Process.myUserHandle() } == true
            }.toSet()
            if (foreign.isNotEmpty()) {
                omitted = true
                BackupValues.removeWidgets(values, foreign)
            }
            val widgets = JSONArray()
            BackupValues.widgetIds(values).forEach { id ->
                val info = manager.getAppWidgetInfo(id)
                val saved = pending[id]
                val provider = info?.provider ?: saved?.provider?.let(ComponentName::unflattenFromString)
                val options = if (info != null) readOptions(manager.getAppWidgetOptions(id)) else saved?.options.orEmpty()
                widgets.put(
                    JSONObject().put("id", id)
                        .put("provider", provider?.flattenToString() ?: JSONObject.NULL)
                        .put("label", info?.loadLabel(context.packageManager)?.toString() ?: saved?.label ?: "Widget")
                        .put("options", JSONObject(options)).apply {
                            if (provider == clock) put("clock", BackupValues.encode(ClockWidgetConfig.backupValues(context, id)))
                        },
                )
            }
            val assets = collectAssets(values)
            val json = JSONObject().put("format", FORMAT).put("app", APP_NAME)
                .put("values", BackupValues.encode(values)).put("widgets", widgets)
                .put("assets", JSONArray(assets.values.toList())).toString(2)
            require(json.toByteArray(Charsets.UTF_8).size <= BackupValues.MAX_FILE_BYTES)
            ExportResult(json, omitted)
        }
    }

    /** Reads every file the values point at into [JSONObject]s keyed by hash, and points the values at those. */
    private fun collectAssets(values: MutableMap<String, Any>): Map<String, JSONObject> {
        val assets = linkedMapOf<String, JSONObject>()
        var assetBytes = 0
        rewriteAssets(values) { original, kind ->
            val uri = if (kind == ASSET_FONT) Uri.fromFile(File(original)) else Uri.parse(original)
            require(uri.scheme == "content" || uri.scheme == "file")
            if (uri.scheme == "file") {
                require(File(requireNotNull(uri.path)).canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator))
            }
            val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use {
                readBounded(it, BackupValues.MAX_ASSET_BYTES)
            }
            validateAsset(context, bytes, kind)
            val id = digest(bytes)
            require(assets.size < BackupValues.MAX_ASSETS || id in assets)
            if (id !in assets) assetBytes += bytes.size
            require(assetBytes <= MAX_ASSET_TOTAL_BYTES)
            assets[id] = JSONObject().put("id", id).put("kind", kind)
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
            "backup-asset:$id"
        }
        return assets
    }

    /**
     * Replaces everything with what [text] holds, or changes nothing and answers null.
     *
     * Everything is read and checked first. Widgets get new ids on this phone, so each is bound
     * again; one the system will not bind by itself is left for the user to confirm.
     */
    suspend fun restore(text: String): RestoreReport? = withContext(Dispatchers.IO) {
        operations.withLock {
            var directory: File? = null
            val allocated = mutableListOf<Int>()
            var committed = false
            try {
                require(text.toByteArray(Charsets.UTF_8).size <= BackupValues.MAX_FILE_BYTES)
                val root = JSONObject(text)
                require(root.getInt("format") == FORMAT && root.optString("app") == APP_NAME)
                val values = BackupValues.decode(root.getJSONObject("values")).toMutableMap()
                values.remove(BackupValues.PENDING_WIDGETS)
                values.keys.toList().forEach { name ->
                    val value = stripPrivateSpaceFromExport(name, values.getValue(name), true)
                    if (value == null) values.remove(name) else values[name] = value
                }
                val widgets = readWidgets(root.getJSONArray("widgets"), BackupValues.widgetIds(values), clock)
                val assets = readAssets(root.getJSONArray("assets"), context)
                if (assets.isNotEmpty()) {
                    directory = File(context.filesDir, "restored_assets/" + UUID.randomUUID())
                    check(directory.mkdirs())
                }
                val paths = assets.mapValues { (id, asset) ->
                    File(directory, id + if (asset.kind == ASSET_FONT) ".font" else ".image").apply { writeBytes(asset.bytes) }
                }
                rewriteAssets(values) { reference, kind ->
                    require(reference.startsWith("backup-asset:"))
                    val id = reference.removePrefix("backup-asset:")
                    require(requireNotNull(assets[id]).kind == kind)
                    val file = paths.getValue(id)
                    if (kind == ASSET_FONT) file.absolutePath else Uri.fromFile(file).toString()
                }
                withContext(NonCancellable) {
                    val previousIds = runCatching { BackupValues.widgetIds(prefs.storedValues()) }.getOrDefault(emptyList())
                    val ownedBefore = host.appWidgetIds.toSet()
                    val mapping = linkedMapOf<Int, Int>()
                    val pending = mutableListOf<PendingWidgetRestore>()
                    widgets.forEach { widget ->
                        val id = host.allocateAppWidgetId().also { allocated.add(it) }
                        mapping[widget.id] = id
                        widget.clock?.let { ClockWidgetConfig.restoreBackup(context, id, it, replaceExisting = true) }
                        val bound = widget.provider != null && runCatching {
                            manager.bindAppWidgetIdIfAllowed(id, widget.provider, optionsBundle(widget.options))
                        }.getOrDefault(false)
                        val info = if (bound) manager.getAppWidgetInfo(id) else null
                        if (!bound || (info?.configure != null && info.provider != clock)) {
                            pending.add(PendingWidgetRestore(id, widget.provider?.flattenToString(), widget.label, widget.options))
                        }
                    }
                    BackupValues.remapWidgets(values, mapping)
                    values[BackupValues.PENDING_WIDGETS] = pendingWidgetsToJson(pending)
                    values[BackupValues.BACKUP_RESTORED] = true
                    prefs.replaceBackupValues(values)
                    committed = true
                    previousIds.filter { it in ownedBefore && it !in mapping.values }.forEach {
                        runCatching { host.deleteAppWidgetId(it) }
                    }
                    runCatching { ClockWidgetProvider.syncWeatherReceiver(context) }
                    runCatching { ClockWidgetProvider.renderAll(context) }
                    RestoreReport(pending.size)
                }
            } catch (failure: Exception) {
                if (!committed) {
                    allocated.forEach { id ->
                        runCatching { host.deleteAppWidgetId(id) }
                        runCatching { ClockWidgetConfig.forget(context, id) }
                    }
                    directory?.deleteRecursively()
                }
                null
            }
        }
    }

    companion object {
        private const val FORMAT = 2
        private const val APP_NAME = "Nike Launcher"
        private val operations = Mutex()

        internal fun optionsBundle(values: Map<String, Int>) = Bundle().apply {
            values.forEach { (key, value) -> putInt(key, value) }
        }
    }
}
