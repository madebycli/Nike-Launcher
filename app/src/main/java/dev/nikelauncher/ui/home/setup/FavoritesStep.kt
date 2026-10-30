// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.ui.apps.AppsState
import dev.nikelauncher.ui.kit.onboarding.SetupHeadline
import dev.nikelauncher.ui.settings.FavoritesChooser
import dev.nikelauncher.ui.state.LibraryState
import kotlinx.coroutines.launch

/** Pick the favorites: search, add, and reorder the chosen apps. */
@Composable
internal fun FavoritesStep(app: NikeApp, apps: AppsState, library: LibraryState, iconSizeDp: Int) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        SetupHeadline(stringResource(R.string.favorites_title), Modifier.padding(top = 20.dp, bottom = 8.dp))
        FavoritesChooser(
            allApps = apps.apps,
            favoriteKeys = library.favoriteKeys,
            folders = library.folders,
            privateSpace = apps.privateSpace,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onSetFavorite = { appInfo, add ->
                scope.launch { if (add) app.prefs.library.addFavorite(appInfo.key) else app.prefs.library.removeFavorite(appInfo.key) }
            },
            onForget = { key -> scope.launch { app.prefs.library.removeFavorite(key) } },
            onReorder = { keys -> scope.launch { app.prefs.library.setFavorites(keys) } },
            modifier = Modifier.weight(1f),
        )
    }
}

