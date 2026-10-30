// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import android.app.SearchManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.ui.applist.AppListActions
import dev.nikelauncher.ui.applist.AppListOptions
import dev.nikelauncher.ui.applist.AppListScreen
import dev.nikelauncher.ui.applist.ScrubBand
import dev.nikelauncher.ui.applist.ScrubState
import dev.nikelauncher.ui.applist.rows.ListRowStyle
import dev.nikelauncher.ui.home.HomeData
import dev.nikelauncher.ui.home.HomeSettings
import kotlinx.coroutines.launch

/** Wires the app list overlay to the preferences, the launcher and the route's controller. */
@Composable
internal fun AppListHost(
    app: NikeApp,
    settings: HomeSettings,
    data: HomeData,
    models: ListModels,
    scrub: ScrubState,
    band: ScrubBand,
    viewportHeightPx: Int,
    controller: AppListController,
    favoriteKeys: List<String>,
    hiddenApps: Set<String>,
    insetLiveDp: Int,
    onMoveToFolder: (AppInfo) -> Unit,
    onToggleFolder: (Folder) -> Unit,
    launchEntry: (AppInfo) -> Boolean,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = app.prefs.library
    // Asked once: whether anything on this phone handles a web search at all.
    val webSearchAvailable = remember(context) {
        runCatching { context.packageManager.resolveActivity(Intent(Intent.ACTION_WEB_SEARCH), 0) != null }.getOrDefault(false)
    }

    val style = remember(
        settings.contentColor, settings.appListAlignment, settings.iconSide, settings.iconSizeDp,
        settings.labelSizeSp, settings.shortcutSwipe, settings.favoritesSource,
    ) {
        ListRowStyle(
            contentColor = settings.contentColor,
            alignment = settings.appListAlignment,
            iconSide = settings.iconSide,
            iconSizeDp = settings.iconSizeDp,
            labelSizeSp = settings.labelSizeSp,
            shortcutSwipe = settings.shortcutSwipe,
            favoritesEditable = settings.favoritesSource == FavoritesSource.MANUAL,
        )
    }
    val options = AppListOptions(
        showAlphabet = settings.showAlphabet,
        edgeSide = settings.edgeSide,
        scrubStyle = settings.scrubStyle,
        scrubBubbleShape = settings.scrubBubbleShape,
        rippleParams = settings.rippleParams,
        statusBarHidden = settings.hideStatusBarAppList,
        showSettingsRow = settings.showSettingsRow,
        autoKeyboard = settings.autoKeyboard,
        holdScrollSpeed = settings.holdScrollSpeed,
        dimOthers = settings.dimOthersScrubbing,
        markWorkApps = settings.markWorkApps,
        textShadow = settings.textShadow,
        textShadowStrength = settings.textShadowStrength,
        edgeZoneWidthDp = settings.edgeZoneWidthDp + insetLiveDp,
        // Offered only where something can take it, or the row is a dead end.
        webSearchFallback = settings.webSearchFallback && webSearchAvailable,
        lastLetterToLine = settings.lastLetterToLine,
        searchEnabled = settings.appListSearch,
        searchAtBottom = settings.appListSearchBottom,
        sectionTopPercent = settings.sectionTopPercent,
    )
    val actions = AppListActions(
        onLaunch = { appInfo ->
            // A launch that never got off the ground leaves nothing to wait for, and the
            // private-space row has nothing coming to the foreground either: it changes what this
            // very list holds, so it wants the list out of the way, unless asked to stay.
            val stay = appInfo.kind == EntryKind.PRIVATE_SPACE && settings.stayAfterPrivateUnlock
            if (stay) controller.keepThroughNextPause()
            val acted = launchEntry(appInfo)
            when {
                stay -> Unit
                acted && appInfo.kind == EntryKind.APP -> controller.closeAfterLaunch()
                else -> controller.close()
            }
        },
        onSetFavorite = { appInfo, add ->
            scope.launch { if (add) library.addFavorite(appInfo.key) else library.removeFavorite(appInfo.key) }
        },
        onSetName = { appInfo, name -> scope.launch { library.setNameOverride(appInfo.key, name) } },
        onChangeIcon = { appInfo -> controller.close(); onNavigate(iconPickerRoute(appInfo.key)) },
        onAppInfo = { app.appRepository.openAppInfo(it) },
        onUnpinShortcut = { appInfo -> controller.close(); app.appRepository.unpin(appInfo) },
        onHideApp = { appInfo, hide -> scope.launch { library.setHidden(appInfo.key, hide) } },
        onMoveToFolder = { appInfo -> controller.close(); onMoveToFolder(appInfo) },
        onToggleFolder = onToggleFolder,
        onManageFolder = { folder -> controller.close(); onNavigate("folder/" + folder.id) },
        onDeleteFolder = { folder ->
            // Its widgets go with it, released so their apps stop updating them.
            folder.widgets.forEach { runCatching { app.widgetHost.deleteAppWidgetId(it.id) } }
            scope.launch { library.deleteFolder(folder.id) }
        },
        onSetFolderOnHome = { folder, onHome ->
            scope.launch { if (onHome) library.addFavorite(folderToken(folder.id)) else library.removeFavorite(folderToken(folder.id)) }
        },
        onRemoveFromFolder = { folder, appInfo -> scope.launch { library.removeAppFromFolder(folder.id, appInfo.key) } },
        onOpenSettings = { controller.close(); onNavigate("settings") },
        onDismiss = { controller.close() },
        onWebSearch = { term ->
            val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, term).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            controller.close()
        },
    )

    AppListScreen(
        model = models.list,
        searchModel = models.search,
        nameOverrides = data.nameOverrides,
        scrub = scrub,
        style = style,
        options = options,
        actions = actions,
        band = band,
        viewportHeightPx = viewportHeightPx,
        visible = controller.visible,
        favoriteKeys = remember(favoriteKeys) { favoriteKeys.toSet() },
        hiddenApps = hiddenApps,
        listState = controller.listState,
        // Distance still to travel, which is exactly what the collapse transform takes: the list
        // arrives scaled down and faded, and grows into place.
        enterPullPx = controller.openDistancePx - controller.openAnim.value,
        searchFocusTick = controller.searchFocusTick,
        query = controller.query,
        onQueryChange = { controller.query = it },
    )
}
