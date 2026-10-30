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
import dev.nikelauncher.data.backup.BackupValues
import dev.nikelauncher.data.backup.PendingWidgetRestore
import dev.nikelauncher.data.backup.RestoreReport
import dev.nikelauncher.data.backup.SettingsBackup
import dev.nikelauncher.data.backup.pendingWidgetsFromJson
import dev.nikelauncher.data.backup.pendingWidgetsToJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nike_prefs")

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



    /** The backup, with the widgets and files it needs: see [SettingsBackup]. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun exportJson(stripOtherProfiles: Boolean): ExportResult = SettingsBackup(context, this).export()

    /** Replaces everything with the backup in [text]; null when it is not one of ours or is damaged. */
    suspend fun importJson(text: String): RestoreReport? = SettingsBackup(context, this).restore(text)

    internal suspend fun storedValues(): Map<String, Any> = data.first().asMap().mapKeys { it.key.name }

    internal suspend fun replaceBackupValues(values: Map<String, Any>) {
        store.edit { pref ->
            pref.clear()
            values.forEach { (name, value) ->
                val key: Preferences.Key<*> = when (value) {
                    is Boolean -> booleanPreferencesKey(name)
                    is Int -> intPreferencesKey(name)
                    is Long -> longPreferencesKey(name)
                    is Float -> floatPreferencesKey(name)
                    is String -> stringPreferencesKey(name)
                    is Set<*> -> stringSetPreferencesKey(name)
                    else -> error("Invalid preference type")
                }
                @Suppress("UNCHECKED_CAST")
                pref[key as Preferences.Key<Any>] = value
            }
        }
    }

    /** Whether the settings came from a backup, which a fresh install's defaults must then leave alone. */
    val backupRestored: Flow<Boolean> =
        data.map { it[booleanPreferencesKey(BackupValues.BACKUP_RESTORED)] ?: false }.distinctUntilChanged()

    val pendingWidgetRestores: Flow<List<PendingWidgetRestore>> =
        data.map { pendingWidgetsFromJson(it[stringPreferencesKey(BackupValues.PENDING_WIDGETS)]) }.distinctUntilChanged()

    /** Marks a pending widget done, and when it was replaced by another, points everything at that one. */
    suspend fun completeWidgetRestore(id: Int, replacementId: Int = id) {
        store.edit { pref ->
            if (replacementId != id) {
                val values = pref.asMap().mapKeys { it.key.name }.toMutableMap()
                val mapping = BackupValues.widgetIds(values).associateWith { if (it == id) replacementId else it }
                BackupValues.remapWidgets(values, mapping)
                pref[PrefKeys.WIDGET_IDS] = values["widget_ids"] as String
                pref[PrefKeys.WIDGET_ID] = values["widget_id"] as Int
                (values[PREF_FOLDERS] as? String)?.let { pref[PrefKeys.FOLDERS] = it }
            }
            val key = stringPreferencesKey(BackupValues.PENDING_WIDGETS)
            pref[key] = pendingWidgetsToJson(pendingWidgetsFromJson(pref[key]).filterNot { it.id == id })
        }
    }
}
