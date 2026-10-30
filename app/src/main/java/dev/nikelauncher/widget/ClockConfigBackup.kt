// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import dev.nikelauncher.data.backup.BackupValues
import org.json.JSONObject

private const val FILE = "clock_widget"

/** The options a clock widget stores, by name; anything else in a backup is refused. */
private val CLOCK_OPTION_NAMES = setOf(
    "hour", "date", "size", "dateSize", "battery", "weather", "weatherSize", "weatherF", "weatherPkg",
    "notifications", "agenda", "agendaSize", "agendaCount", "agendaHours", "agendaRelative", "alarm",
    "notificationsSize", "batterySize", "color", "timeOpacity", "dateOpacity", "batteryOpacity",
    "weatherOpacity", "notificationsOpacity", "agendaOpacity",
)

/** Every option this widget has stored, so a backup carries the clock's settings with the widget. */
internal fun ClockWidgetConfig.Companion.backupValues(context: Context, widgetId: Int): Map<String, Any> {
    val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    val suffix = "_$widgetId"
    return buildMap {
        prefs.all.forEach { (key, value) ->
            if (!key.endsWith(suffix)) return@forEach
            val name = key.removeSuffix(suffix)
            if (name !in CLOCK_OPTION_NAMES) return@forEach
            when (value) {
                is String, is Boolean, is Int -> put(name, value)
            }
        }
    }
}

/** Checks the options of a backed-up clock before anything is written. */
internal fun ClockWidgetConfig.Companion.validateBackup(values: JSONObject): Map<String, Any> {
    val result = BackupValues.decode(values)
    require(result.keys.all { it in CLOCK_OPTION_NAMES })
    result.forEach { (_, value) -> require(value is String || value is Boolean || value is Int) }
    (result["date"] as? String)?.let { require(it.isEmpty() || isValidDatePattern(it)) }
    return result
}

/**
 * Writes backed-up options under a widget id. A new Android widget id may have belonged to a
 * deleted widget before, so with [replaceExisting] its whole old snapshot goes in the same edit:
 * a backup from an older version lacks fields added since, and would otherwise inherit stale ones.
 */
internal fun ClockWidgetConfig.Companion.restoreBackup(
    context: Context,
    widgetId: Int,
    values: Map<String, Any>,
    replaceExisting: Boolean = false,
) {
    val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    if (replaceExisting) prefs.all.keys.filter { it.endsWith("_$widgetId") }.forEach(editor::remove)
    values.forEach { (name, value) ->
        val key = name + "_" + widgetId
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            else -> error("Invalid clock option")
        }
    }
    editor.commit()
}
