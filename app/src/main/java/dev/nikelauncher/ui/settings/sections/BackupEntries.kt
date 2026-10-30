// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.BACKUP

@Composable
internal fun backupEntries(env: SettingsEnv): List<SettingsEntry> = listOf(
    SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_export), env.actions.onExport, value = stringResource(R.string.settings_export_detail)), "backup save"),
    SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_import), env.actions.onImport, value = stringResource(R.string.settings_import_detail)), "backup restore"),
)
