// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dev.nikelauncher.data.PrefKeys
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class SearchDisplayMode { NONE, BUTTON, BAR }

internal fun searchDisplayMode(bar: Boolean, floating: Boolean) =
    if (bar) SearchDisplayMode.BAR else if (floating) SearchDisplayMode.BUTTON else SearchDisplayMode.NONE

/** One choice writes the existing independent keys atomically; no stored key changes type or name. */
class SearchPrefs internal constructor(private val store: DataStore<Preferences>) {
    val mode = store.data.map { searchDisplayMode(it[PrefKeys.APPLIST_SEARCH_ENABLED] ?: false, it[FLOATING] ?: true) }.distinctUntilChanged()
    val buttonAtBottom = store.data.map { it[BOTTOM] ?: true }.distinctUntilChanged()
    suspend fun setMode(value: SearchDisplayMode) {
        store.edit { it[PrefKeys.APPLIST_SEARCH_ENABLED] = value == SearchDisplayMode.BAR; it[FLOATING] = value == SearchDisplayMode.BUTTON }
    }
    suspend fun setButtonAtBottom(value: Boolean) { store.edit { it[BOTTOM] = value } }
    private companion object {
        val FLOATING = booleanPreferencesKey("search_button_floating")
        val BOTTOM = booleanPreferencesKey("search_button_at_bottom")
    }
}
