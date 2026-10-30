// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import dev.nikelauncher.data.PREF_FOLDERS
import dev.nikelauncher.data.foldersFromJson
import dev.nikelauncher.data.foldersToJson
import org.json.JSONArray
import org.json.JSONObject

/** Reading and writing the typed values of a backup file, and the widget ids inside them. */
internal object BackupValues {
    const val MAX_FILE_BYTES = 64 * 1024 * 1024
    const val MAX_ASSET_BYTES = 8 * 1024 * 1024
    const val MAX_ASSETS = 128
    const val MAX_WIDGETS = 100
    const val PENDING_WIDGETS = "restore_widgets_pending_json"
    const val BACKUP_RESTORED = "backup_restored"

    private const val WIDGET_IDS = "widget_ids"
    private const val WIDGET_ID = "widget_id"

    fun encode(values: Map<String, Any>): JSONObject = JSONObject().apply {
        values.toSortedMap().forEach { (name, value) ->
            val type = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is String -> "string"
                is Set<*> -> "stringSet"
                else -> error("Unsupported backup value")
            }
            put(
                name,
                JSONObject().put("type", type).put(
                    "value",
                    if (value is Set<*>) JSONArray(value.filterIsInstance<String>().sorted()) else value,
                ),
            )
        }
    }

    /**
     * Every value checked for its type and size before any of it is used. [knownTypes] pins the
     * type of a name that must not change shape, such as a clock option.
     */
    fun decode(values: JSONObject, knownTypes: Map<String, String> = emptyMap()): Map<String, Any> {
        require(values.length() <= 4096)
        return buildMap {
            values.keys().forEach { name ->
                require(name.isNotBlank() && name.length <= 160)
                val entry = values.getJSONObject(name)
                val type = entry.getString("type")
                require(knownTypes[name] == null || knownTypes[name] == type)
                val raw = entry.get("value")
                val value: Any = when (type) {
                    "boolean" -> { require(raw is Boolean); raw }
                    "int" -> {
                        require(raw is Int || raw is Long)
                        val number = (raw as Number).toLong()
                        require(number in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                        number.toInt()
                    }
                    "long" -> { require(raw is Int || raw is Long); (raw as Number).toLong() }
                    "float" -> {
                        require(raw is Number)
                        raw.toFloat().also { require(it.isFinite()) }
                    }
                    "string" -> { require(raw is String); raw }
                    "stringSet" -> {
                        require(raw is JSONArray && raw.length() <= 10000)
                        (0 until raw.length()).map {
                            raw.get(it).also { item -> require(item is String) } as String
                        }.toSet()
                    }
                    else -> error("Unknown backup value type")
                }
                put(name, value)
            }
        }
    }

    private fun homeWidgets(values: Map<String, Any>): List<Int> =
        (values[WIDGET_IDS] as? String)?.let { raw ->
            if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim().toInt().also { id -> require(id > 0) } }
        } ?: listOfNotNull((values[WIDGET_ID] as? Int)?.takeIf { it > 0 })

    /** Every widget the values refer to: on the home screen and inside folders. */
    fun widgetIds(values: Map<String, Any>): List<Int> {
        val folderIds = (values[PREF_FOLDERS] as? String)?.let { raw ->
            val folders = JSONArray(raw)
            buildList {
                for (index in 0 until folders.length()) {
                    val widgets = folders.getJSONObject(index).optJSONArray("widgets") ?: continue
                    require(widgets.length() <= MAX_WIDGETS)
                    for (item in 0 until widgets.length()) {
                        val widget = widgets.getJSONObject(item)
                        require(widget.getInt("id") > 0 && widget.getInt("h") in 80..900)
                        add(widget.getInt("id"))
                    }
                }
            }
        }.orEmpty()
        val all = homeWidgets(values) + folderIds
        require(all.size <= MAX_WIDGETS && all.distinct().size == all.size)
        return all
    }

    /** Points every reference at the id the widget has now, once it has been given a new one. */
    fun remapWidgets(values: MutableMap<String, Any>, mapping: Map<Int, Int>) {
        val home = homeWidgets(values)
        values[WIDGET_IDS] = home.map { mapping.getValue(it) }.joinToString(",")
        values[WIDGET_ID] = home.firstOrNull()?.let { mapping.getValue(it) } ?: -1
        (values[PREF_FOLDERS] as? String)?.let { raw ->
            values[PREF_FOLDERS] = foldersToJson(
                foldersFromJson(raw).map { folder -> folder.copy(widgets = folder.widgets.map { it.copy(id = mapping.getValue(it.id)) }) },
            )
        }
    }

    /** Takes widgets out of the values, for those that belong to a profile the file must not leave. */
    fun removeWidgets(values: MutableMap<String, Any>, ids: Set<Int>) {
        val kept = homeWidgets(values).filterNot { it in ids }
        values[WIDGET_IDS] = kept.joinToString(",")
        values[WIDGET_ID] = kept.firstOrNull() ?: -1
        (values[PREF_FOLDERS] as? String)?.let { raw ->
            values[PREF_FOLDERS] = foldersToJson(
                foldersFromJson(raw).map { folder -> folder.copy(widgets = folder.widgets.filterNot { it.id in ids }) },
            )
        }
    }
}
