// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shortcut
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction

@get:StringRes
internal val CornerAction.label: Int
    get() = when (this) {
        CornerAction.MENU -> R.string.corner_action_menu
        CornerAction.SEARCH -> R.string.applist_search
        CornerAction.APP -> R.string.corner_action_app
        CornerAction.SHORTCUTS -> R.string.corner_action_shortcuts
        CornerAction.CALENDAR -> R.string.widget_settings_calendar
        CornerAction.WEATHER -> R.string.widget_settings_weather
        CornerAction.NOTIFICATIONS -> R.string.corner_action_notifications
        CornerAction.SETTINGS -> R.string.action_open_settings
        CornerAction.ASSISTANT -> R.string.corner_action_assistant
        CornerAction.SHADE -> R.string.corner_action_shade
        CornerAction.LOCK -> R.string.corner_action_lock
    }

internal val CornerAction.icon: ImageVector
    get() = when (this) {
        CornerAction.MENU -> Icons.Outlined.Apps
        CornerAction.SEARCH -> Icons.Outlined.Search
        CornerAction.APP -> Icons.AutoMirrored.Outlined.OpenInNew
        CornerAction.SHORTCUTS -> Icons.Outlined.Shortcut
        CornerAction.CALENDAR -> Icons.Outlined.CalendarMonth
        CornerAction.WEATHER -> Icons.Outlined.WbSunny
        CornerAction.NOTIFICATIONS -> Icons.Outlined.NotificationsNone
        CornerAction.SETTINGS -> Icons.Outlined.Settings
        CornerAction.ASSISTANT -> Icons.Outlined.MicNone
        CornerAction.SHADE -> Icons.Outlined.ExpandMore
        CornerAction.LOCK -> Icons.Outlined.Lock
    }
