// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.BlockPin
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.BlockOffsets
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.QuickLaunchSlot
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.data.restoreConcealed
import dev.nikelauncher.service.SystemUi
import dev.nikelauncher.ui.applist.ScrubberGeometry
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.home.AppActions
import dev.nikelauncher.ui.home.CornerActions
import dev.nikelauncher.ui.home.FolderActions
import dev.nikelauncher.ui.home.GestureActions
import dev.nikelauncher.ui.home.HomeActions
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.home.LayoutActions
import dev.nikelauncher.widget.WidgetSlotActions
import kotlinx.coroutines.launch

/** The route's pieces the home actions reach into. */
/** The furthest a block can be held from the screen edge. */
private const val EDGE_DISTANCE_MAX_DP = 120

internal class HomeRouteHooks(
    val launchEntry: (AppInfo) -> Boolean,
    val controller: AppListController,
    val bands: BandEditing,
    val onMoveToFolder: (AppInfo) -> Unit,
    val setCornerBounds: (Rect?) -> Unit,
    val onEditModeBand: () -> Unit,
    val onPeekStatusBar: () -> Unit,
    val onNavigate: (String) -> Unit,
    /** True while something other than the plain home screen is up, so measurements are ignored. */
    val homeBusy: () -> Boolean,
    val viewportHeightPx: () -> Int,
    val blockOffsets: () -> BlockOffsets,
    val minBandHeightPx: () -> Float,
)

