// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.EntryKeys
import dev.nikelauncher.data.QuickLaunchSlot
import dev.nikelauncher.data.folderIdFromToken
import dev.nikelauncher.ui.apps.AppsState
import dev.nikelauncher.ui.common.IconPickerScreen
import dev.nikelauncher.ui.common.encodePackOverride
import dev.nikelauncher.ui.settings.AppPickerScreen
import dev.nikelauncher.ui.settings.FolderAppsScreen
import dev.nikelauncher.ui.settings.HiddenAppsScreen
import dev.nikelauncher.ui.settings.ManageFavoritesScreen
import dev.nikelauncher.ui.state.LibraryState
import kotlinx.coroutines.launch

/** The screens that choose which apps go where: favorites, folders, quick launch, hidden apps. */
internal fun NavGraphBuilder.listDestinations(
    app: NikeApp,
    nav: NavController,
    apps: AppsState,
    library: LibraryState,
    iconSizeDp: Int,
) {
    val foldersById = library.folders.associateBy { it.id }

    composable("favorites") {
        val scope = rememberCoroutineScope()
        ManageFavoritesScreen(
            allApps = apps.apps,
            favoriteKeys = library.favoriteKeys,
            folders = library.folders,
            privateSpace = apps.privateSpace,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onReorder = { keys -> scope.launch { app.prefs.library.setFavorites(keys) } },
            onSetFavorite = { appInfo, add ->
                scope.launch { if (add) app.prefs.library.addFavorite(appInfo.key) else app.prefs.library.removeFavorite(appInfo.key) }
            },
            // Missing here can just mean a locked private space or a paused work profile, both of
            // which come back, so this only drops the favorite, the same as unticking a row that
            // IS resolved. forgetEntry's wider wipe is for a shortcut that is gone for good, which
            // AppRepository.unpin already calls on its own.
            onForget = { key -> scope.launch { app.prefs.library.removeFavorite(key) } },
            onBack = { nav.popBackStack() },
        )
    }

    composable("folder/{id}") { entry ->
        val scope = rememberCoroutineScope()
        val id = entry.arguments?.getString("id").orEmpty()
        FolderAppsScreen(
            folder = foldersById[id],
            allApps = apps.apps,
            privateSpace = apps.privateSpace,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onSetInFolder = { appInfo, inFolder ->
                scope.launch {
                    if (inFolder) app.prefs.library.addAppToFolder(id, appInfo.key)
                    else app.prefs.library.removeAppFromFolder(id, appInfo.key)
                }
            },
            onReorder = { keys -> scope.launch { app.prefs.library.setFolderApps(id, keys) } },
            onForget = { key -> scope.launch { app.prefs.library.removeAppFromFolder(id, key) } },
            onBack = { nav.popBackStack() },
        )
    }

    composable("apppicker/{slot}") { entry ->
        val scope = rememberCoroutineScope()
        val slot = runCatching { QuickLaunchSlot.valueOf(entry.arguments?.getString("slot").orEmpty()) }.getOrDefault(QuickLaunchSlot.LEFT)
        AppPickerScreen(
            title = stringResource(if (slot == QuickLaunchSlot.LEFT) R.string.settings_quick_launch_left else R.string.settings_quick_launch_right),
            allApps = apps.apps,
            selectedKey = if (slot == QuickLaunchSlot.LEFT) library.quickLaunchLeftKey else library.quickLaunchRightKey,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onPick = { picked ->
                scope.launch { app.prefs.library.setQuickLaunch(slot, picked?.key) }
                nav.popBackStack()
            },
            onBack = { nav.popBackStack() },
        )
    }

    composable("cornerpicker") {
        val scope = rememberCoroutineScope()
        AppPickerScreen(
            title = stringResource(R.string.settings_corner_button),
            allApps = apps.apps,
            selectedKey = library.cornerButtonKey,
            extraChoice = EntryKeys.SEARCH to stringResource(R.string.settings_corner_search),
            onPickExtra = { key ->
                scope.launch { app.prefs.library.setCornerButton(key) }
                nav.popBackStack()
            },
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onPick = { picked ->
                scope.launch { app.prefs.library.setCornerButton(picked?.key) }
                nav.popBackStack()
            },
            onBack = { nav.popBackStack() },
        )
    }

    composable("settings/hidden") {
        val scope = rememberCoroutineScope()
        HiddenAppsScreen(
            allApps = apps.apps,
            hiddenApps = library.hiddenApps,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onToggleHidden = { appInfo, hidden -> scope.launch { app.prefs.library.setHidden(appInfo.key, hidden) } },
            onBack = { nav.popBackStack() },
        )
    }

    composable("settings/frequentexcluded") {
        val scope = rememberCoroutineScope()
        HiddenAppsScreen(
            allApps = apps.apps,
            hiddenApps = library.frequentExcluded,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onToggleHidden = { appInfo, excluded -> scope.launch { app.prefs.library.setFrequentExcluded(appInfo.key, excluded) } },
            onBack = { nav.popBackStack() },
            titleRes = R.string.settings_frequent_excluded,
            countRes = R.string.frequent_excluded_count,
            addRes = R.string.frequent_excluded_add,
        )
    }
}

/** Picking an icon for an app or a folder: from an icon pack, from the gallery, or back to the default. */
internal fun NavGraphBuilder.iconPickerDestination(
    app: NikeApp,
    nav: NavController,
    apps: AppsState,
    library: LibraryState,
    pickFromGallery: (String) -> Unit,
) {
    composable("iconpicker/{key}") { entry ->
        val scope = rememberCoroutineScope()
        val key = entry.arguments?.getString("key")?.let { Uri.decode(it) }.orEmpty()
        val folderId = folderIdFromToken(key)
        val label = when {
            folderId != null -> library.folders.firstOrNull { it.id == folderId }?.name.orEmpty()
            else -> library.nameOverrides[key] ?: apps.apps.find { it.key == key }?.label.orEmpty()
        }

        fun applyIcon(value: String?) {
            scope.launch {
                if (folderId != null) app.prefs.library.setFolderIcon(folderId, value)
                else app.prefs.library.setIconOverride(key, value)
            }
        }

        IconPickerScreen(
            appLabel = label,
            onPickPackIcon = { packPkg, drawableName ->
                applyIcon(encodePackOverride(packPkg, drawableName))
                nav.popBackStack()
            },
            onPickFromGallery = { pickFromGallery(key) },
            onResetIcon = {
                applyIcon(null)
                nav.popBackStack()
            },
            onBack = { nav.popBackStack() },
        )
    }
}
