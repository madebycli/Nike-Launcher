// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.state

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import dev.nikelauncher.data.QuickLaunchRegion
import dev.nikelauncher.data.AppListBackgroundEffect
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.AzStripVisibility
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.Prefs
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.ScrubStyle
import dev.nikelauncher.data.ShortcutSwipe
import dev.nikelauncher.data.TextColorMode
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.theme.DEFAULT_SHADOW_STRENGTH
import dev.nikelauncher.ui.theme.rememberContentColor

/** Collects every setting the home destination reads into the one value it takes. */
@Composable
fun rememberHomeSettings(prefs: Prefs, library: LibraryState, appsByKey: Map<String, AppInfo>): HomeSettings {
    val a = prefs.appearance
    val h = prefs.home
    val l = prefs.appList
    val s = prefs.strip
    val iconSizeDp by a.iconSizeDp.collectAsState(56)
    val sidePaddingDp by a.sidePaddingDp.collectAsState(20)
    val textColorMode by a.textColorMode.collectAsState(TextColorMode.AUTO)
    val textColorCustom by a.textColorCustom.collectAsState(0xFFFFFFFF.toInt())
    val edgeSide by s.edgeSide.collectAsState(EdgeSide.RIGHT)
    val azVisibility by s.azStripVisibility.collectAsState(AzStripVisibility.NEVER)
    val edgeZoneWidthDp by s.edgeZoneWidthDp.collectAsState(56)
    val insetAuto by s.edgeZoneInsetAuto.collectAsState(true)
    val insetDp by s.edgeZoneInsetDp.collectAsState(0)
    val alignment by a.alignment.collectAsState(HomeAlignment.LEFT)
    val contentColor = rememberContentColor(textColorMode, textColorCustom)
    val stripAlwaysVisible = when (azVisibility) {
        AzStripVisibility.NEVER -> false
        AzStripVisibility.ALWAYS -> true
        AzStripVisibility.LANDSCAPE -> LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    return HomeSettings(
        iconSizeDp = iconSizeDp,
        labelSizeSp = a.labelSizeSp.collectAsState(16).value,
        itemSpacingDp = a.itemSpacingDp.collectAsState(10).value,
        sidePaddingDp = sidePaddingDp,
        nowPlayingHeightDp = h.nowPlayingHeightDp.collectAsState(64).value,
        nowPlayingEnabled = h.nowPlayingEnabled.collectAsState(false).value,
        edgeSide = edgeSide,
        scrubStyle = s.scrubStyle.collectAsState(ScrubStyle.BELL).value,
        scrubBubbleShape = s.scrubBubbleShape.collectAsState(ScrubBubbleShape.ROUNDED).value,
        rippleParams = s.rippleParams.collectAsState(RippleParams()).value,
        azStripVisibility = azVisibility,
        shortcutSwipe = h.shortcutSwipe.collectAsState(ShortcutSwipe.RIGHT).value,
        showAlphabet = s.showAlphabet.collectAsState(true).value,
        alignment = alignment,
        appListAlignment = a.appListAlignment.collectAsState(HomeAlignment.LEFT).value,
        iconSide = a.iconSide.collectAsState(IconSide.LEFT).value,
        widgetsStacked = h.widgetsStacked.collectAsState(false).value,
        widgetSidePaddingDp = h.widgetSidePaddingDp.collectAsState(sidePaddingDp).value,
        widgetOffsetXDp = h.widgetOffsetXDp.collectAsState(0).value,
        edgeZoneWidthDp = edgeZoneWidthDp,
        swipeUpOpensAppList = h.swipeUpOpensList.collectAsState(false).value,
        appListSearch = l.appListSearchEnabled.collectAsState(false).value,
        appListSearchBottom = l.appListSearchBottom.collectAsState(false).value,
        sectionTopPercent = l.sectionTopPercent.collectAsState(26).value,
        closeFolderOnLaunch = h.closeFolderOnLaunch.collectAsState(false).value,
        showListHeaders = l.showListHeaders.collectAsState(true).value,
        showSettingsRow = h.showSettingsRow.collectAsState(true).value,
        showFavoriteIcons = a.showFavoriteIcons.collectAsState(true).value,
        favoritesEverSet = library.favoritesEverSet,
        edgeZoneBandOnly = s.edgeZoneBandOnly.collectAsState(false).value,
        holdScroll = s.holdScroll.collectAsState(false).value,
        holdScrollSpeed = s.holdScrollSpeed.collectAsState(4).value,
        dimOthersScrubbing = s.dimOthersScrubbing.collectAsState(true).value,
        markWorkApps = a.markWorkApps.collectAsState(true).value,
        textShadow = a.textShadow.collectAsState(false).value,
        textShadowStrength = a.textShadowStrength.collectAsState(DEFAULT_SHADOW_STRENGTH).value,
        compactTapArea = a.compactTapArea.collectAsState(false).value,
        privateSpaceSection = l.privateSpaceSection.collectAsState(false).value,
        foldersInAppList = l.foldersInAppList.collectAsState(false).value,
        recentSection = l.recentSection.collectAsState(false).value,
        recentSectionCount = l.recentSectionCount.collectAsState(5).value,
        edgeZoneInsetDp = insetDp,
        edgeZoneInsetAuto = insetAuto,
        stayAfterPrivateUnlock = l.stayAfterPrivateUnlock.collectAsState(false).value,
        webSearchFallback = l.webSearchFallback.collectAsState(false).value,
        swipeUpOpensSearch = l.swipeUpOpensSearch.collectAsState(false).value,
        favoritesSource = library.favoritesSource,
        appListSearchHidden = l.appListSearchHidden.collectAsState(false).value,
        hideStatusBarAppList = a.hideStatusBarAppList.collectAsState(false).value,
        sortByUsage = prefs.library.sortByUsage.collectAsState(false).value,
        quickLaunchLeft = library.quickLaunchLeftKey?.let { appsByKey[it] },
        quickLaunchRegion = h.quickLaunchRegion.collectAsState(QuickLaunchRegion()).value,
        quickLaunchRight = library.quickLaunchRightKey?.let { appsByKey[it] },
        dimWallpaperAlpha = a.dimWallpaperAlpha.collectAsState(0.35f).value,
        dimHomeAlpha = a.dimHomeAlpha.collectAsState(0f).value,
        appListBackgroundEffect = l.backgroundEffect.collectAsState(AppListBackgroundEffect.NONE).value,
        appListBackgroundStrength = l.backgroundStrength.collectAsState(50).value,
        appListBackgroundImage = l.backgroundImage.collectAsState(null).value,
        appListBackgroundEdges = l.backgroundEdges.collectAsState(false).value,
        appListGrain = l.grain.collectAsState(0).value,
        dimColor = a.dimColor.collectAsState(0xFF000000.toInt()).value,
        appListTint = l.tint.collectAsState(null).value ?: a.dimColor.collectAsState(0xFF000000.toInt()).value,
        hapticsEnabled = s.hapticsEnabled.collectAsState(true).value,
        showFavoriteLabels = a.showFavoriteLabels.collectAsState(true).value,
        doubleTapToLock = h.doubleTapToLock.collectAsState(false).value,
        contentColor = contentColor,
        autoKeyboard = l.autoKeyboard.collectAsState(false).value,
        lastLetterToLine = l.lastLetterToLine.collectAsState(false).value,
        stripInsetSide = if (stripAlwaysVisible) edgeSide else null,
        edgeZoneInsetLiveDp = liveInsetDp(insetAuto, insetDp),
        blockGapDp = prefs.homeLayout.blockGapDp.collectAsState(12).value,
        mediaSidePaddingDp = prefs.homeLayout.mediaSidePaddingDp.collectAsState(sidePaddingDp).value,
    )
}

/**
 * Extra reach of the edge zone beyond its width, in dp. The gesture inset the system reports is only
 * known inside the route, so this is the number the user set; automatic reads as zero here.
 */
private fun liveInsetDp(auto: Boolean, set: Int): Int = if (auto) 0 else set
