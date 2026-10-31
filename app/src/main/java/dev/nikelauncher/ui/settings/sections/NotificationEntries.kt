// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.NOTIFICATIONS

@Composable
internal fun notificationEntries(env: SettingsEnv): List<SettingsEntry> {
    val list = env.prefs.appList
    val home = env.prefs.home
    val badges by list.notificationBadges.collectAsState(false)
    val previews by list.notificationPreviews.collectAsState(true)
    val nowPlaying by home.nowPlayingEnabled.collectAsState(false)

    return listOf(
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_notification_badges), badges, { env.write { list.setNotificationBadges(it) } }, value = stringResource(R.string.settings_notification_badges_detail), icon = Icons.Filled.Notifications), "count badge icons"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_notification_previews), previews, { env.write { list.setNotificationPreviews(it) } }, icon = Icons.Filled.Notifications), "text message preview home app list"),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(if (env.listenerEnabled) R.string.settings_notification_access_granted else R.string.settings_notification_access_missing),
                onClick = env.actions.onOpenNotificationSettings,
                trailing = { Button(onClick = env.actions.onOpenNotificationSettings) { Text(stringResource(R.string.settings_open_settings)) } }, icon = Icons.Filled.Notifications,
            ),
            "permission access",
            visible = nowPlaying || badges || previews,
        ),
    )
}
