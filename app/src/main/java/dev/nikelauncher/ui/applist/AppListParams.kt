// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.runtime.Immutable
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.ScrubStyle

/** How the app list behaves, taken from the settings. */
@Immutable
internal class AppListOptions(
    val showAlphabet: Boolean,
    val edgeSide: EdgeSide,
    val scrubStyle: ScrubStyle,
    val scrubBubbleShape: ScrubBubbleShape,
    val rippleParams: RippleParams,
    /** Whether the status bar is set to stay hidden here, so its gap is not reserved. */
    val statusBarHidden: Boolean,
    /** Whether the list ends with a row that opens the launcher's own settings. */
    val showSettingsRow: Boolean,
    /** Whether opening the list puts the cursor in the search box. */
    val autoKeyboard: Boolean,
    /** Rows a second to walk while a finger rests on a letter. */
    val holdScrollSpeed: Int,
    /** Whether the rest of the list fades away while a letter is being scrubbed. */
    val dimOthers: Boolean,
    /** Whether a work profile's apps say so after their name. */
    val markWorkApps: Boolean,
    val textShadow: Boolean,
    val textShadowStrength: Int,
    /** Width of the live edge zone, which rows have to clear whether or not letters show. */
    val edgeZoneWidthDp: Int,
    /** Whether a query matching nothing offers the words to the browser. */
    val webSearchFallback: Boolean,
    /** Whether the last letters can reach the same line as the first. */
    val lastLetterToLine: Boolean,
    val searchEnabled: Boolean,
    val searchAtBottom: Boolean,
    val sectionTopPercent: Int,
)

/** What the app list can ask the rest of the app to do. */
internal class AppListActions(
    val onLaunch: (AppInfo) -> Unit,
    val onSetFavorite: (AppInfo, Boolean) -> Unit,
    val onSetName: (AppInfo, String?) -> Unit,
    val onChangeIcon: (AppInfo) -> Unit,
    val onAppInfo: (AppInfo) -> Unit,
    val onUnpinShortcut: (AppInfo) -> Unit,
    val onHideApp: (AppInfo, Boolean) -> Unit,
    val onMoveToFolder: (AppInfo) -> Unit,
    val onToggleFolder: (Folder) -> Unit,
    val onManageFolder: (Folder) -> Unit,
    val onDeleteFolder: (Folder) -> Unit,
    val onSetFolderOnHome: (Folder, Boolean) -> Unit,
    val onRemoveFromFolder: (Folder, AppInfo) -> Unit,
    val onOpenSettings: () -> Unit,
    val onDismiss: () -> Unit,
    val onWebSearch: (String) -> Unit,
)
