// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Which way an app row is swiped to reach the shortcuts it publishes.
 *
 * A direction and not a switch because only one of the two does anything, and which one should
 * be the useful one is a question about the hand holding the phone.
 */
enum class ShortcutSwipe { OFF, RIGHT, LEFT }

class HomePrefs internal constructor(private val store: DataStore<Preferences>) {

    private val data get() = store.data

    /**
     * Absent means this install predates multiple widgets, so the lone [PrefKeys.WIDGET_ID] is
     * promoted. An empty string is different: it means every widget was removed, and must not
     * resurrect that old key.
     */
    private fun readWidgetIds(pref: Preferences): List<Int> {
        val raw = pref[PrefKeys.WIDGET_IDS]
            ?: return listOfNotNull(pref[PrefKeys.WIDGET_ID]?.takeIf { it > 0 })
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
    }

    private fun MutablePreferences.writeWidgetIds(ids: List<Int>) {
        this[PrefKeys.WIDGET_IDS] = ids.joinToString(",")
        // Mirrored so a downgrade to a single-widget build still finds one.
        this[PrefKeys.WIDGET_ID] = ids.firstOrNull() ?: -1
    }

    val nowPlayingHeightDp: Flow<Int> = data.map { it[PrefKeys.NOW_PLAYING_HEIGHT_DP] ?: 64 }.distinctUntilChanged()

    /** Where each home block is held; a block never moved holds its default place. */
    val homePins: Flow<HomePins> = data.map { pref ->
        HomePins.Default.run {
            copy(
                widget = pref[PrefKeys.HOME_PIN_WIDGET]?.let(BlockPin::decode) ?: widget,
                nowPlaying = pref[PrefKeys.HOME_PIN_NOW_PLAYING]?.let(BlockPin::decode) ?: nowPlaying,
                favorites = pref[PrefKeys.HOME_PIN_FAVORITES]?.let(BlockPin::decode) ?: favorites,
            )
        }
    }.distinctUntilChanged()

    suspend fun setHomePin(block: HomeBlock, pin: BlockPin?) {
        val key = when (block) {
            HomeBlock.WIDGET -> PrefKeys.HOME_PIN_WIDGET
            HomeBlock.NOW_PLAYING -> PrefKeys.HOME_PIN_NOW_PLAYING
            HomeBlock.FAVORITES -> PrefKeys.HOME_PIN_FAVORITES
        }
        store.edit { if (pin == null) it.remove(key) else it[key] = pin.encode() }
    }

    /**
     * Whether swiping a row sideways offers the shortcuts its app publishes.
     *
     * On by default, since it is the quick way to reach them and the only one — they are not
     * in the long-press menu, which is for setting a row up rather than using it. Off is for
     * anyone who would rather their rows answered only to a tap.
     */
    /**
     * Falls back to the switch this replaced, so anyone who had turned it off stays off and
     * everyone else keeps the direction it always had.
     */
    val shortcutSwipe: Flow<ShortcutSwipe> =
        data.map { prefs ->
            prefs[PrefKeys.SHORTCUT_SWIPE]
                ?.let { name -> runCatching { ShortcutSwipe.valueOf(name) }.getOrNull() }
                ?: if (prefs[PrefKeys.SWIPE_FOR_SHORTCUTS] == false) ShortcutSwipe.OFF else ShortcutSwipe.RIGHT
        }.distinctUntilChanged()

    /**
     * Whether the launcher turns with the device.
     *
     * Null until it is set, which leaves the decision to the screen: a tablet has the height
     * for this sideways and a phone does not. Someone who wants it anyway — a phone in a car
     * mount is the case that came up — can say so.
     */
    val allowRotation: Flow<Boolean?> = data.map { it[PrefKeys.ALLOW_ROTATION] }.distinctUntilChanged()

    val doubleTapToLock: Flow<Boolean> =
        data.map { it[PrefKeys.DOUBLE_TAP_TO_LOCK] ?: false }.distinctUntilChanged()

    val nowPlayingEnabled: Flow<Boolean> = data.map { it[PrefKeys.NOW_PLAYING_ENABLED] ?: false }.distinctUntilChanged()

    val widgetId: Flow<Int> = data.map { it[PrefKeys.WIDGET_ID] ?: -1 }.distinctUntilChanged()

    val widgetHeightDp: Flow<Int> = data.map { it[PrefKeys.WIDGET_HEIGHT_DP] ?: 180 }.distinctUntilChanged()

    /** Home widgets one under another instead of swiped between (#128). */
    val widgetsStacked: Flow<Boolean> = data.map { it[PrefKeys.WIDGETS_STACKED] ?: false }.distinctUntilChanged()

    suspend fun setWidgetsStacked(v: Boolean) {
        store.edit { it[PrefKeys.WIDGETS_STACKED] = v }
    }

    val widgetIds: Flow<List<Int>> = data.map { readWidgetIds(it) }.distinctUntilChanged()

    /**
     * The widget's own left/right inset. Absent means it has never been set apart, so it
     * follows the favorites and nothing moves the first time this key appears.
     */
    val widgetSidePaddingDp: Flow<Int> =
        data.map { it[PrefKeys.WIDGET_SIDE_PADDING_DP] ?: it[PrefKeys.SIDE_PADDING_DP] ?: 20 }.distinctUntilChanged()

