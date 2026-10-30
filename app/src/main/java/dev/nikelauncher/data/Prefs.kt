// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "nike_prefs")

/** Bumped only if the shape of an exported file changes, so an old one can be refused. */
private const val EXPORT_FORMAT = 1

/** [json] to write out, and whether anything belonging to the private space was left out of it. */
data class ExportResult(val json: String, val omittedPrivateSpace: Boolean)

/**
 * Every stored setting, grouped by what it belongs to. The groups hold the flows and setters;
 * this class owns the store and the backup, which spans all of them.
 */
class Prefs(private val context: Context) {
    private val store = context.dataStore
    private val data get() = store.data

    val library = LibraryPrefs(store)
    val appearance = AppearancePrefs(store)
    val home = HomePrefs(store)
    val appList = AppListPrefs(store)
    val strip = StripPrefs(store)



    /**
     * Every stored setting as JSON, for moving a set-up to another phone or keeping it.
     *
     * Written from the store itself rather than a list of keys kept alongside it, because a
     * list like that is only right until the next setting is added and nobody remembers it.
     * The type travels with each value, since JSON cannot tell 1 from 1L from 1.0f and the
     * store very much can.
     *
     * A custom font is a path into this app's own storage, so it points at nothing on another
     * phone; the font falls back to the default there until one is picked again.
     *
     * [privateSerial] is the private space's profile serial, when there is one, so its apps —
     * and how often they were opened — never leave the device in a file the user can save
     * anywhere. Passed in rather than looked up here because resolving it needs Android types
     * this class otherwise avoids; the caller already has it from the same read that decided
     * what to show on screen.
     */
    suspend fun exportJson(stripOtherProfiles: Boolean): ExportResult {
        val stored = data.first()
        val values = JSONObject()
        var omittedPrivateSpace = false
        stored.asMap().forEach { (key, rawValue) ->
            val value = stripPrivateSpaceFromExport(key.name, rawValue, stripOtherProfiles)
            if (value == null) {
                omittedPrivateSpace = true
                return@forEach
            }
            if (value != rawValue) omittedPrivateSpace = true
            val entry = JSONObject()
            when (value) {
                is Boolean -> entry.put("type", "boolean").put("value", value)
                is Int -> entry.put("type", "int").put("value", value)
                is Long -> entry.put("type", "long").put("value", value)
                is Float -> entry.put("type", "float").put("value", value.toDouble())
                is String -> entry.put("type", "string").put("value", value)
                is Set<*> -> entry.put("type", "stringSet")
                    .put("value", JSONArray().apply { value.forEach { put(it.toString()) } })
                else -> return@forEach
            }
            values.put(key.name, entry)
        }
        val json = JSONObject()
            .put("format", EXPORT_FORMAT)
            .put("app", "Nike Launcher")
            .put("values", values)
            .toString(2)
        return ExportResult(json, omittedPrivateSpace)
    }

    /**
     * Replaces every setting with the ones in [text]. Returns false if it is not ours.
     *
     * Parsed fully before anything is written: a half-read file that had already cleared the
     * store would leave someone with neither their old set-up nor the one they were restoring.
     */
    suspend fun importJson(text: String): Boolean {
        val parsed = runCatching {
            val root = JSONObject(text)
            if (root.optInt("format") != EXPORT_FORMAT) return false
            val values = root.getJSONObject("values")
            buildList {
                values.keys().forEach { name ->
                    val entry = values.getJSONObject(name)
                    // Pair(...) rather than `to`: DataStore has an infix `to` of its own on
                    // Key, which builds a Preferences.Pair and not the one wanted here.
                    val pair: Pair<Preferences.Key<*>, Any> = when (entry.getString("type")) {
                        "boolean" -> Pair(booleanPreferencesKey(name), entry.getBoolean("value"))
                        "int" -> Pair(intPreferencesKey(name), entry.getInt("value"))
                        "long" -> Pair(longPreferencesKey(name), entry.getLong("value"))
                        "float" -> Pair(floatPreferencesKey(name), entry.getDouble("value").toFloat())
                        "string" -> Pair(stringPreferencesKey(name), entry.getString("value"))
                        "stringSet" -> {
                            val array = entry.getJSONArray("value")
                            Pair(
                                stringSetPreferencesKey(name),
                                (0 until array.length()).map { array.getString(it) }.toSet(),
                            )
                        }
                        else -> return@forEach
                    }
                    add(pair)
                }
            }
        }.getOrNull() ?: return false

        store.edit { store ->
            store.clear()
            parsed.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                store[key as Preferences.Key<Any>] = value
            }
        }
        return true
    }
}
