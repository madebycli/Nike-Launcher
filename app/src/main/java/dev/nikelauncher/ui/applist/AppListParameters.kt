// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.Folder
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

/** Inputs and callbacks supplied by the home route. */
internal data class AppListParameters(
    val model: AppListModel,
    val nameOverrides: Map<String, String>,
    val scrub: ScrubState,
    val iconSizeDp: Int,
    val labelSizeSp: Int,
    val band: ScrubBand,
    val viewportHeightPx: Int,
    val visible: Boolean,
    val favoriteKeys: Set<String>,
    val onLaunch: (AppInfo) -> Unit,
    val onSetFavorite: (AppInfo, Boolean) -> Unit,
    val onSetName: (AppInfo, String?) -> Unit,
    val onChangeIcon: (AppInfo) -> Unit,
    val onAppInfo: (AppInfo) -> Unit,
    val onUnpinShortcut: (AppInfo) -> Unit,
    val onHideApp: (AppInfo, Boolean) -> Unit,
    val hiddenApps: Set<String>,
    val onMoveToFolder: (AppInfo) -> Unit,
    val onToggleFolder: (Folder) -> Unit = {},
    val onManageFolder: (Folder) -> Unit = {},
    val onDeleteFolder: (Folder) -> Unit = {},
    val onSetFolderOnHome: (Folder, Boolean) -> Unit = { _, _ -> },
    val onRemoveFromFolder: (Folder, AppInfo) -> Unit = { _, _ -> },
    val onOpenSettings: () -> Unit,
    val onDismiss: () -> Unit,
    val contentColor: Color,
    val showAlphabet: Boolean,
    val edgeSide: EdgeSide,
    val scrubStyle: ScrubStyle,
    val scrubBubbleShape: ScrubBubbleShape,
    val rippleParams: RippleParams,
    val listState: LazyListState,
    val enterPullPx: Float,
    val statusBarHidden: Boolean,
    val searchModel: AppListModel,
    val shortcutSwipe: ShortcutSwipe,
    val favoritesEditable: Boolean,
    val showSettingsRow: Boolean,
    val autoKeyboard: Boolean,
    val searchFocusTick: Int,
    val holdScrollSpeed: Int,
    val dimOthers: Boolean,
    val markWorkApps: Boolean,
    val edgeZoneWidthDp: Int,
    val textShadow: Boolean,
    val textShadowStrength: Int,
    val webSearchFallback: Boolean,
    val onWebSearch: (String) -> Unit,
    val lastLetterToLine: Boolean,
    val searchEnabled: Boolean,
    val searchAtBottom: Boolean,
    val sectionTopPercent: Int,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val alignment: HomeAlignment,
    val iconSide: IconSide,
)
