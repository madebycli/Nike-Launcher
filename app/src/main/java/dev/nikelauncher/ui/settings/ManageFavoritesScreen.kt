// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.nikelauncher.ui.kit.search.SearchField
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.PrivateSpace
import dev.nikelauncher.data.concealsStored
import dev.nikelauncher.data.folderIdFromToken
import dev.nikelauncher.data.restoreConcealed
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.settings.favorites.SelectedFavorites
import androidx.compose.foundation.layout.height
import dev.nikelauncher.R
import androidx.compose.ui.res.stringResource

/**
 * Pick favorites from the full app list with checkboxes, rather than relying on a
 * long-press in the A-Z list that nothing advertises.
 *
 * The ones already chosen are listed first, in the order they sit on the home screen, and can
 * be dragged into a different one — the same shape as a folder's members, and for the same
 * reason: that order is not alphabetical and cannot be edited in a list that is.
 */
@Composable
fun ManageFavoritesScreen(
    allApps: List<AppInfo>,
    favoriteKeys: List<String>,
    folders: List<Folder>,
    privateSpace: PrivateSpace,
    nameOverrides: Map<String, String>,
    iconSizeDp: Int,
    onSetFavorite: (AppInfo, Boolean) -> Unit,
    onForget: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    Scaffold(
        containerColor = surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface),
            )
        },
    ) { padding ->
        FavoritesChooser(
            allApps, favoriteKeys, folders, privateSpace, nameOverrides, iconSizeDp,
            onSetFavorite, onForget, onReorder, Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/** The ticked apps in their home order, then a search over the rest. Shared with the first-run setup. */
@Composable
internal fun FavoritesChooser(
    allApps: List<AppInfo>,
    favoriteKeys: List<String>,
    folders: List<Folder>,
    /** So a favorite that lives in a locked private space can be left out of this screen. */
    privateSpace: PrivateSpace,
    nameOverrides: Map<String, String>,
    iconSizeDp: Int,
    onSetFavorite: (AppInfo, Boolean) -> Unit,
    /**
     * Unticks a key nothing can resolve right now; see the row that offers it. Only ever drops
     * the favorite, not the row's rename/icon/folder/hidden/launch-count — a locked private
     * space or a paused work profile can make a real app or shortcut "missing" temporarily,
     * and those come back.
     */
    onForget: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites = favoriteKeys.toSet()
    val appsByKey = remember(allApps) { allApps.associateBy { it.key } }
    val foldersById = remember(folders) { folders.associateBy { it.id } }
    // A favorite inside a locked private space is left out entirely, rather than shown as the
    // row that says an app is missing: that row would still say how many are in there. The
    // stored keys are untouched, so those favorites come back when the space is opened. When
    // the space itself could not be read, the same goes for any favorite that names nothing,
    // since there is then no serial to tell a private one from an uninstalled one.
    //
    // One predicate, and the reorder below puts rows back with this same one. Hiding by a wide
    // rule and restoring by a narrower one leaves the keys between the two in neither list,
    // and the restore reads a list that is shorter than it expected as rows the user deleted —
    // so the first drag would write that deletion to the store.
    val conceal: (String) -> Boolean = remember(privateSpace, appsByKey, foldersById) {
        { key ->
            val resolves = key in appsByKey || folderIdFromToken(key)?.let { it in foldersById } == true
            privateSpace.concealsStored(key, resolves)
        }
    }
    val shownKeys = remember(favoriteKeys, conceal) { favoriteKeys.filterNot(conceal) }
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = remember(allApps, favorites, query, nameOverrides) {
        allApps.filter { app ->
            app.key !in favorites &&
                (query.isBlank() || (nameOverrides[app.key] ?: app.label).contains(query.trim(), ignoreCase = true))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier,
    ) {
        item {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.applist_search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (query.isBlank() && shownKeys.isNotEmpty()) {
            item {
                SelectedFavorites(shownKeys, appsByKey, foldersById, nameOverrides, iconSizeDp,
                    onRemove = { key -> appsByKey[key]?.let { onSetFavorite(it, false) } ?: onForget(key) },
                    onReorder = { order -> onReorder(restoreConcealed(favoriteKeys, order, conceal)) })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        itemsIndexed(candidates, key = { _, app -> app.key }) { index, app ->
            SegmentedListItem(
                onClick = { onSetFavorite(app, true) },
                shapes = ListItemDefaults.segmentedShapes(index, candidates.size),
                colors = ListItemDefaults.segmentedColors(),
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = ListItemDefaults.SegmentedGap),
                leadingContent = { AppIcon(app = app, sizeDp = minOf(iconSizeDp, 44)) },
                trailingContent = {
                    FilledTonalIconButton(onClick = { onSetFavorite(app, true) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.favorites_add))
                    }
                },
                content = { Text(nameOverrides[app.key] ?: app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }

}
