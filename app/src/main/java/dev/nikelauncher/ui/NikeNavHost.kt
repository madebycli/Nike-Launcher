// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.nikelauncher.NikeApp
import dev.nikelauncher.TypedKey
import dev.nikelauncher.data.EntryKeys
import dev.nikelauncher.data.folderIdFromToken
import dev.nikelauncher.ui.apps.rememberAppsState
import dev.nikelauncher.ui.home.HomeData
import dev.nikelauncher.ui.home.HomeRoute
import dev.nikelauncher.ui.home.setup.SetupFlow
import dev.nikelauncher.ui.nav.EnsureDefaultWidget
import dev.nikelauncher.ui.nav.WarmIconCache
import dev.nikelauncher.ui.nav.iconPickerDestination
import dev.nikelauncher.ui.nav.listDestinations
import dev.nikelauncher.ui.nav.rememberGalleryIconPicker
import dev.nikelauncher.ui.nav.rememberWidgetActions
import dev.nikelauncher.ui.settings.SettingsNav
import dev.nikelauncher.ui.settings.SettingsRoute
import dev.nikelauncher.ui.state.buildFavoriteEntries
import dev.nikelauncher.ui.state.rememberHomeSettings
import dev.nikelauncher.ui.state.rememberLibraryState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The navigation graph. What the destinations share is read here, once, and handed down. */
@Composable
fun NikeNavHost(
    app: NikeApp,
    homeIntentTick: Int,
    typedToSearch: List<TypedKey>,
    onTypedToSearchHandled: (List<TypedKey>) -> Unit,
    iconPackPackage: String?,
    iconOverrides: Map<String, String>,
    onPeekStatusBar: () -> Unit,
    /** Reported up so the status bar can stay put while the overlay is showing. */
    onAppListVisibleChange: (Boolean) -> Unit,
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    // Before anything the user does can write to the store, so "the store is empty" still means
    // "this is a first run" when it is read.
    LaunchedEffect(Unit) { app.prefs.home.ensureInstallMarker() }

    val apps = rememberAppsState(app)
    val library = rememberLibraryState(app.prefs)
    val appsByKey = remember(apps.apps) { apps.apps.associateBy { it.key } }
    val settings = rememberHomeSettings(app.prefs, library, appsByKey)

    EnsureDefaultWidget(app, library.layoutDefaultsVersion, library.widgetIds)
    WarmIconCache(apps.apps, library.favoriteKeys, library.folders, iconPackPackage, iconOverrides, settings.iconSizeDp)

    val foldersById = remember(library.folders) { library.folders.associateBy { it.id } }
    val favorites = remember(
        library.favoritesSource, library.favoriteKeys, appsByKey, foldersById,
        if (library.favoritesSource == dev.nikelauncher.data.FavoritesSource.FREQUENT) library.launchCounts else emptyMap<String, Int>(),
        library.frequentCount, library.frequentExcluded,
    ) {
        buildFavoriteEntries(
            library.favoritesSource, library.favoriteKeys, appsByKey, foldersById,
            library.launchCounts, library.frequentCount, library.frequentExcluded,
        )
    }

    // Noted the first time there is one, and never unset. Whether the hint is still wanted is a
    // question about whether someone has done this before, not about the list being empty now.
    LaunchedEffect(library.favoriteKeys.isNotEmpty()) {
        if (library.favoriteKeys.isNotEmpty()) app.prefs.library.markFavoritesEverSet()
    }

    val widgetActions = rememberWidgetActions(app) { nav.navigate("settings") }
    val pickFromGallery = rememberGalleryIconPicker(app)

    // HOME has to unwind the whole stack. The equivalent effect inside the home destination can't
    // do this: that destination isn't composed while Settings is on screen.
    LaunchedEffect(homeIntentTick) {
        if (homeIntentTick > 0 && nav.currentDestination?.route != "home") nav.popBackStack("home", inclusive = false)
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeRoute(
                app = app,
                homeIntentTick = homeIntentTick,
                settings = settings,
                data = HomeData(
                    favorites = favorites,
                    appsByKey = appsByKey,
                    nameOverrides = library.nameOverrides,
                    widgetIds = library.widgetIds,
                    widgetHeightDp = library.widgetHeightDp,
                    nowPlayingHasContent = false,
                    pins = library.pins,
                    blockOffsets = library.blockOffsets,
                ),
                folders = library.folders,
                favoriteKeys = library.favoriteKeys,
                hiddenApps = library.hiddenApps,
                launchCounts = library.launchCounts,
                widgetActions = widgetActions,
                cornerApp = library.cornerButtonKey?.takeIf { it != EntryKeys.SEARCH }?.let { appsByKey[it] },
                cornerIsSearch = library.cornerButtonKey == EntryKeys.SEARCH,
                typedToSearch = typedToSearch,
                onTypedToSearchHandled = onTypedToSearchHandled,
                // Both flows start at a value that shows nothing, so the dialog can't flash before
                // the stored answer arrives. A legacy install is stamped 0 and never qualifies.
                scrubBandFractions = library.scrubBand,
                onPeekStatusBar = onPeekStatusBar,
                onAppListVisibleChange = onAppListVisibleChange,
                onTogglePrivateSpace = apps::togglePrivateSpace,
                onNavigate = { route -> nav.navigate(route) },
            )
        }

        composable("settings") {
            SettingsRoute(
                app = app,
                allApps = apps.apps,
                privateSpace = apps.privateSpace,
                homeTick = homeIntentTick,
                nav = SettingsNav(
                    onHiddenApps = { nav.navigate("settings/hidden") },
                    onFrequentExcluded = { nav.navigate("settings/frequentexcluded") },
                    onFavorites = { nav.navigate("favorites") },
                    onQuickLaunchPicker = { slot -> nav.navigate("apppicker/" + slot.name) },
                    onCornerPicker = { nav.navigate("cornerpicker") },
                ),
                onBack = { nav.popBackStack() },
            )
        }

        listDestinations(app, nav, apps, library, settings.iconSizeDp)
        iconPickerDestination(app, nav, apps, library, pickFromGallery)
    }

    // Both flows start at a value that shows nothing, so this can't flash before the stored
    // answer arrives. A legacy install is stamped 0 and never qualifies.
    if (library.layoutDefaultsVersion == 1 && !library.welcomeSeen) {
        SetupFlow(app, apps, library, settings.iconSizeDp) {
            scope.launch {
                // A restored backup already holds the choices a new install would be given.
                if (!app.prefs.backupRestored.first()) app.prefs.home.applyNewInstallDefaults()
                app.prefs.home.setWelcomeSeen(true)
            }
        }
    }
}
