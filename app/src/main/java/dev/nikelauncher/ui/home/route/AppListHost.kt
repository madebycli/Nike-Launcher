// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import android.app.SearchManager
import dev.nikelauncher.ui.home.corner.FloatingSearchButton
import dev.nikelauncher.ui.theme.LocalTextCloudStrength
import dev.nikelauncher.data.CornerAppearance
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.ui.applist.AppListScreen
import dev.nikelauncher.ui.applist.ScrubBand
import dev.nikelauncher.ui.applist.ScrubState
import dev.nikelauncher.ui.home.HomeData
import dev.nikelauncher.ui.home.HomeSettings
import kotlinx.coroutines.launch

/** Wires the app list overlay to the preferences, the launcher and the route's controller. */
@OptIn(ExperimentalLayoutApi::class)
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

    // The list's own search field stays off unless asked for; the floating button asks.
    var searchRequested by remember { mutableStateOf(false) }
    LaunchedEffect(controller.visible, settings.searchDisplayMode) { if (!controller.visible || settings.searchDisplayMode != dev.nikelauncher.data.prefs.SearchDisplayMode.BUTTON) searchRequested = false }
    val floatingSearch = settings.searchDisplayMode == dev.nikelauncher.data.prefs.SearchDisplayMode.BUTTON
    val imeVisible = WindowInsets.isImeVisible
    var keyboardSeen by remember { mutableStateOf(false) }
    LaunchedEffect(controller.visible, searchRequested, imeVisible, controller.query, settings.appListSearch) {
        if (!controller.visible || !searchRequested || settings.appListSearch) keyboardSeen = false
        else if (imeVisible) keyboardSeen = true
        else if (dismissEmptySearch(keyboardSeen, imeVisible, controller.query)) searchRequested = false
    }
    LaunchedEffect(controller.searchFocusTick) {
        if (controller.visible && controller.searchFocusTick > 0) searchRequested = true
    }
    LaunchedEffect(controller.visible, settings.autoKeyboard, floatingSearch) {
        if (controller.visible && settings.autoKeyboard && floatingSearch) searchRequested = true
    }
    val hideWhileScrolling by app.prefs.corner.hideSearchWhileScrolling.collectAsState(true)
    val searchAppearance by app.prefs.corner.searchAppearance.collectAsState(CornerAppearance())

    val textCloud by app.prefs.appList.textCloud.collectAsState(0)
    CompositionLocalProvider(LocalTextCloudStrength provides textCloud) {
    Box(Modifier.fillMaxSize()) {
        AppListScreen(
            model = models.list,
            nameOverrides = data.nameOverrides,
            scrub = scrub,
            iconSizeDp = settings.iconSizeDp,
            labelSizeSp = settings.labelSizeSp,
            band = band,
            viewportHeightPx = viewportHeightPx,
            visible = controller.visible,
            favoriteKeys = remember(favoriteKeys) { favoriteKeys.toSet() },
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
            hiddenApps = hiddenApps,
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
            contentColor = settings.contentColor,
            showAlphabet = settings.showAlphabet,
            edgeSide = settings.edgeSide,
            scrubStyle = settings.scrubStyle,
            scrubBubbleShape = settings.scrubBubbleShape,
            rippleParams = settings.rippleParams,
            statusBarHidden = settings.hideStatusBarAppList,
            searchModel = models.search,
            shortcutSwipe = settings.shortcutSwipe,
            favoritesEditable = settings.favoritesSource == dev.nikelauncher.data.FavoritesSource.MANUAL,
            showSettingsRow = settings.showSettingsRow,
            autoKeyboard = settings.autoKeyboard,
            searchFocusTick = controller.searchFocusTick,
            holdScrollSpeed = settings.holdScrollSpeed,
            dimOthers = settings.dimOthersScrubbing,
            markWorkApps = settings.markWorkApps,
            textShadow = settings.textShadow,
            textShadowStrength = settings.textShadowStrength,
            edgeZoneWidthDp = settings.edgeZoneWidthDp + insetLiveDp,
            // Offered only where something can take it, or the row is a dead end.
            webSearchFallback = settings.webSearchFallback && webSearchAvailable,
            onWebSearch = { term ->
                val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, term).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                controller.close()
            },
            lastLetterToLine = settings.lastLetterToLine,
            searchEnabled = settings.appListSearch || searchRequested,
            searchAtBottom = if (settings.appListSearch) settings.appListSearchBottom else settings.buttonSearchBottom,
            sectionTopPercent = settings.sectionTopPercent,
            listState = controller.listState,
            // Distance still to travel, which is exactly what the collapse transform takes: the list
            // arrives scaled down and faded, and grows into place.
            enterPullPx = controller.openDistancePx - controller.openAnim.value,
            query = controller.query,
            onQueryChange = { controller.query = it },
            alignment = settings.appListAlignment,
            iconSide = settings.iconSide,
        )
        if (floatingSearch && !settings.appListSearch) {
            FloatingSearchButton(
                visible = controller.visible && !searchRequested && controller.query.isBlank() &&
                    (!hideWhileScrolling || (!scrub.scrubbing && !controller.listState.isScrollInProgress)),
                appearance = searchAppearance,
                overWallpaper = settings.contentColor,
                onClick = { searchRequested = true; controller.focusSearch() },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
    }
}
