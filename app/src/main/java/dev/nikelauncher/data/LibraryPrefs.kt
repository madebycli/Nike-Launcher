// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** The two swipe directions under the favorites that can launch an app. */
enum class QuickLaunchSlot { LEFT, RIGHT }

/** Which edge an app is carried in from when it is opened by a sideways swipe. */

/**
 * Where the favorites section gets its apps.
 *
 * FREQUENT is the same section drawn from what has actually been opened, so everything that
 * renders a favorite renders one of these too — it is the list that is computed, not the row.
 */
enum class FavoritesSource { MANUAL, FREQUENT }

/** Enough to be a shortlist, not so many that the section becomes the app list again. */
val FREQUENT_RANGE = 3..12

class LibraryPrefs internal constructor(private val store: DataStore<Preferences>) {

    private val data get() = store.data

    /** Favorites are stored as one newline-joined string; these are the only two readers. */
    private fun readFavorites(pref: Preferences): List<String> =
        pref[PrefKeys.FAVORITES]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun MutablePreferences.writeFavorites(list: List<String>) {
        this[PrefKeys.FAVORITES] = list.joinToString("\n")
    }

    val hiddenApps: Flow<Set<String>> =
        data.map { it[PrefKeys.HIDDEN_APPS] ?: emptySet() }.distinctUntilChanged()

    val favorites: Flow<List<String>> =
        data.map { pref -> readFavorites(pref) }.distinctUntilChanged()

    val folders: Flow<List<Folder>> =
        data.map { pref -> foldersFromJson(pref[PrefKeys.FOLDERS]) }.distinctUntilChanged()

    val nameOverrides: Flow<Map<String, String>> =
        data.map { pref -> jsonToMap(pref[PrefKeys.NAME_OVERRIDES]) }.distinctUntilChanged()

    val iconOverrides: Flow<Map<String, String>> =
        data.map { pref -> jsonToMap(pref[PrefKeys.ICON_OVERRIDES]) }.distinctUntilChanged()

    /** Orders each letter's apps by how often they were opened from here, rather than by name. */
    val sortByUsage: Flow<Boolean> = data.map { it[PrefKeys.SORT_BY_USAGE] ?: false }.distinctUntilChanged()

    val launchCounts: Flow<Map<String, Int>> = data.map { pref ->
        jsonToMap(pref[PrefKeys.LAUNCH_COUNTS]).mapValues { (_, v) -> v.toIntOrNull() ?: 0 }
    }.distinctUntilChanged()

    val quickLaunchLeft: Flow<String?> = data.map { it[PrefKeys.QUICK_LAUNCH_LEFT] }.distinctUntilChanged()

    val quickLaunchRight: Flow<String?> = data.map { it[PrefKeys.QUICK_LAUNCH_RIGHT] }.distinctUntilChanged()

    /**
     * Apps kept out of the computed favorites, while still in the A-Z list.
     *
     * Hiding an app takes it out of everywhere, which is the wrong tool for an app you open
     * often and would rather not have a tap away. Only consulted while the favorites are the
     * computed ones; a manual list already says what is on it.
     */
    val frequentExcluded: Flow<Set<String>> =
        data.map { it[PrefKeys.FREQUENT_EXCLUDED] ?: emptySet() }.distinctUntilChanged()

    val favoritesSource: Flow<FavoritesSource> =
        data.map { prefs ->
            prefs[PrefKeys.FAVORITES_SOURCE]
                ?.let { name -> runCatching { FavoritesSource.valueOf(name) }.getOrNull() }
                ?: FavoritesSource.MANUAL
        }.distinctUntilChanged()

    /** How many of the most-opened apps the favorites section shows when it is computed. */
    val frequentCount: Flow<Int> =
        data.map { (it[PrefKeys.FREQUENT_COUNT] ?: 6).coerceIn(FREQUENT_RANGE) }.distinctUntilChanged()

    /**
     * The app a button in the bottom corner opens, or null for no button at all.
     *
     * Null rather than a flag beside a key: there is nothing to draw without an app to draw it
     * for, so the two cannot disagree.
     */
    val cornerButtonKey: Flow<String?> = data.map { it[PrefKeys.CORNER_BUTTON_KEY] }.distinctUntilChanged()

    /**
     * Whether there has ever been a favorite on the home screen.
     *
     * The empty-favorites prompt is for someone who does not yet know how, and it had no way of
     * telling them from someone who knows exactly how and wants none — so a bare home screen by
     * choice carried the hint for good.
     */
    val favoritesEverSet: Flow<Boolean> =
        data.map { it[PrefKeys.FAVORITES_EVER_SET] ?: false }.distinctUntilChanged()

    suspend fun setHidden(componentKey: String, hidden: Boolean) {
        store.edit { pref ->
            val current = pref[PrefKeys.HIDDEN_APPS] ?: emptySet()
            pref[PrefKeys.HIDDEN_APPS] = if (hidden) current + componentKey else current - componentKey
        }
    }

