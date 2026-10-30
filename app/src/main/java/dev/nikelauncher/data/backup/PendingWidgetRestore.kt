// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import org.json.JSONArray
import org.json.JSONObject

/** A widget a restore could not bind by itself and is waiting for the user to set up. */
data class PendingWidgetRestore(
    val id: Int,
    val provider: String?,
    val label: String,
    val options: Map<String, Int> = emptyMap(),
)

internal fun pendingWidgetsToJson(items: List<PendingWidgetRestore>): String = JSONArray().apply {
    items.forEach { item ->
        put(
            JSONObject().put("id", item.id).put("provider", item.provider ?: JSONObject.NULL)
                .put("label", item.label).put("options", JSONObject(item.options)),
        )
    }
}.toString()

internal fun pendingWidgetsFromJson(raw: String?): List<PendingWidgetRestore> = runCatching {
    val array = JSONArray(raw ?: "[]")
    require(array.length() <= BackupValues.MAX_WIDGETS)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val options = item.optJSONObject("options") ?: JSONObject()
        PendingWidgetRestore(
            item.getInt("id"),
            item.optString("provider").takeIf { it.isNotBlank() && it != "null" },
            item.optString("label", "Widget"),
            options.keys().asSequence().associateWith { options.getInt(it) },
        )
    }
}.getOrDefault(emptyList())
