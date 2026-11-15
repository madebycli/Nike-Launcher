// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.state

import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.folderIdFromToken
import dev.nikelauncher.ui.home.FavoriteEntry

/**
 * The rows of the favorites block.
 *
 * A favorites row is an app or a folder; both come out of the same ordered token list. Or out of
 * what has actually been opened, when the section is set to keep itself: the rows are the same
 * rows either way, so nothing downstream knows which of the two it was handed. Folders are not in
 * the computed list: a folder is something someone made, and there is no count of openings that
 * would put one together.
 */
fun buildFavoriteEntries(
    source: FavoritesSource,
    favoriteKeys: List<String>,
    appsByKey: Map<String, AppInfo>,
    foldersById: Map<String, Folder>,
    launchCounts: Map<String, Int>,
    frequentCount: Int,
    frequentExcluded: Set<String>,
): List<FavoriteEntry> {
    if (source == FavoritesSource.FREQUENT) {
        return launchCounts.asSequence()
            .filter { it.value > 0 }
            // Before take, so the next most-used app moves up into the slot rather than the row
            // simply going missing.
            .filter { it.key !in frequentExcluded }
            .mapNotNull { (key, count) -> appsByKey[key]?.let { it to count } }
            .sortedWith(compareByDescending<Pair<AppInfo, Int>> { it.second }.thenBy { it.first.label.lowercase() })
            .take(frequentCount)
            .map { FavoriteEntry.App(it.first) }
            .toList()
    }
    return favoriteKeys.mapNotNull { token ->
        val folderId = folderIdFromToken(token)
        if (folderId != null) foldersById[folderId]?.let { FavoriteEntry.FolderRef(it) }
        else appsByKey[token]?.let { FavoriteEntry.App(it) }
    }
}
