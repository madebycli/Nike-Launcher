// SPDX-License-Identifier: GPL-3.0-or-later
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
import androidx.compose.ui.graphics.vector.ImageVector
import dev.nikelauncher.R

/**
 * The settings screen shows one of these at a time; the first screen is the list of them.
 *
 * Grouped by the surface each option acts on rather than by what kind of option it is, so finding
 * a setting does not take knowing how it is implemented.
 */
enum class SettingsSection(@StringRes val labelRes: Int, val icon: ImageVector) {
    APPEARANCE(R.string.settings_section_appearance, Icons.Filled.Palette),
    HOME(R.string.settings_section_home, Icons.Filled.Home),
    APP_LIST(R.string.settings_section_app_list, Icons.Filled.ViewList),
    STRIP(R.string.settings_section_strip, Icons.Filled.SortByAlpha),
    GESTURES(R.string.settings_section_gestures, Icons.Filled.Swipe),
    NOTIFICATIONS(R.string.settings_section_notifications, Icons.Filled.Notifications),
    APPS(R.string.settings_section_apps, Icons.Filled.Apps),
    BACKUP(R.string.settings_section_backup, Icons.Filled.Backup),
    ABOUT(R.string.settings_section_about, Icons.Filled.Info),
}
