// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import android.appwidget.AppWidgetManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.widget.clockAppLabel
import dev.nikelauncher.data.prefs.WidgetApps
import dev.nikelauncher.data.prefs.WidgetAppSlot
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.widget.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The same app pickers used by the widget, backed by defaults included in the settings backup. */
@Composable
internal fun DefaultAppsChoice() {
    val context = LocalContext.current
    val app = context.applicationContext as NikeApp
    val defaults by app.prefs.widgetApps.choices.collectAsState(WidgetApps())
    val scope = rememberCoroutineScope()
    var chooser by remember { mutableStateOf<ClockAppKind?>(null) }
    SettingsGroup(listOf(
        settingsItem(stringResource(R.string.widget_settings_clock_app), { chooser = ClockAppKind.CLOCK }, Icons.Filled.Apps, clockAppLabel(defaults.clock)),
        settingsItem(stringResource(R.string.widget_settings_calendar_app), { chooser = ClockAppKind.CALENDAR }, Icons.Filled.Apps, clockAppLabel(defaults.calendar)),
        settingsItem(stringResource(R.string.widget_settings_weather_app), { chooser = ClockAppKind.WEATHER }, Icons.Filled.Apps,
            defaults.weather?.let { clockAppLabel(it) } ?: stringResource(R.string.widget_settings_no_app)),
    ))
    chooser?.let { kind ->
        val selected = when (kind) { ClockAppKind.CLOCK -> defaults.clock; ClockAppKind.CALENDAR -> defaults.calendar; ClockAppKind.WEATHER -> defaults.weather }
        ClockAppPickerDialog(kind, selected, onPick = { pkg ->
            chooser = null
            scope.launch {
                val slot = when (kind) { ClockAppKind.CLOCK -> WidgetAppSlot.CLOCK; ClockAppKind.CALENDAR -> WidgetAppSlot.CALENDAR; ClockAppKind.WEATHER -> WidgetAppSlot.WEATHER }
                app.prefs.widgetApps.set(slot, pkg)
                // The first-run clock may already have been bound after the home role was granted.
                val manager = AppWidgetManager.getInstance(context)
                app.prefs.home.widgetIds.first().filter { manager.getAppWidgetInfo(it)?.provider == ClockWidgetProvider.componentName(context) }.forEach { id ->
                    val config = ClockWidgetConfig.read(context, id)
                    val changed = when (kind) { ClockAppKind.CLOCK -> config.copy(clockPackage = pkg); ClockAppKind.CALENDAR -> config.copy(calendarPackage = pkg); ClockAppKind.WEATHER -> config.copy(weatherPackage = pkg) }
                    ClockWidgetConfig.write(context, id, changed)
                    ClockWidgetProvider.render(context, manager, id)
                }
            }
        }, onDismiss = { chooser = null })
    }
}
