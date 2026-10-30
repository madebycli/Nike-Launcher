// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * How far down the viewport a scrubbed letter may be parked.
 *
 * Nearly the whole screen. The top is the top, and the only reason to stop short of the
 * bottom is that past this the letter's own row is half off the screen and there is nothing
 * under it to read — which is a broken state rather than a taste anyone holds.
 */
val SECTION_TOP_RANGE = 0..90

/** Enough to be a list of what is new, short of being a second app list. */
val RECENT_SECTION_RANGE = 3..15

class AppListPrefs internal constructor(private val store: DataStore<Preferences>) {

    private val data get() = store.data

    val appListSearchEnabled: Flow<Boolean> =
        data.map { it[PrefKeys.APPLIST_SEARCH_ENABLED] ?: false }.distinctUntilChanged()

    /** Within thumb reach on a tall phone, rather than up by the status bar. */
    val appListSearchBottom: Flow<Boolean> =
        data.map { it[PrefKeys.APPLIST_SEARCH_BOTTOM] ?: false }.distinctUntilChanged()

    /**
     * Lets a search reach apps hidden from the list.
     *
     * Off by default: hidden means hidden, and someone who hid an app did not ask for it back.
     */
    val appListSearchHidden: Flow<Boolean> =
        data.map { it[PrefKeys.APPLIST_SEARCH_HIDDEN] ?: false }.distinctUntilChanged()

    val showListHeaders: Flow<Boolean> =
        data.map { it[PrefKeys.SHOW_LIST_HEADERS] ?: true }.distinctUntilChanged()

    val notificationBadges: Flow<Boolean> =
        data.map { it[PrefKeys.NOTIFICATION_BADGES] ?: false }.distinctUntilChanged()

    val notificationPreviews: Flow<Boolean> =
        data.map { it[PrefKeys.NOTIFICATION_PREVIEWS] ?: false }.distinctUntilChanged()

    val notificationTextSp: Flow<Int> =
        data.map { it[PrefKeys.NOTIFICATION_TEXT_SP] ?: 12 }.distinctUntilChanged()

    /** Blur is the compositor's, applied to the real wallpaper; an image is softened once and drawn. */
    val backgroundEffect: Flow<AppListBackgroundEffect> = data.map {
        runCatching { AppListBackgroundEffect.valueOf(it[PrefKeys.APP_LIST_BACKGROUND_EFFECT] ?: "NONE") }
            .getOrDefault(AppListBackgroundEffect.NONE)
            .let { effect -> if (effect == AppListBackgroundEffect.PIXEL) AppListBackgroundEffect.NONE else effect }
    }.distinctUntilChanged()

    val backgroundStrength: Flow<Int> =
        data.map { (it[PrefKeys.APP_LIST_BACKGROUND_STRENGTH] ?: 50).coerceIn(0, 100) }.distinctUntilChanged()

    /** Whether the picture shows only toward the edges, leaving the middle to the wallpaper. */
    val backgroundEdges: Flow<Boolean> =
        data.map { it[PrefKeys.APP_LIST_BACKGROUND_EDGES] ?: false }.distinctUntilChanged()

    val grain: Flow<Int> = data.map { (it[PrefKeys.APP_LIST_GRAIN] ?: 0).coerceIn(0, 100) }.distinctUntilChanged()

    val backgroundImage: Flow<String?> =
        data.map { it[PrefKeys.APP_LIST_BACKGROUND_IMAGE] }.distinctUntilChanged()

    /** The color the wallpaper is dimmed with while the list is open, or null to use the general dim color. */
    val tint: Flow<Int?> = data.map { it[PrefKeys.APP_LIST_TINT] }.distinctUntilChanged()

    suspend fun setTint(v: Int?) {
        store.edit { if (v == null) it.remove(PrefKeys.APP_LIST_TINT) else it[PrefKeys.APP_LIST_TINT] = v or 0xFF000000.toInt() }
    }

    suspend fun setBackgroundEffect(v: AppListBackgroundEffect) {
        store.edit { it[PrefKeys.APP_LIST_BACKGROUND_EFFECT] = v.name }
    }

    suspend fun setBackgroundStrength(v: Int) {
        store.edit { it[PrefKeys.APP_LIST_BACKGROUND_STRENGTH] = v.coerceIn(0, 100) }
    }

    suspend fun setBackgroundEdges(v: Boolean) {
        store.edit { it[PrefKeys.APP_LIST_BACKGROUND_EDGES] = v }
    }

    suspend fun setGrain(v: Int) {
        store.edit { it[PrefKeys.APP_LIST_GRAIN] = v.coerceIn(0, 100) }
    }

    suspend fun setBackgroundImage(v: String?) {
        store.edit { if (v == null) it.remove(PrefKeys.APP_LIST_BACKGROUND_IMAGE) else it[PrefKeys.APP_LIST_BACKGROUND_IMAGE] = v }
    }

    /** How far down the screen a scrubbed letter is parked, as a percentage of the viewport. */
    val sectionTopPercent: Flow<Int> =
        data.map { (it[PrefKeys.SECTION_TOP_PERCENT] ?: 26).coerceIn(SECTION_TOP_RANGE) }
            .distinctUntilChanged()

    /**
     * Whether opening the app list puts the cursor in the search box and the keyboard up.
     *
     * Off by default: the keyboard covers half of what was just opened, and most openings are
     * to look rather than to type.
     */
    val autoKeyboard: Flow<Boolean> =
        data.map { it[PrefKeys.AUTO_KEYBOARD] ?: false }.distinctUntilChanged()

