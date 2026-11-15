// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class MediaWidgetStyle { CLASSIC, EXPRESSIVE }
data class MediaWidgetOptions(val style: MediaWidgetStyle = MediaWidgetStyle.CLASSIC,
    val controls: Boolean = true, val progress: Boolean = false)

/** Existing installs keep the original card until another style is selected. */
class MediaWidgetPrefs internal constructor(private val store: DataStore<Preferences>) {
    val options = store.data.map { p -> MediaWidgetOptions(
        style = p[STYLE]?.let { runCatching { MediaWidgetStyle.valueOf(it) }.getOrNull() } ?: MediaWidgetStyle.CLASSIC,
        controls = p[CONTROLS] ?: true, progress = p[PROGRESS] ?: false,
    ) }.distinctUntilChanged()
    suspend fun setStyle(value: MediaWidgetStyle) { store.edit { it[STYLE] = value.name } }
    suspend fun setControls(value: Boolean) { store.edit { it[CONTROLS] = value } }
    suspend fun setProgress(value: Boolean) { store.edit { it[PROGRESS] = value } }
    private companion object {
        val STYLE = stringPreferencesKey("media_widget_style")
        val CONTROLS = booleanPreferencesKey("media_widget_controls")
        val PROGRESS = booleanPreferencesKey("media_widget_progress")
    }
}
