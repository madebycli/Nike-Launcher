// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.validateBackup
import org.json.JSONArray

/** A widget as a backup file holds it. [clock] is its options when it is this launcher's own clock. */
internal data class SavedWidget(
    val id: Int,
    val provider: ComponentName?,
    val label: String,
    val options: Map<String, Int>,
    val clock: Map<String, Any>?,
)

private val optionNames = setOf(
    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
    AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
    AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
)

internal fun readOptions(bundle: Bundle): Map<String, Int> =
    optionNames.filter { bundle.containsKey(it) }.associateWith { bundle.getInt(it) }

/** Checks the widgets of a file against the ids its values refer to: the same set, each once. */
internal fun readWidgets(array: JSONArray, ids: List<Int>, clock: ComponentName): List<SavedWidget> {
    require(array.length() == ids.size)
    val widgets = (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val raw = item.optString("provider").takeIf { it.isNotBlank() && it != "null" }
        require(raw == null || raw.length <= 512)
        val provider = raw?.let { requireNotNull(ComponentName.unflattenFromString(it)) }
        val options = item.getJSONObject("options")
        require(options.keys().asSequence().all { it in optionNames })
        val dimensions = options.keys().asSequence().associateWith { options.getInt(it).also { value -> require(value in 0..10000) } }
        val clockOptions = item.optJSONObject("clock")?.let {
            require(provider == clock)
            ClockWidgetConfig.validateBackup(it)
        }
        if (provider == clock) require(clockOptions != null)
        SavedWidget(item.getInt("id"), provider, item.getString("label").take(160), dimensions, clockOptions)
    }
    require(widgets.map { it.id }.toSet() == ids.toSet() && widgets.map { it.id }.distinct().size == widgets.size)
    return widgets
}
