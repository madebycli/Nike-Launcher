// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.BlockPin
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.HomePins
import dev.nikelauncher.data.QuickLaunchSlot
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.widget.WidgetSlotActions

/** A favorites row is an app or a folder; both reorder through the same list. */
sealed interface FavoriteEntry {
    val token: String

    data class App(val app: AppInfo) : FavoriteEntry {
        override val token: String get() = app.key
    }

    data class FolderRef(val folder: Folder) : FavoriteEntry {
        override val token: String get() = folderToken(folder.id)
    }
}

/** What the home screen shows: the rows, the widgets and where each block is held. */
@Immutable
data class HomeData(
    val favorites: List<FavoriteEntry>,
    val appsByKey: Map<String, AppInfo>,
    val nameOverrides: Map<String, String>,
    val widgetIds: List<Int>,
    val widgetHeightDp: Int,
    val nowPlayingHasContent: Boolean,
    val pins: HomePins,
) {
    fun displayName(app: AppInfo, markWorkApps: Boolean, workSuffix: String): String {
        val base = nameOverrides[app.key] ?: app.label
        return if (markWorkApps && app.work) base + workSuffix else base
    }
}

/** Everything the home screen can ask the rest of the app to do, grouped by what it concerns. */
class HomeActions(
    val apps: AppActions,
    val folders: FolderActions,
    val layout: LayoutActions,
    val gestures: GestureActions,
    val widgets: WidgetSlotActions,
    val onOpenSettings: () -> Unit,
    val onManageFavorites: () -> Unit,
    val corner: CornerActions,
)

class AppActions(
    val onLaunch: (AppInfo) -> Unit,
    val onRemoveFavorite: (AppInfo) -> Unit,
    val onMoveToFolder: (AppInfo) -> Unit,
    val onSetName: (AppInfo, String?) -> Unit,
    val onChangeIcon: (AppInfo) -> Unit,
    val onAppInfo: (AppInfo) -> Unit,
    val onUnpinShortcut: (AppInfo) -> Unit,
)

class FolderActions(
    val onOpenApp: (AppInfo) -> Unit,
    val onRemoveApp: (Folder, AppInfo) -> Unit,
    val onRename: (Folder, String) -> Unit,
    val onChangeIcon: (Folder) -> Unit,
    val onResetIcon: (Folder) -> Unit,
    val onDelete: (Folder) -> Unit,
    val onManage: (Folder) -> Unit,
    /** Set when folders are also in the app list, where one taken off here still lives. */
    val onRemoveFromHome: ((Folder) -> Unit)?,
)

class LayoutActions(
    val onReorderFavorites: (List<String>) -> Unit,
    val onCommitPin: (HomeBlock, BlockPin?) -> Unit,
    val onResizeNowPlaying: (Int) -> Unit,
    val onFavoritesBoundsChanged: (topPx: Float, bottomPx: Float) -> Unit,
    val onEditScrubBand: () -> Unit,
)

class GestureActions(
    /** Total distance dragged up past the end, and this frame's share of it. */
    val onSwipeUpDrag: (total: Float, delta: Float) -> Unit,
    val onSwipeUpEnd: (velocity: Float) -> Unit,
    val onQuickLaunch: (QuickLaunchSlot) -> Unit,
    val onPeekStatusBar: () -> Unit,
    val onExpandShade: () -> Unit,
)

class CornerActions(
    /** The app a button in the bottom corner opens, or null when there is no button. */
    val app: AppInfo?,
    /** True when the corner opens the app list's search rather than launching anything. */
    val isSearch: Boolean,
    val onOpenApp: (AppInfo) -> Unit,
    val onOpenSearch: () -> Unit,
    /** Where the corner button is in the window, or null when there is none to keep clear. */
    val onBounds: (Rect?) -> Unit,
    val onOpenSettings: () -> Unit,
    /** Opens the screen where the app the button launches is chosen. */
    val onPickApp: () -> Unit,
)
