// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.nikelauncher.ui.theme.DEFAULT_SHADOW_STRENGTH
import dev.nikelauncher.ui.theme.SHADOW_STRENGTH_RANGE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Which edge favorites and app rows line up against. */
enum class HomeAlignment { LEFT, CENTER, RIGHT }

/** Which side of an app's name its icon sits on. */
enum class IconSide { LEFT, RIGHT }

enum class AppFont { GOOGLE_SANS_FLEX, SYSTEM, NIAGARA, SANS_SERIF, SERIF, MONOSPACE, CUSTOM }

/** AUTO picks light or dark text from the wallpaper's own colors. */
enum class TextColorMode { AUTO, LIGHT, DARK, MATERIAL, CUSTOM }

/**
 * The mask an icon is drawn through.
 *
 * SYSTEM leaves the drawable alone, which means whatever shape the device already applies.
 * The rest re-mask an adaptive icon's own layers; an icon that is not adaptive has no safe
 * zone to cut into, so it is left as it is whatever this says.
 */
enum class IconShape { SYSTEM, CIRCLE, ROUNDED, SQUARE }

class AppearancePrefs internal constructor(private val store: DataStore<Preferences>) {

    private val data get() = store.data

    val iconSizeDp: Flow<Int> = data.map { it[PrefKeys.ICON_SIZE_DP] ?: 56 }.distinctUntilChanged()

    val labelSizeSp: Flow<Int> = data.map { it[PrefKeys.LABEL_SIZE_SP] ?: 16 }.distinctUntilChanged()

    /** Vertical gap between favorite rows. */
    val itemSpacingDp: Flow<Int> = data.map { it[PrefKeys.ITEM_SPACING_DP] ?: 10 }.distinctUntilChanged()

    /** Left/right inset applied to every element on the home screen, so they stay in line. */
    val sidePaddingDp: Flow<Int> = data.map { it[PrefKeys.SIDE_PADDING_DP] ?: 20 }.distinctUntilChanged()

    /** Absolute path of the typeface the user supplied, once it has been copied in. */
    val fontFile: Flow<String?> = data.map { it[PrefKeys.FONT_FILE] }.distinctUntilChanged()

    /**
     * What the wallpaper is dimmed with, as an opaque RGB; the dim sliders set how much of it
     * lands. Black until the user picks otherwise, which is what it always was.
     */
    val dimColor: Flow<Int> =
        data.map { it[PrefKeys.DIM_COLOR] ?: 0xFF000000.toInt() }.distinctUntilChanged()

    /** Used when the text color mode is CUSTOM. Opaque white until the user picks something. */
    val textColorCustom: Flow<Int> =
        data.map { it[PrefKeys.TEXT_COLOR_CUSTOM] ?: 0xFFFFFFFF.toInt() }.distinctUntilChanged()

    /**
     * Draws the monochrome layer Android 13 added to adaptive icons, tinted to the system
     * palette, instead of the app's own colors. Apps that ship no such layer keep their
     * ordinary icon — there is nothing to derive one from.
     */
    val themedIcons: Flow<Boolean> = data.map { it[PrefKeys.THEMED_ICONS] ?: false }.distinctUntilChanged()

    val iconShape: Flow<IconShape> = data.map { pref ->
        pref[PrefKeys.ICON_SHAPE]?.let { runCatching { IconShape.valueOf(it) }.getOrNull() } ?: IconShape.SYSTEM
    }.distinctUntilChanged()

    val font: Flow<AppFont> = data.map {
        runCatching { AppFont.valueOf(it[PrefKeys.FONT] ?: AppFont.GOOGLE_SANS_FLEX.name) }.getOrDefault(AppFont.SYSTEM)
    }.distinctUntilChanged()

    val hideStatusBar: Flow<Boolean> = data.map { it[PrefKeys.HIDE_STATUS_BAR] ?: false }.distinctUntilChanged()

    /**
     * The same choice for the app list, which used to follow the home screen's.
     *
     * Falls back to it until set, so nobody's status bar appears where it did not before.
     */
    val hideStatusBarAppList: Flow<Boolean> =
        data.map { it[PrefKeys.HIDE_STATUS_BAR_APPLIST] ?: it[PrefKeys.HIDE_STATUS_BAR] ?: false }
            .distinctUntilChanged()

    val dimWallpaperAlpha: Flow<Float> = data.map { it[PrefKeys.DIM_WALLPAPER_ALPHA] ?: 0.35f }.distinctUntilChanged()

    val dimHomeAlpha: Flow<Float> = data.map { it[PrefKeys.DIM_HOME_ALPHA] ?: 0f }.distinctUntilChanged()