    /**
     * How far the widget is shifted sideways, negative left and positive right.
     *
     * A third-party widget lays out its own contents and most clocks center theirs, which
     * nothing out here can reach inside. Moving the whole widget is what moves them. Side
     * padding already sets how wide it is; this is only where that width sits.
     */
    val widgetOffsetXDp: Flow<Int> =
        data.map { (it[PrefKeys.WIDGET_OFFSET_X_DP] ?: 0).coerceIn(-200, 200) }.distinctUntilChanged()

    /** Opening the app list by swiping up from the home screen. */
    val swipeUpOpensList: Flow<Boolean> =
        data.map { it[PrefKeys.SWIPE_UP_OPENS_LIST] ?: false }.distinctUntilChanged()

    val showSettingsRow: Flow<Boolean> =
        data.map { it[PrefKeys.SHOW_SETTINGS_ROW] ?: true }.distinctUntilChanged()

    val closeFolderOnLaunch: Flow<Boolean> =
        data.map { it[PrefKeys.CLOSE_FOLDER_ON_LAUNCH] ?: false }.distinctUntilChanged()

    /** 0 = installed before this scheme, 1 = a genuine first run. Absent until [ensureInstallMarker]. */
    val layoutDefaultsVersion: Flow<Int?> =
        data.map { it[PrefKeys.LAYOUT_DEFAULTS_VERSION] }.distinctUntilChanged()

    val welcomeSeen: Flow<Boolean> = data.map { it[PrefKeys.WELCOME_SEEN] ?: false }.distinctUntilChanged()

    suspend fun setNowPlayingHeightDp(v: Int) {
        store.edit { it[PrefKeys.NOW_PLAYING_HEIGHT_DP] = v }
    }

    suspend fun setShortcutSwipe(v: ShortcutSwipe) {
        store.edit {
            it[PrefKeys.SHORTCUT_SWIPE] = v.name
            // Mirrored, so a downgrade to the version that only had a switch reads it right.
            it[PrefKeys.SWIPE_FOR_SHORTCUTS] = v != ShortcutSwipe.OFF
        }
    }

    suspend fun setAllowRotation(v: Boolean) {
        store.edit { it[PrefKeys.ALLOW_ROTATION] = v }
    }

    suspend fun setDoubleTapToLock(v: Boolean) {
        store.edit { it[PrefKeys.DOUBLE_TAP_TO_LOCK] = v }
    }

    suspend fun setWidgetId(id: Int) {
        store.edit { it[PrefKeys.WIDGET_ID] = id }
    }

    suspend fun setNowPlayingEnabled(v: Boolean) {
        store.edit { it[PrefKeys.NOW_PLAYING_ENABLED] = v }
    }

    suspend fun setWidgetHeightDp(v: Int) {
        store.edit { it[PrefKeys.WIDGET_HEIGHT_DP] = v }
    }

    suspend fun addWidgetId(id: Int) {
        store.edit { pref ->
            val current = readWidgetIds(pref)
            if (id !in current) pref.writeWidgetIds(current + id)
        }
    }

    suspend fun removeWidgetId(id: Int) {
        store.edit { pref -> pref.writeWidgetIds(readWidgetIds(pref) - id) }
    }

    suspend fun setWidgetSidePaddingDp(v: Int) {
        store.edit { it[PrefKeys.WIDGET_SIDE_PADDING_DP] = v }
    }

    suspend fun setWidgetOffsetXDp(v: Int) {
        store.edit { it[PrefKeys.WIDGET_OFFSET_X_DP] = v.coerceIn(-200, 200) }
    }

    suspend fun setSwipeUpOpensList(v: Boolean) {
        store.edit { it[PrefKeys.SWIPE_UP_OPENS_LIST] = v }
    }

    /**
     * Settings a fresh install should start with, which an existing one must not be given.
     *
     * Some defaults are only right for someone who has not used this before. Confining the live
     * edge to the letters is the better first experience; changing it under someone who has
     * been swiping the whole side for months is not an improvement, it is their launcher
     * behaving differently one morning. So it is written once, when the welcome is dismissed —
     * a screen only a new install ever sees — and only where nothing has been chosen already.
     */
    suspend fun applyNewInstallDefaults() {
        store.edit {
            if (it[PrefKeys.EDGE_ZONE_BAND_ONLY] == null) it[PrefKeys.EDGE_ZONE_BAND_ONLY] = true
        }
    }

    suspend fun setShowSettingsRow(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_SETTINGS_ROW] = v }
    }

    suspend fun setCloseFolderOnLaunch(v: Boolean) {
        store.edit { it[PrefKeys.CLOSE_FOLDER_ON_LAUNCH] = v }
    }

    suspend fun setWelcomeSeen(v: Boolean) {
        store.edit { it[PrefKeys.WELCOME_SEEN] = v }
    }

    /**
     * Stamps whether this install is new, once. Nothing writes to the store before the user
     * changes something, so an empty store is the one reliable signal of a first run — which
     * is why this has to run before any other setter can muddy it.
     */
    suspend fun ensureInstallMarker() {
        store.edit { pref ->
            if (pref[PrefKeys.LAYOUT_DEFAULTS_VERSION] == null) {
                pref[PrefKeys.LAYOUT_DEFAULTS_VERSION] = if (pref.asMap().isEmpty()) 1 else 0
            }
        }
    }
}
