// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nikelauncher.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.orion.OrionAccent

/**
 * The settings screen shows one of these at a time; the first screen is a tile for each.
 *
 * Grouped by the surface each option acts on rather than by what kind of option it is, so finding
 * a setting does not take knowing how it is implemented. Each has its own hue and shape, shared by
 * its tile and the hero of its page.
 */
enum class SettingsSection(
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
    val accent: Color,
    private val polygon: () -> RoundedPolygon,
) {
    APPEARANCE(R.string.settings_section_appearance, R.string.settings_section_appearance_desc, Icons.Filled.Palette, OrionAccent.Purple, { MaterialShapes.Cookie9Sided }),
    HOME(R.string.settings_section_home, R.string.settings_section_home_desc, Icons.Filled.Home, OrionAccent.Blue, { MaterialShapes.Clover4Leaf }),
    APP_LIST(R.string.settings_section_app_list, R.string.settings_section_app_list_desc, Icons.Filled.ViewList, OrionAccent.Teal, { MaterialShapes.Cookie6Sided }),
    STRIP(R.string.settings_section_strip, R.string.settings_section_strip_desc, Icons.Filled.SortByAlpha, OrionAccent.Orange, { MaterialShapes.Sunny }),
    GESTURES(R.string.settings_section_gestures, R.string.settings_section_gestures_desc, Icons.Filled.Swipe, OrionAccent.Pink, { MaterialShapes.Flower }),
    NOTIFICATIONS(R.string.settings_section_notifications, R.string.settings_section_notifications_desc, Icons.Filled.Notifications, OrionAccent.Yellow, { MaterialShapes.SoftBurst }),
    APPS(R.string.settings_section_apps, R.string.settings_section_apps_desc, Icons.Filled.Apps, OrionAccent.Green, { MaterialShapes.Cookie12Sided }),
    BACKUP(R.string.settings_section_backup, R.string.settings_section_backup_desc, Icons.Filled.Backup, OrionAccent.Indigo, { MaterialShapes.Puffy }),
    ABOUT(R.string.settings_section_about, R.string.settings_section_about_desc, Icons.Filled.Info, OrionAccent.Cyan, { MaterialShapes.Clover8Leaf }),
    ;

    val shape: RoundedPolygon get() = polygon()
}