    val showFavoriteLabels: Flow<Boolean> =
        data.map { it[PrefKeys.SHOW_FAVORITE_LABELS] ?: true }.distinctUntilChanged()

    val textColorMode: Flow<TextColorMode> = data.map {
        runCatching { TextColorMode.valueOf(it[PrefKeys.TEXT_COLOR_MODE] ?: TextColorMode.AUTO.name) }
            .getOrDefault(TextColorMode.AUTO)
    }.distinctUntilChanged()

    /** Lay icons and labels out from the right edge instead of the left. */
    val alignRight: Flow<Boolean> = data.map { it[PrefKeys.ALIGN_RIGHT] ?: false }.distinctUntilChanged()

    val iconPackPackage: Flow<String?> = data.map { it[PrefKeys.ICON_PACK_PACKAGE] }.distinctUntilChanged()

    /** Drawing icons at all; off leaves text-only rows everywhere. */
    val showAppIcons: Flow<Boolean> = data.map { it[PrefKeys.SHOW_APP_ICONS] ?: true }.distinctUntilChanged()

    /**
     * Falls back to the one switch these two were split out of, so anyone who had turned icons
     * off keeps them off in both places rather than having half of them come back.
     */
    val showFavoriteIcons: Flow<Boolean> =
        data.map { it[PrefKeys.SHOW_FAVORITE_ICONS] ?: it[PrefKeys.SHOW_APP_ICONS] ?: true }
            .distinctUntilChanged()

    /**
     * Whether a favorite answers a tap only on its icon and name.
     *
     * Off by default, which is how it always behaved: the whole line is the button. On, the
     * rest of the line is home screen again, for anyone opening apps they never meant to by
     * tapping beside a name.
     */
    val compactTapArea: Flow<Boolean> =
        data.map { it[PrefKeys.COMPACT_TAP_AREA] ?: false }.distinctUntilChanged()

    /**
     * Whether launcher text carries a shadow, for reading it over a busy wallpaper.
     *
     * Only ever behind light text. A shadow under dark text on a light wallpaper muddies it
     * rather than separating it, so the setting asks for contrast and this decides where
     * contrast is actually gained.
     */
    val textShadow: Flow<Boolean> =
        data.map { it[PrefKeys.TEXT_SHADOW] ?: false }.distinctUntilChanged()

    /** How strong that shadow is; the default is the look it shipped with. */
    val textShadowStrength: Flow<Int> =
        data.map { (it[PrefKeys.TEXT_SHADOW_STRENGTH] ?: DEFAULT_SHADOW_STRENGTH).coerceIn(SHADOW_STRENGTH_RANGE) }
            .distinctUntilChanged()

    /**
     * Whether a work profile's apps say so after their name.
     *
     * On, since anyone with a work profile has two of several apps and no way to tell them
     * apart in a list of names. Nothing to see for anyone who has no second profile.
     */
    val markWorkApps: Flow<Boolean> =
        data.map { it[PrefKeys.MARK_WORK_APPS] ?: true }.distinctUntilChanged()

    /**
     * Reads [key], falling back to the shared alignment and then to the old right-handed
     * boolean, so an upgrade keeps whichever side the user had chosen and splitting the two
     * lists apart starts them both where they already were.
     */
    private fun readAlignment(pref: Preferences, key: Preferences.Key<String>): HomeAlignment {
        val stored = pref[key] ?: pref[PrefKeys.ALIGNMENT]
        return when {
            stored != null -> runCatching { HomeAlignment.valueOf(stored) }.getOrDefault(HomeAlignment.LEFT)
            pref[PrefKeys.ALIGN_RIGHT] == true -> HomeAlignment.RIGHT
            else -> HomeAlignment.LEFT
        }
    }

    /** How the favorites on the home screen line up. */
    val alignment: Flow<HomeAlignment> =
        data.map { readAlignment(it, PrefKeys.ALIGNMENT) }.distinctUntilChanged()

    /** How the A-Z list lines up, which people want to differ from the home screen. */
    val appListAlignment: Flow<HomeAlignment> =
        data.map { readAlignment(it, PrefKeys.APPLIST_ALIGNMENT) }.distinctUntilChanged()

    val iconSide: Flow<IconSide> = data.map {
        runCatching { IconSide.valueOf(it[PrefKeys.ICON_SIDE] ?: IconSide.LEFT.name) }
            .getOrDefault(IconSide.LEFT)
    }.distinctUntilChanged()

    /** How long a pull-down keeps the status bar on screen before it fades away again. */
    val statusBarPeekSeconds: Flow<Int> =
        data.map { it[PrefKeys.STATUS_BAR_PEEK_SECONDS] ?: 5 }.distinctUntilChanged()

    suspend fun setIconSizeDp(v: Int) {
        store.edit { it[PrefKeys.ICON_SIZE_DP] = v }
    }

