// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The corner button: what it does, how it looks. Which app it opens is kept with the library. */
class CornerPrefs internal constructor(private val store: DataStore<Preferences>) {

    private object Keys {
        val POSITION_X = floatPreferencesKey("corner_position_x")
        val POSITION_Y = floatPreferencesKey("corner_position_y")
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
        val ICON = stringPreferencesKey("corner_icon")
        val BUTTON_KEY = stringPreferencesKey("corner_button_key")
        val SEARCH_FLOATING = booleanPreferencesKey("search_button_floating")
        val SEARCH_HIDE_WHILE_SCROLLING = booleanPreferencesKey("search_button_hide_while_scrolling")
        val SEARCH_COLOR_MODE = stringPreferencesKey("search_button_color_mode")
        val SEARCH_BACKGROUND = intPreferencesKey("search_button_background")
        val SEARCH_FOREGROUND = intPreferencesKey("search_button_foreground")
        val SEARCH_AUTO_FOREGROUND = booleanPreferencesKey("search_button_auto_foreground")
        val SEARCH_BACKGROUND_OPACITY = intPreferencesKey("search_button_background_opacity")
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> runCatching { enumValueOf<E>(name) }.getOrNull() } ?: default

    val settings: Flow<CornerSettings> = store.data.map { pref ->
        CornerSettings(
            enabled = pref[Keys.ENABLED] ?: true,
            position = cornerPosition(pref[Keys.POSITION_X], pref[Keys.POSITION_Y]),
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
                icon = pref[Keys.ICON].toEnum(CornerIcon.ACTION),
            ),
        )
    }.distinctUntilChanged()

    suspend fun setPosition(position: CornerPosition?) {
        store.edit {
            val value = position?.let { p -> cornerPosition(p.x, p.y) }
            if (value == null) { it.remove(Keys.POSITION_X); it.remove(Keys.POSITION_Y) }
            else { it[Keys.POSITION_X] = value.x; it[Keys.POSITION_Y] = value.y }
        }
    }

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

    suspend fun setIcon(v: CornerIcon) = store.edit { it[Keys.ICON] = v.name }.let { }

    suspend fun resetColors() {
        store.edit {
            it.remove(Keys.COLOR_MODE)
            it.remove(Keys.BACKGROUND)
            it.remove(Keys.FOREGROUND)
            it.remove(Keys.AUTO_FOREGROUND)
            it.remove(Keys.BACKGROUND_OPACITY)
        }
    }

    /** A search button floating in the app list's corner, for lists whose own search field is off. */
    val floatingSearch: Flow<Boolean> = store.data.map { it[Keys.SEARCH_FLOATING] ?: true }.distinctUntilChanged()

    /** Whether it steps aside while the list is being scrolled or scrubbed. */
    val hideSearchWhileScrolling: Flow<Boolean> =
        store.data.map { it[Keys.SEARCH_HIDE_WHILE_SCROLLING] ?: true }.distinctUntilChanged()

    val searchAppearance: Flow<CornerAppearance> = store.data.map { pref ->
        val defaults = CornerAppearance()
        CornerAppearance(
            colorMode = pref[Keys.SEARCH_COLOR_MODE].toEnum(CornerColorMode.CLASSIC),
            background = pref[Keys.SEARCH_BACKGROUND] ?: defaults.background,
            foreground = pref[Keys.SEARCH_FOREGROUND] ?: defaults.foreground,
            automaticForeground = pref[Keys.SEARCH_AUTO_FOREGROUND] ?: true,
            backgroundOpacity = (pref[Keys.SEARCH_BACKGROUND_OPACITY] ?: 100).coerceIn(0, 100),
        )
    }.distinctUntilChanged()

    suspend fun setFloatingSearch(v: Boolean) = store.edit { it[Keys.SEARCH_FLOATING] = v }.let { }
    suspend fun setHideSearchWhileScrolling(v: Boolean) = store.edit { it[Keys.SEARCH_HIDE_WHILE_SCROLLING] = v }.let { }
    suspend fun setSearchColorMode(v: CornerColorMode) = store.edit { it[Keys.SEARCH_COLOR_MODE] = v.name }.let { }
    suspend fun setSearchBackground(v: Int) = store.edit { it[Keys.SEARCH_BACKGROUND] = v or 0xFF000000.toInt() }.let { }
    suspend fun setSearchForeground(v: Int) = store.edit { it[Keys.SEARCH_FOREGROUND] = v or 0xFF000000.toInt() }.let { }
    suspend fun setSearchAutomaticForeground(v: Boolean) = store.edit { it[Keys.SEARCH_AUTO_FOREGROUND] = v }.let { }
    suspend fun setSearchBackgroundOpacity(v: Int) = store.edit { it[Keys.SEARCH_BACKGROUND_OPACITY] = v.coerceIn(0, 100) }.let { }
}