    suspend fun setFavorites(list: List<String>) {
        store.edit { it.writeFavorites(list) }
    }

    suspend fun addFavorite(componentKey: String) {
        store.edit { pref ->
            val current = readFavorites(pref)
            if (componentKey !in current) pref.writeFavorites(current + componentKey)
        }
    }

    suspend fun removeFavorite(componentKey: String) {
        store.edit { pref ->
            pref.writeFavorites(readFavorites(pref) - componentKey)
        }
    }

    /**
     * Everything stored about one row, dropped in a single write. Only for a row that is gone
     * for good — a shortcut its publisher withdrew, which [AppRepository.unpin] calls once the
     * system itself has unpinned it. Never for a row that might merely be unreachable right
     * now: an app in a locked private space or a paused work profile also drops out of the
     * list, but its name, icon and place are still waiting when the profile comes back, so
     * unticking it in Manage Favorites only removes the favorite ([removeFavorite]), not this.
     *
     * A shortcut that has been unpinned does not come back, so what is kept under its key is
     * only clutter nothing can reach, and it would be restored onto a new phone by the settings
     * backup as well.
     *
     * One transaction rather than seven, so a row cannot end up half forgotten.
     */
    suspend fun forgetEntry(componentKey: String) {
        store.edit { pref ->
            val favorites = readFavorites(pref)
            if (componentKey in favorites) pref.writeFavorites(favorites - componentKey)

            val folders = foldersFromJson(pref[PrefKeys.FOLDERS])
            if (folders.any { componentKey in it.apps }) {
                pref[PrefKeys.FOLDERS] = foldersToJson(folders.map { it.copy(apps = it.apps - componentKey) })
            }

            val names = jsonToMap(pref[PrefKeys.NAME_OVERRIDES])
            if (componentKey in names) pref[PrefKeys.NAME_OVERRIDES] = mapToJson(names - componentKey)

            val icons = jsonToMap(pref[PrefKeys.ICON_OVERRIDES])
            if (componentKey in icons) pref[PrefKeys.ICON_OVERRIDES] = mapToJson(icons - componentKey)

            val hidden = pref[PrefKeys.HIDDEN_APPS] ?: emptySet()
            if (componentKey in hidden) pref[PrefKeys.HIDDEN_APPS] = hidden - componentKey

            val counts = jsonToMap(pref[PrefKeys.LAUNCH_COUNTS])
            if (componentKey in counts) pref[PrefKeys.LAUNCH_COUNTS] = mapToJson(counts - componentKey)

            // A quick-launch slot that still points at this key would otherwise swipe to
            // nothing once the row is gone for good.
            if (pref[PrefKeys.CORNER_BUTTON_KEY] == componentKey) pref.remove(PrefKeys.CORNER_BUTTON_KEY)
            if (pref[PrefKeys.QUICK_LAUNCH_LEFT] == componentKey) pref.remove(PrefKeys.QUICK_LAUNCH_LEFT)
            if (pref[PrefKeys.QUICK_LAUNCH_RIGHT] == componentKey) pref.remove(PrefKeys.QUICK_LAUNCH_RIGHT)
        }
    }

    /** Creates the folder if [id] is new, otherwise replaces it. */
    suspend fun upsertFolder(folder: Folder) {
        store.edit { pref ->
            val existing = foldersFromJson(pref[PrefKeys.FOLDERS])
            val updated = if (existing.any { it.id == folder.id }) {
                existing.map { if (it.id == folder.id) folder else it }
            } else {
                existing + folder
            }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
        }
    }

    /** Removes the folder and its row; the apps themselves are untouched. */
    suspend fun deleteFolder(id: String) {
        store.edit { pref ->
            pref[PrefKeys.FOLDERS] = foldersToJson(foldersFromJson(pref[PrefKeys.FOLDERS]).filterNot { it.id == id })
            pref.writeFavorites(readFavorites(pref).filterNot { it == folderToken(id) })
        }
    }

    suspend fun setFolderIcon(id: String, icon: String?) {
        store.edit { pref ->
            val updated = foldersFromJson(pref[PrefKeys.FOLDERS])
                .map { if (it.id == id) it.copy(icon = icon) else it }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
        }
    }

    suspend fun setFolderApps(id: String, apps: List<String>) {
        store.edit { pref ->
            val updated = foldersFromJson(pref[PrefKeys.FOLDERS])
                .map { if (it.id == id) it.copy(apps = apps) else it }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
        }
    }

    /**
     * Puts [componentKey] in the folder. An app that was a top-level favorite moves into it
     * rather than being duplicated in both places.
     */
    suspend fun addAppToFolder(folderId: String, componentKey: String) {
        store.edit { pref ->
            val updated = foldersFromJson(pref[PrefKeys.FOLDERS]).map { folder ->
                if (folder.id == folderId && componentKey !in folder.apps) {
                    folder.copy(apps = folder.apps + componentKey)
                } else {
                    folder
                }
            }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
            pref.writeFavorites(readFavorites(pref).filterNot { it == componentKey })
        }
    }

