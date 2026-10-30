// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The corner button: what it does, how it looks. Which app it opens is kept with the library. */
class CornerPrefs internal constructor(private val store: DataStore<Preferences>) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("corner_enabled")
        val TAP = stringPreferencesKey("corner_tap")
        val SWIPE = stringPreferencesKey("corner_swipe")
        val SHAPE = stringPreferencesKey("corner_shape")
        val SIZE = intPreferencesKey("corner_size")
        val COLOR_MODE = stringPreferencesKey("corner_color_mode")
        val BACKGROUND = intPreferencesKey("corner_background")
        val FOREGROUND = intPreferencesKey("corner_foreground")
        val AUTO_FOREGROUND = booleanPreferencesKey("corner_auto_foreground")
        val BACKGROUND_OPACITY = intPreferencesKey("corner_background_opacity")
        val BUTTON_KEY = stringPreferencesKey("corner_button_key")
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> runCatching { enumValueOf<E>(name) }.getOrNull() } ?: default

    val settings: Flow<CornerSettings> = store.data.map { pref ->
        CornerSettings(
            enabled = pref[Keys.ENABLED] ?: true,
            // A phone that already had an app or the search chosen keeps doing exactly that.
            tap = pref[Keys.TAP]?.let { runCatching { CornerAction.valueOf(it) }.getOrNull() }
                ?: when (pref[Keys.BUTTON_KEY]) {
                    EntryKeys.SEARCH -> CornerAction.SEARCH
                    null -> CornerAction.MENU
                    else -> CornerAction.APP
                },
            swipe = pref[Keys.SWIPE].toEnum(CornerAction.MENU),
            shape = pref[Keys.SHAPE].toEnum(CornerShape.CIRCLE),
            sizeDp = (pref[Keys.SIZE] ?: 56).coerceIn(CORNER_SIZE_RANGE),
            appearance = CornerAppearance(
                colorMode = pref[Keys.COLOR_MODE].toEnum(CornerColorMode.CLASSIC),
                background = pref[Keys.BACKGROUND] ?: CornerAppearance().background,
                foreground = pref[Keys.FOREGROUND] ?: CornerAppearance().foreground,
                automaticForeground = pref[Keys.AUTO_FOREGROUND] ?: true,
                backgroundOpacity = (pref[Keys.BACKGROUND_OPACITY] ?: 100).coerceIn(0, 100),
            ),
        )
    }.distinctUntilChanged()

    suspend fun setEnabled(v: Boolean) = store.edit { it[Keys.ENABLED] = v }.let { }
    suspend fun setTap(v: CornerAction) = store.edit { it[Keys.TAP] = v.name }.let { }
    suspend fun setSwipe(v: CornerAction) = store.edit { it[Keys.SWIPE] = v.name }.let { }
    suspend fun setShape(v: CornerShape) = store.edit { it[Keys.SHAPE] = v.name }.let { }
    suspend fun setSize(v: Int) = store.edit { it[Keys.SIZE] = v.coerceIn(CORNER_SIZE_RANGE) }.let { }
    suspend fun setColorMode(v: CornerColorMode) = store.edit { it[Keys.COLOR_MODE] = v.name }.let { }
    suspend fun setBackground(v: Int) = store.edit { it[Keys.BACKGROUND] = v }.let { }
    suspend fun setForeground(v: Int) = store.edit { it[Keys.FOREGROUND] = v }.let { }
    suspend fun setAutomaticForeground(v: Boolean) = store.edit { it[Keys.AUTO_FOREGROUND] = v }.let { }
    suspend fun setBackgroundOpacity(v: Int) = store.edit { it[Keys.BACKGROUND_OPACITY] = v.coerceIn(0, 100) }.let { }

    suspend fun resetColors() {
        store.edit {
            it.remove(Keys.COLOR_MODE)
            it.remove(Keys.BACKGROUND)
            it.remove(Keys.FOREGROUND)
            it.remove(Keys.AUTO_FOREGROUND)
            it.remove(Keys.BACKGROUND_OPACITY)
        }
    }
}
