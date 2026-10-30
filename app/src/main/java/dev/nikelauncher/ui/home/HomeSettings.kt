// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.nikelauncher.data.QuickLaunchRegion
import dev.nikelauncher.data.AppListBackgroundEffect
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.AzStripVisibility
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.ScrubStyle
import dev.nikelauncher.data.ShortcutSwipe

/** Display and behavior settings the home destination reads, grouped so they travel as one. */
@Immutable
data class HomeSettings(
    val iconSizeDp: Int,
    val labelSizeSp: Int,
    val itemSpacingDp: Int,
    val sidePaddingDp: Int,
    val nowPlayingHeightDp: Int,
    val nowPlayingEnabled: Boolean,
    val edgeSide: EdgeSide,
    val scrubStyle: ScrubStyle,
    val scrubBubbleShape: ScrubBubbleShape,
    val rippleParams: RippleParams,
    val azStripVisibility: AzStripVisibility,
    val shortcutSwipe: ShortcutSwipe,
    val showAlphabet: Boolean,
    val alignment: HomeAlignment,
    val appListAlignment: HomeAlignment,
    val iconSide: IconSide,
    val widgetsStacked: Boolean,
    val widgetSidePaddingDp: Int,
    val widgetOffsetXDp: Int,
    val edgeZoneWidthDp: Int,
    val swipeUpOpensAppList: Boolean,
    val appListSearch: Boolean,
    val appListSearchBottom: Boolean,
    val sectionTopPercent: Int,
    val closeFolderOnLaunch: Boolean,
    val showListHeaders: Boolean,
    val showSettingsRow: Boolean,
    val showFavoriteIcons: Boolean,
    val favoritesEverSet: Boolean,
    val edgeZoneBandOnly: Boolean,
    val holdScroll: Boolean,
    val holdScrollSpeed: Int,
    val dimOthersScrubbing: Boolean,
    val markWorkApps: Boolean,
    val textShadow: Boolean,
    val textShadowStrength: Int,
    val compactTapArea: Boolean,
    val privateSpaceSection: Boolean,
    val foldersInAppList: Boolean,
    val recentSection: Boolean,
    val recentSectionCount: Int,
    val edgeZoneInsetDp: Int,
    val edgeZoneInsetAuto: Boolean,
    val stayAfterPrivateUnlock: Boolean,
    val webSearchFallback: Boolean,
    val swipeUpOpensSearch: Boolean,
    val favoritesSource: FavoritesSource,
    val appListSearchHidden: Boolean,
    val hideStatusBarAppList: Boolean,
    val sortByUsage: Boolean,
    val quickLaunchLeft: AppInfo?,
    val quickLaunchRight: AppInfo?,
    val quickLaunchRegion: QuickLaunchRegion,
    val dimWallpaperAlpha: Float,
    val dimHomeAlpha: Float,
    val appListBackgroundEffect: AppListBackgroundEffect,
    val appListBackgroundStrength: Int,
    val appListBackgroundImage: String?,
    val appListBackgroundEdges: Boolean,
    val appListGrain: Int,
    val dimColor: Int,
    val hapticsEnabled: Boolean,
    val showFavoriteLabels: Boolean,
    val doubleTapToLock: Boolean,
    val contentColor: Color,
    /** Whether opening the app list puts the cursor in its search box. */
    val autoKeyboard: Boolean,
    /** Whether the last letters reach the same line as the first. */
    val lastLetterToLine: Boolean,
    /** The side an always-present A-Z strip occupies, so content can keep out from under it. */
    val stripInsetSide: EdgeSide?,
    /** How far the live edge zones reach in, beyond their own width. */
    val edgeZoneInsetLiveDp: Int,
) {
    val quickLaunchEnabled: Boolean get() = quickLaunchLeft != null || quickLaunchRight != null

    /** False when the favorites are computed from usage, so there is no order to drag. */
    val favoritesReorderable: Boolean get() = favoritesSource == FavoritesSource.MANUAL

    /** Only while it would still be telling someone something they do not know. */
    val showEmptyHint: Boolean get() = !favoritesEverSet && favoritesSource == FavoritesSource.MANUAL
}