    /** Changes one folder in place, leaving the rest of the list as it was. */
    private suspend fun updateFolder(folderId: String, change: (Folder) -> Folder) {
        store.edit { pref ->
            val updated = foldersFromJson(pref[PrefKeys.FOLDERS]).map { if (it.id == folderId) change(it) else it }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
        }
    }

    suspend fun setFolderNowPlaying(folderId: String, on: Boolean) = updateFolder(folderId) {
        it.copy(nowPlaying = on)
    }

    suspend fun addFolderWidget(folderId: String, widgetId: Int) = updateFolder(folderId) {
        it.copy(widgets = it.widgets + FolderWidget(widgetId, FOLDER_WIDGET_DEFAULT_HEIGHT_DP))
    }

    suspend fun removeFolderWidget(folderId: String, widgetId: Int) = updateFolder(folderId) {
        it.copy(widgets = it.widgets.filterNot { w -> w.id == widgetId })
    }

    suspend fun setFolderWidgetHeight(folderId: String, widgetId: Int, heightDp: Int) = updateFolder(folderId) {
        it.copy(widgets = it.widgets.map { w -> if (w.id == widgetId) w.copy(heightDp = heightDp) else w })
    }

    suspend fun removeAppFromFolder(folderId: String, componentKey: String) {
        store.edit { pref ->
            val updated = foldersFromJson(pref[PrefKeys.FOLDERS]).map { folder ->
                if (folder.id == folderId) folder.copy(apps = folder.apps - componentKey) else folder
            }
            pref[PrefKeys.FOLDERS] = foldersToJson(updated)
        }
    }

    suspend fun setNameOverride(componentKey: String, name: String?) {
        store.edit { pref ->
            val map = jsonToMap(pref[PrefKeys.NAME_OVERRIDES]).toMutableMap()
            if (name.isNullOrBlank()) map.remove(componentKey) else map[componentKey] = name
            pref[PrefKeys.NAME_OVERRIDES] = mapToJson(map)
        }
    }

    suspend fun setIconOverride(componentKey: String, uri: String?) {
        store.edit { pref ->
            val map = jsonToMap(pref[PrefKeys.ICON_OVERRIDES]).toMutableMap()
            if (uri.isNullOrBlank()) map.remove(componentKey) else map[componentKey] = uri
            pref[PrefKeys.ICON_OVERRIDES] = mapToJson(map)
        }
    }

    suspend fun setSortByUsage(v: Boolean) {
        store.edit { it[PrefKeys.SORT_BY_USAGE] = v }
    }

    /**
     * Counted for every launch, not just while the usage sort is on, so turning the sort on
     * later has a history to order by rather than starting from nothing.
     */
    suspend fun incrementLaunchCount(componentKey: String) {
        store.edit { pref ->
            val map = jsonToMap(pref[PrefKeys.LAUNCH_COUNTS]).toMutableMap()
            map[componentKey] = ((map[componentKey]?.toIntOrNull() ?: 0) + 1).toString()
            pref[PrefKeys.LAUNCH_COUNTS] = mapToJson(map)
        }
    }

    suspend fun setQuickLaunch(slot: QuickLaunchSlot, componentKey: String?) {
        val key = when (slot) {
            QuickLaunchSlot.LEFT -> PrefKeys.QUICK_LAUNCH_LEFT
            QuickLaunchSlot.RIGHT -> PrefKeys.QUICK_LAUNCH_RIGHT
        }
        store.edit { pref ->
            if (componentKey.isNullOrBlank()) pref.remove(key) else pref[key] = componentKey
        }
    }

    suspend fun setFavoritesSource(v: FavoritesSource) {
        store.edit { it[PrefKeys.FAVORITES_SOURCE] = v.name }
    }

    suspend fun setFrequentCount(v: Int) {
        store.edit { it[PrefKeys.FREQUENT_COUNT] = v.coerceIn(FREQUENT_RANGE) }
    }

    suspend fun setCornerButton(componentKey: String?) {
        store.edit { pref ->
            if (componentKey.isNullOrBlank()) pref.remove(PrefKeys.CORNER_BUTTON_KEY)
            else pref[PrefKeys.CORNER_BUTTON_KEY] = componentKey
        }
    }

    suspend fun setFrequentExcluded(key: String, excluded: Boolean) {
        store.edit { prefs ->
            val current = prefs[PrefKeys.FREQUENT_EXCLUDED] ?: emptySet()
            prefs[PrefKeys.FREQUENT_EXCLUDED] =
                if (excluded) current + key else current - key
        }
    }

    suspend fun markFavoritesEverSet() {
        store.edit {
            if (it[PrefKeys.FAVORITES_EVER_SET] != true) it[PrefKeys.FAVORITES_EVER_SET] = true
        }
    }

    private fun jsonToMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { k -> map[k] = obj.getString(k) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun mapToJson(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }
}