    suspend fun setLabelSizeSp(v: Int) {
        store.edit { it[PrefKeys.LABEL_SIZE_SP] = v }
    }

    /** The rows' distance from the screen edge, changed by [deltaDp] from what is stored. */
    suspend fun nudgeSidePaddingDp(deltaDp: Int, max: Int) {
        store.edit { it[PrefKeys.SIDE_PADDING_DP] = ((it[PrefKeys.SIDE_PADDING_DP] ?: 20) + deltaDp).coerceIn(0, max) }
    }

    suspend fun setSidePaddingDp(v: Int) {
        store.edit { it[PrefKeys.SIDE_PADDING_DP] = v }
    }

    suspend fun setItemSpacingDp(v: Int) {
        store.edit { it[PrefKeys.ITEM_SPACING_DP] = v }
    }

    suspend fun setFontFile(path: String?) {
        store.edit { pref ->
            if (path == null) pref.remove(PrefKeys.FONT_FILE) else pref[PrefKeys.FONT_FILE] = path
        }
    }

    suspend fun setDimColor(argb: Int) {
        store.edit { it[PrefKeys.DIM_COLOR] = argb }
    }

    suspend fun setTextColorCustom(argb: Int) {
        store.edit { it[PrefKeys.TEXT_COLOR_CUSTOM] = argb }
    }

    suspend fun setThemedIcons(v: Boolean) {
        store.edit { it[PrefKeys.THEMED_ICONS] = v }
    }

    suspend fun setIconShape(v: IconShape) {
        store.edit { it[PrefKeys.ICON_SHAPE] = v.name }
    }

    suspend fun setFont(f: AppFont) {
        store.edit { it[PrefKeys.FONT] = f.name }
    }

    suspend fun setHideStatusBarAppList(v: Boolean) {
        store.edit { it[PrefKeys.HIDE_STATUS_BAR_APPLIST] = v }
    }

    suspend fun setHideStatusBar(v: Boolean) {
        store.edit { it[PrefKeys.HIDE_STATUS_BAR] = v }
    }

    suspend fun setDimWallpaperAlpha(v: Float) {
        store.edit { it[PrefKeys.DIM_WALLPAPER_ALPHA] = v }
    }

    suspend fun setDimHomeAlpha(v: Float) {
        store.edit { it[PrefKeys.DIM_HOME_ALPHA] = v }
    }

    suspend fun setShowFavoriteLabels(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_FAVORITE_LABELS] = v }
    }

    suspend fun setTextColorMode(v: TextColorMode) {
        store.edit { it[PrefKeys.TEXT_COLOR_MODE] = v.name }
    }

    suspend fun setAlignRight(v: Boolean) {
        store.edit { it[PrefKeys.ALIGN_RIGHT] = v }
    }

    suspend fun setIconPackPackage(pkg: String?) {
        store.edit { pref ->
            if (pkg.isNullOrBlank()) pref.remove(PrefKeys.ICON_PACK_PACKAGE) else pref[PrefKeys.ICON_PACK_PACKAGE] = pkg
        }
    }

    suspend fun setShowAppIcons(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_APP_ICONS] = v }
    }

    suspend fun setShowFavoriteIcons(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_FAVORITE_ICONS] = v }
    }

    suspend fun setCompactTapArea(v: Boolean) {
        store.edit { it[PrefKeys.COMPACT_TAP_AREA] = v }
    }

    suspend fun setTextShadowStrength(v: Int) {
        store.edit { it[PrefKeys.TEXT_SHADOW_STRENGTH] = v.coerceIn(SHADOW_STRENGTH_RANGE) }
    }

    suspend fun setTextShadow(v: Boolean) {
        store.edit { it[PrefKeys.TEXT_SHADOW] = v }
    }

    suspend fun setMarkWorkApps(v: Boolean) {
        store.edit { it[PrefKeys.MARK_WORK_APPS] = v }
    }

    suspend fun setAlignment(v: HomeAlignment) {
        store.edit {
            it[PrefKeys.ALIGNMENT] = v.name
            // Kept in step so a downgrade still lands on the side the user picked.
            it[PrefKeys.ALIGN_RIGHT] = v == HomeAlignment.RIGHT
        }
    }

    suspend fun setAppListAlignment(v: HomeAlignment) {
        store.edit { it[PrefKeys.APPLIST_ALIGNMENT] = v.name }
    }

    suspend fun setIconSide(v: IconSide) {
        store.edit { it[PrefKeys.ICON_SIDE] = v.name }
    }

    suspend fun setStatusBarPeekSeconds(v: Int) {
        store.edit { it[PrefKeys.STATUS_BAR_PEEK_SECONDS] = v }
    }
}