    /**
     * Whether the letters at the end of the list reach the same line as the rest.
     *
     * Off by default because the room they need is real: a screen's worth of empty space under
     * the last app, which is there whenever the list is open rather than only when it is used.
     */
    val lastLetterToLine: Flow<Boolean> =
        data.map { it[PrefKeys.LAST_LETTER_TO_LINE] ?: true }.distinctUntilChanged()

    /**
     * Whether a search that matches no app offers to hand the words to the browser.
     *
     * Off by default, and it is a row to tap rather than something that happens: typing in the
     * launcher does not reach a search engine because you stopped typing.
     */
    val webSearchFallback: Flow<Boolean> =
        data.map { it[PrefKeys.WEB_SEARCH_FALLBACK] ?: false }.distinctUntilChanged()

    /** Whether swiping up puts the cursor in the search box rather than only opening the list. */
    val swipeUpOpensSearch: Flow<Boolean> =
        data.map { it[PrefKeys.SWIPE_UP_OPENS_SEARCH] ?: false }.distinctUntilChanged()

    /**
     * Whether the private space's apps get a section of their own below the alphabet.
     *
     * Off by default. An app installed in both profiles has the same name in both, and only
     * someone who keeps a private space has any idea which of the two they are looking at.
     */
    val privateSpaceSection: Flow<Boolean> =
        data.map { it[PrefKeys.PRIVATE_SPACE_SECTION] ?: false }.distinctUntilChanged()

    /** Whether folders also appear in the app list, filed under their own names (#126). */
    val foldersInAppList: Flow<Boolean> =
        data.map { it[PrefKeys.FOLDERS_IN_APP_LIST] ?: false }.distinctUntilChanged()

    /** Whether the list ends with what was installed most recently. */
    val recentSection: Flow<Boolean> =
        data.map { it[PrefKeys.RECENT_SECTION] ?: false }.distinctUntilChanged()

    /** How many of them. */
    val recentSectionCount: Flow<Int> =
        data.map { (it[PrefKeys.RECENT_SECTION_COUNT] ?: 5).coerceIn(RECENT_SECTION_RANGE) }
            .distinctUntilChanged()

    /**
     * Whether unlocking the private space leaves the app list open.
     *
     * Off by default, as it always behaved: the list closes and the home screen comes back.
     * On, the list stays where the space's apps are about to appear.
     */
    val stayAfterPrivateUnlock: Flow<Boolean> =
        data.map { it[PrefKeys.STAY_AFTER_PRIVATE_UNLOCK] ?: false }.distinctUntilChanged()

    suspend fun setAppListSearchEnabled(v: Boolean) {
        store.edit { it[PrefKeys.APPLIST_SEARCH_ENABLED] = v }
    }

    suspend fun setAppListSearchBottom(v: Boolean) {
        store.edit { it[PrefKeys.APPLIST_SEARCH_BOTTOM] = v }
    }

    suspend fun setAppListSearchHidden(v: Boolean) {
        store.edit { it[PrefKeys.APPLIST_SEARCH_HIDDEN] = v }
    }

    suspend fun setShowListHeaders(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_LIST_HEADERS] = v }
    }

    suspend fun setNotificationBadges(v: Boolean) {
        store.edit { it[PrefKeys.NOTIFICATION_BADGES] = v }
    }

    suspend fun setNotificationPreviews(v: Boolean) {
        store.edit { it[PrefKeys.NOTIFICATION_PREVIEWS] = v }
    }

    suspend fun setNotificationTextSp(v: Int) {
        store.edit { it[PrefKeys.NOTIFICATION_TEXT_SP] = v }
    }

    suspend fun setSectionTopPercent(v: Int) {
        store.edit { it[PrefKeys.SECTION_TOP_PERCENT] = v.coerceIn(SECTION_TOP_RANGE) }
    }

    suspend fun setLastLetterToLine(v: Boolean) {
        store.edit { it[PrefKeys.LAST_LETTER_TO_LINE] = v }
    }

    suspend fun setAutoKeyboard(v: Boolean) {
        store.edit { it[PrefKeys.AUTO_KEYBOARD] = v }
    }

    suspend fun setWebSearchFallback(v: Boolean) {
        store.edit { it[PrefKeys.WEB_SEARCH_FALLBACK] = v }
    }

    suspend fun setSwipeUpOpensSearch(v: Boolean) {
        store.edit { it[PrefKeys.SWIPE_UP_OPENS_SEARCH] = v }
    }

    suspend fun setPrivateSpaceSection(v: Boolean) {
        store.edit { it[PrefKeys.PRIVATE_SPACE_SECTION] = v }
    }

    suspend fun setFoldersInAppList(v: Boolean) {
        store.edit { it[PrefKeys.FOLDERS_IN_APP_LIST] = v }
    }

    suspend fun setRecentSection(v: Boolean) {
        store.edit { it[PrefKeys.RECENT_SECTION] = v }
    }

    suspend fun setRecentSectionCount(v: Int) {
        store.edit { it[PrefKeys.RECENT_SECTION_COUNT] = v.coerceIn(RECENT_SECTION_RANGE) }
    }

    suspend fun setStayAfterPrivateUnlock(v: Boolean) {
        store.edit { it[PrefKeys.STAY_AFTER_PRIVATE_UNLOCK] = v }
    }
}
