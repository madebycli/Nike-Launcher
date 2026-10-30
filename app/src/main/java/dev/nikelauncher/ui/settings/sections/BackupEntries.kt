// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.widget.WidgetRestoreActivity

private val SECTION = SettingsSection.BACKUP

@Composable
internal fun backupEntries(env: SettingsEnv): List<SettingsEntry> {
    val context = LocalContext.current
    val pending by env.prefs.pendingWidgetRestores.collectAsState(emptyList())
    val widgetsLeft = stringResource(R.string.backup_widgets_remaining, pending.size)
    return listOfNotNull(
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_export), env.actions.onExport, value = stringResource(R.string.settings_export_detail), icon = Icons.Filled.Upload), "backup save"),
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_import), env.actions.onImport, value = stringResource(R.string.settings_import_detail), icon = Icons.Filled.Download), "backup restore"),
        // Only while a restore has widgets it could not bind by itself.
        if (pending.isEmpty()) null else SettingsEntry(
            SECTION,
            settingsItem(stringResource(R.string.backup_widgets_title), { context.startActivity(Intent(context, WidgetRestoreActivity::class.java)) }, value = widgetsLeft, icon = Icons.Filled.Widgets),
            "backup widgets restore",
        ),
    )
}
