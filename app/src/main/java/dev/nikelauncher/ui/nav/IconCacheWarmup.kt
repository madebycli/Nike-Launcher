// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.nav

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.ui.common.IconStyle
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.iconCacheGeneration
import dev.nikelauncher.ui.common.warmIconCache

/**
 * Rasterises icons in the background so opening the A-Z list does not have to. Favorites and folder
 * members go first: they are what the home screen needs before anything else.
 */
@Composable
fun WarmIconCache(
    apps: List<AppInfo>,
    favoriteKeys: List<String>,
    folders: List<Folder>,
    iconPackPackage: String?,
    iconOverrides: Map<String, String>,
    iconSizeDp: Int,
) {
    val context = LocalContext.current
    val listIconPx = with(LocalDensity.current) { iconSizeDp.dp.roundToPx() }
    val priorityKeys = remember(favoriteKeys, folders) { favoriteKeys.toSet() + folders.flatMap { it.apps } }
    // Same style the rows will ask for, or the warm pass fills the cache under keys nothing then
    // looks up, and every icon is rasterised twice.
    val iconConfig = LocalIconConfig.current
    val scheme = MaterialTheme.colorScheme
    val style = remember(iconConfig, scheme) {
        IconStyle(
            shape = iconConfig.shape,
            themed = iconConfig.themed,
            background = scheme.primaryContainer.toArgb(),
            foreground = scheme.onPrimaryContainer.toArgb(),
        )
    }
    val generation by iconCacheGeneration.collectAsState()
    LaunchedEffect(apps, iconPackPackage, iconOverrides, listIconPx, priorityKeys, style, generation) {
        warmIconCache(context, apps, iconPackPackage, iconOverrides, listIconPx, style, priorityKeys)
    }
}
