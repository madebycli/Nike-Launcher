// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsScaffold
import dev.nikelauncher.widget.ClockWidgetAgenda
import dev.nikelauncher.widget.ClockWidgetConfig

/**
 * Every clock option as rows: what is shown, then how each part is drawn. [onSave] and [onCancel]
 * end the activity; nothing is written until save.
 */
@Composable
fun ClockConfigScreen(
    initial: ClockWidgetConfig,
    onSave: (ClockWidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(initial) }
    var dialog by remember { mutableStateOf<ClockConfigDialog?>(null) }
    // Asked for here rather than at install, and re-read on the way back so the section stops
    // offering what has just been granted.
    var calendarGranted by remember { mutableStateOf(ClockWidgetAgenda.hasCalendarPermission(context)) }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarGranted = granted
        // Refused, so nothing would ever appear; better to leave the row off than to show a
        // widget that silently has nothing in it.
        if (!granted) config = config.copy(showAgenda = false)
    }

    val update = { change: (ClockWidgetConfig) -> ClockWidgetConfig -> config = change(config) }

    SettingsScaffold(title = stringResource(R.string.widget_clock_label), onBack = onCancel) {
        item { SettingsGroup(displayRows(config, update) { dialog = it }) }
        item {
            SettingsGroup(
                contentRows(config, calendarGranted, update, { dialog = it }) {
                    askCalendar.launch(Manifest.permission.READ_CALENDAR)
                },
            )
        }
        item { SettingsGroup(textStyleRows(context, config, update)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = { onSave(config) }) { Text(stringResource(R.string.action_save)) }
            }
        }
    }

    ClockConfigDialogs(dialog, config, update) { dialog = null }
}
