// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.BlockOffsets
import dev.nikelauncher.data.HomePins
import dev.nikelauncher.data.Prefs

/** What the user has arranged: favorites, folders, names and the blocks' places. */
@Immutable
class LibraryState(
    val hiddenApps: Set<String>,
    val favoriteKeys: List<String>,
    val frequentExcluded: Set<String>,
    val favoritesEverSet: Boolean,
    val folders: List<Folder>,
    val nameOverrides: Map<String, String>,
    val launchCounts: Map<String, Int>,
    val favoritesSource: FavoritesSource,
    val frequentCount: Int,
    val cornerButtonKey: String?,
    val quickLaunchLeftKey: String?,
    val quickLaunchRightKey: String?,
    val widgetIds: List<Int>,
    val widgetHeightDp: Int,
    val pins: HomePins,
    val blockOffsets: BlockOffsets,
    val scrubBand: Pair<Float, Float>?,
    val layoutDefaultsVersion: Int?,
    val welcomeSeen: Boolean,
)

/**
 * The stored arrangement, collected once and shared with every destination, so the DataStore is
 * read once per key instead of once per consumer. Starts at values that show nothing, so no screen
 * flashes before the stored answer arrives.
 */
@Composable
fun rememberLibraryState(prefs: Prefs): LibraryState {
    val library = prefs.library
    val home = prefs.home
    val hiddenApps by library.hiddenApps.collectAsState(emptySet())
    val favoriteKeys by library.favorites.collectAsState(emptyList())
    val frequentExcluded by library.frequentExcluded.collectAsState(emptySet())
    val favoritesEverSet by library.favoritesEverSet.collectAsState(true)
    val folders by library.folders.collectAsState(emptyList())
    val nameOverrides by library.nameOverrides.collectAsState(emptyMap())
    val launchCounts by library.launchCounts.collectAsState(emptyMap())
    val favoritesSource by library.favoritesSource.collectAsState(FavoritesSource.MANUAL)
    val frequentCount by library.frequentCount.collectAsState(6)
    val cornerButtonKey by library.cornerButtonKey.collectAsState(null)
    val quickLeft by library.quickLaunchLeft.collectAsState(null)
    val quickRight by library.quickLaunchRight.collectAsState(null)
    val widgetIds by home.widgetIds.collectAsState(emptyList())
    val widgetHeightDp by home.widgetHeightDp.collectAsState(180)
    val pins by home.homePins.collectAsState(HomePins.Default)
    val blockOffsets by home.blockOffsets.collectAsState(BlockOffsets())
    val scrubBand by prefs.strip.scrubBand.collectAsState(null)
    val layoutDefaultsVersion by home.layoutDefaultsVersion.collectAsState(null)
    val welcomeSeen by home.welcomeSeen.collectAsState(true)
    return LibraryState(
        hiddenApps, favoriteKeys, frequentExcluded, favoritesEverSet, folders, nameOverrides, launchCounts,
        favoritesSource, frequentCount, cornerButtonKey, quickLeft, quickRight, widgetIds, widgetHeightDp,
        pins, blockOffsets, scrubBand, layoutDefaultsVersion, welcomeSeen,
    )
}
