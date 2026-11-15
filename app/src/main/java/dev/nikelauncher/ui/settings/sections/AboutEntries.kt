// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.BuildConfig
import dev.nikelauncher.R
import dev.nikelauncher.data.CrashLog
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.ABOUT

@Composable
internal fun aboutEntries(@Suppress("UNUSED_PARAMETER") env: SettingsEnv): List<SettingsEntry> {
    val context = LocalContext.current
    // Only after a crash, so there is nothing here the rest of the time. A launcher draws other
    // apps' widgets with their own code, so the trace is often the only thing that says whose
    // crash it was.
    val crash = remember { CrashLog.read(context) }
    var crashCleared by remember { mutableStateOf(false) }
    val appName = stringResource(R.string.app_name)

    return listOf(
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.app_name),
                onClick = {},
                value = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), icon = Icons.Filled.Info,
            ),
        ),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.settings_crash_copy),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(appName, crash))
                    context.toast(R.string.settings_crash_copied)
                    CrashLog.clear(context)
                    crashCleared = true
                },
                value = stringResource(R.string.settings_crash_detail), icon = Icons.Filled.BugReport,
            ),
            visible = crash != null && !crashCleared,
        ),
    )
}
