// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.ScrubStyle
import dev.nikelauncher.data.ShortcutSwipe

@Composable
fun AppListScreen(
    model: AppListModel,
    nameOverrides: Map<String, String>,
    scrub: ScrubState,
    iconSizeDp: Int,
    labelSizeSp: Int,
    band: ScrubBand,
    viewportHeightPx: Int,
    visible: Boolean,
    favoriteKeys: Set<String>,
    onLaunch: (AppInfo) -> Unit,
    onSetFavorite: (AppInfo, Boolean) -> Unit,
    onSetName: (AppInfo, String?) -> Unit,
    onChangeIcon: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUnpinShortcut: (AppInfo) -> Unit,
    onHideApp: (AppInfo, Boolean) -> Unit,
    /** Which apps are hidden, so a search that turns one up can say so and put it back. */
    hiddenApps: Set<String>,
    onMoveToFolder: (AppInfo) -> Unit,
    /** Opens or closes a folder filed in the list. */
    onToggleFolder: (Folder) -> Unit = {},
    onManageFolder: (Folder) -> Unit = {},
    onDeleteFolder: (Folder) -> Unit = {},
    onSetFolderOnHome: (Folder, Boolean) -> Unit = { _, _ -> },
    onRemoveFromFolder: (Folder, AppInfo) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    contentColor: Color,
    showAlphabet: Boolean,
    edgeSide: EdgeSide,
    scrubStyle: ScrubStyle,
    scrubBubbleShape: ScrubBubbleShape,
    rippleParams: RippleParams,
    /** Hoisted so a swipe that overshoots the opening animation can keep scrolling it. */
    listState: LazyListState,
    /** How far the list still has to travel to be fully open; 0 once it has arrived. */
    enterPullPx: Float,
    /** Whether the status bar is set to stay hidden here, so its gap is not reserved. */
    statusBarHidden: Boolean,
    /** The model a query runs against, which may carry hidden apps the list itself omits. */
    searchModel: AppListModel,
    /** Whether a sideways swipe on a row offers its app's shortcuts. */
    shortcutSwipe: ShortcutSwipe,
    /** False when the favorites are computed, so starring would write to a list nobody sees. */
    favoritesEditable: Boolean,
    /** Whether the list ends with a row that opens the launcher's own settings. */
    showSettingsRow: Boolean,
    /** Whether opening the list puts the cursor in the search box. */
    autoKeyboard: Boolean,
    /** Bumped when something has asked for the cursor to be in the search box. */
    searchFocusTick: Int,
    /** Rows a second to walk while a finger rests on a letter. */
    holdScrollSpeed: Int,
    /** Whether the rest of the list fades away while a letter is being scrubbed. */
    dimOthers: Boolean,
    /** Whether a work profile's apps say so after their name. */
    markWorkApps: Boolean,
    /** Width of the live edge zone, which rows have to clear whether or not letters show. */
    edgeZoneWidthDp: Int,
    /** Whether the launcher's own text carries a shadow for legibility. */
    textShadow: Boolean,
    /** How strong that shadow is. */
    textShadowStrength: Int,
    /** Whether a query matching nothing offers the words to the browser. */
    webSearchFallback: Boolean,
    onWebSearch: (String) -> Unit,
    /** Whether the last letters can reach the same line as the first. */
    lastLetterToLine: Boolean,
    searchEnabled: Boolean,
    searchAtBottom: Boolean,
    sectionTopPercent: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    alignment: HomeAlignment,
    iconSide: IconSide,
) {
    val parameters =
        AppListParameters(
            model = model,
            nameOverrides = nameOverrides,
            scrub = scrub,
            iconSizeDp = iconSizeDp,
            labelSizeSp = labelSizeSp,
            band = band,
            viewportHeightPx = viewportHeightPx,
            visible = visible,
            favoriteKeys = favoriteKeys,
            onLaunch = onLaunch,
            onSetFavorite = onSetFavorite,
            onSetName = onSetName,
            onChangeIcon = onChangeIcon,
            onAppInfo = onAppInfo,
            onUnpinShortcut = onUnpinShortcut,
            onHideApp = onHideApp,
            hiddenApps = hiddenApps,
            onMoveToFolder = onMoveToFolder,
            onToggleFolder = onToggleFolder,
            onManageFolder = onManageFolder,
            onDeleteFolder = onDeleteFolder,
            onSetFolderOnHome = onSetFolderOnHome,
            onRemoveFromFolder = onRemoveFromFolder,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss,
            contentColor = contentColor,
            showAlphabet = showAlphabet,
            edgeSide = edgeSide,
            scrubStyle = scrubStyle,
            scrubBubbleShape = scrubBubbleShape,
            rippleParams = rippleParams,
            listState = listState,
            enterPullPx = enterPullPx,
            statusBarHidden = statusBarHidden,
            searchModel = searchModel,
            shortcutSwipe = shortcutSwipe,
            favoritesEditable = favoritesEditable,
            showSettingsRow = showSettingsRow,
            autoKeyboard = autoKeyboard,
            searchFocusTick = searchFocusTick,
            holdScrollSpeed = holdScrollSpeed,
            dimOthers = dimOthers,
            markWorkApps = markWorkApps,
            edgeZoneWidthDp = edgeZoneWidthDp,
            textShadow = textShadow,
            textShadowStrength = textShadowStrength,
            webSearchFallback = webSearchFallback,
            onWebSearch = onWebSearch,
            lastLetterToLine = lastLetterToLine,
            searchEnabled = searchEnabled,
            searchAtBottom = searchAtBottom,
            sectionTopPercent = sectionTopPercent,
            query = query,
            onQueryChange = onQueryChange,
            alignment = alignment,
            iconSide = iconSide,
        )
    rememberAppListSession(parameters).Content()
}