/** Builds what the home screen can ask for, wired to the preferences and the rest of the route. */
@Composable
internal fun rememberHomeActions(
    app: NikeApp,
    settings: HomeSettings,
    favoriteKeys: List<String>,
    cornerApp: AppInfo?,
    cornerIsSearch: Boolean,
    widgets: WidgetSlotActions,
    hooks: HomeRouteHooks,
): HomeActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = app.prefs.library
    return remember(settings, favoriteKeys, cornerApp, cornerIsSearch, widgets, hooks) {
        HomeActions(
            apps = AppActions(
                onLaunch = { hooks.launchEntry(it) },
                // Remove means the same thing to whoever taps it either way: take this off my
                // home screen. Which list it writes to depends on which list is being shown;
                // against the computed favorites it used to edit the manual list nobody could see.
                onRemoveFavorite = {
                    scope.launch {
                        if (settings.favoritesSource == FavoritesSource.FREQUENT) library.setFrequentExcluded(it.key, excluded = true)
                        else library.removeFavorite(it.key)
                    }
                },
                onMoveToFolder = hooks.onMoveToFolder,
                onSetName = { appInfo, name -> scope.launch { library.setNameOverride(appInfo.key, name) } },
                onChangeIcon = { hooks.onNavigate(iconPickerRoute(it.key)) },
                onAppInfo = { app.appRepository.openAppInfo(it) },
                onUnpinShortcut = { app.appRepository.unpin(it) },
            ),
            folders = FolderActions(
                onOpenApp = { hooks.launchEntry(it) },
                onRemoveApp = { folder, appInfo -> scope.launch { library.removeAppFromFolder(folder.id, appInfo.key) } },
                onRename = { folder, name -> scope.launch { library.upsertFolder(folder.copy(name = name)) } },
                onChangeIcon = { hooks.onNavigate(iconPickerRoute(folderToken(it.id))) },
                onResetIcon = { folder -> scope.launch { library.setFolderIcon(folder.id, null) } },
                onDelete = { folder ->
                    // Its widgets go with it, released so their apps stop updating them.
                    folder.widgets.forEach { runCatching { app.widgetHost.deleteAppWidgetId(it.id) } }
                    scope.launch { library.deleteFolder(folder.id) }
                },
                onManage = { hooks.onNavigate("folder/" + it.id) },
                onRemoveFromHome = if (!settings.foldersInAppList) null else { folder ->
                    scope.launch { library.removeFavorite(folderToken(folder.id)) }
                },
            ),
            layout = LayoutActions(
                onReorderFavorites = { drawnOrder ->
                    scope.launch {
                        // The home screen draws only the favorites that resolve to a row, and this
                        // hands back the order of what it drew. Written as it stands it would delete
                        // every favorite that drew nothing, which is what a locked private space's
                        // favorites do. Whatever drew no row goes back where it was.
                        val drawn = drawnOrder.toSet()
                        library.setFavorites(restoreConcealed(favoriteKeys, drawnOrder) { it !in drawn })
                    }
                },
                onCommitPin = { block: HomeBlock, pin: BlockPin? -> scope.launch { app.prefs.home.setHomePin(block, pin) } },
                onNudgeX = { block, dp ->
                    scope.launch { app.prefs.home.nudgeBlockOffset(block, dp) }
                },
                onNudgeEdge = { block, dp ->
                    scope.launch {
                        app.prefs.homeLayout.setEdges(setOf(block), delta = dp)
                    }
                },
                onResetOffset = { block -> scope.launch { app.prefs.home.setBlockOffset(block, 0) } },
                onResizeNowPlaying = { scope.launch { app.prefs.home.setNowPlayingHeightDp(it) } },
                onFavoritesBoundsChanged = { top, bottom ->
                    // Measured in window space, so a home screen still travelling under the open
                    // overlay would drag the alphabet along with it. Edit mode is ignored for the
                    // same reason: the strip should match where the favorites actually sit.
                    if (hooks.homeBusy()) return@LayoutActions
                    hooks.bands.favoritesBand = ScrubberGeometry.bandForFavorites(
                        topPx = top,
                        bottomPx = bottom,
                        viewportPx = hooks.viewportHeightPx().toFloat(),
                        minHeightPx = hooks.minBandHeightPx(),
                    )
                },
                onEditScrubBand = hooks.onEditModeBand,
                onCommitPins = { pins -> scope.launch { app.prefs.homeLayout.setPins(pins) } },
                onSetBlockGap = { dp -> scope.launch { app.prefs.homeLayout.setGap(dp) } },
                onSetRowSpacing = { dp -> scope.launch { app.prefs.appearance.setItemSpacingDp(dp) } },
                onSetEdges = { blocks, dp -> scope.launch { app.prefs.homeLayout.setEdges(blocks, dp) } },
            ),
            gestures = GestureActions(
                onSwipeUpDrag = hooks.controller::onSwipeUpDrag,
                onSwipeUpEnd = hooks.controller::onSwipeUpEnd,
                onQuickLaunch = { slot ->
                    val target = when (slot) {
                        QuickLaunchSlot.LEFT -> settings.quickLaunchLeft
                        QuickLaunchSlot.RIGHT -> settings.quickLaunchRight
                    }
                    target?.let { hooks.launchEntry(it) }
                },
                onPeekStatusBar = hooks.onPeekStatusBar,
                onExpandShade = {
                    // Say so rather than failing silently, so a pull that does nothing tells you
                    // whether the gesture or the permission is at fault.
                    if (!SystemUi.expandNotificationShade(context)) context.toast(R.string.toast_enable_accessibility_shade)
                },
            ),
            widgets = widgets,
            onOpenSettings = { hooks.onNavigate("settings") },
            onManageFavorites = { hooks.onNavigate("favorites") },
            corner = CornerActions(
                app = cornerApp,
                isSearch = cornerIsSearch,
                onOpenApp = { hooks.launchEntry(it) },
                onOpenSearch = { hooks.controller.openFully(focusSearch = true) },
                onBounds = hooks.setCornerBounds,
                onOpenSettings = { hooks.onNavigate("settings") },
                onPickApp = { hooks.onNavigate("cornerpicker") },
            ),
        )
    }
}

internal fun iconPickerRoute(key: String) = "iconpicker/" + Uri.encode(key)
