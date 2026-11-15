// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import android.text.format.DateFormat
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.widget.clockAppLabel
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetConfig.HourFormat
import java.util.Date

private val chevron: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }

/** The first page: the four areas, then what else the widget can carry. */
@Composable
internal fun ColumnScope.HomePage(config: ClockWidgetConfig, update: ConfigUpdate, open: (ConfigPage) -> Unit) {
    SettingsGroup(
        listOf(
            settingsItem(stringResource(R.string.widget_settings_clock), { open(ConfigPage.CLOCK) }, Icons.Filled.Schedule, stringResource(config.style.label), trailing = chevron),
            settingsItem(stringResource(R.string.widget_settings_calendar), { open(ConfigPage.CALENDAR) }, Icons.Filled.CalendarMonth, stringResource(R.string.widget_settings_calendar_summary), trailing = chevron),
            settingsItem(
                stringResource(R.string.widget_settings_weather), { open(ConfigPage.WEATHER) }, Icons.Filled.WbSunny,
                stringResource(if (config.showWeather) R.string.widget_settings_weather_summary else R.string.widget_settings_disabled),
                trailing = chevron,
            ),
            settingsItem(stringResource(R.string.widget_settings_appearance), { open(ConfigPage.APPEARANCE) }, Icons.Filled.Palette, stringResource(R.string.widget_settings_appearance_summary), trailing = chevron),
        ),
    )
    SettingsSectionLabel(stringResource(R.string.widget_settings_more))
    SettingsGroup(
        listOf(
            settingsSwitch(stringResource(R.string.widget_settings_battery), config.showBattery, { on -> update { it.copy(showBattery = on) } }, Icons.Filled.BatteryStd),
            settingsSwitch(stringResource(R.string.widget_settings_alarm), config.showAlarm, { on -> update { it.copy(showAlarm = on) } }, Icons.Filled.Alarm),
            settingsSwitch(stringResource(R.string.widget_settings_notifications), config.showNotifications, { on -> update { it.copy(showNotifications = on) } }, Icons.Filled.Notifications),
        ),
    )
}

/** Style, how the hour is written, and which app the clock opens. */
@Composable
internal fun ColumnScope.ClockPage(config: ClockWidgetConfig, update: ConfigUpdate, openStyles: () -> Unit, open: (ClockConfigDialog) -> Unit) {
    SettingsGroup(
        listOf(
            settingsItem(stringResource(R.string.clock_style), openStyles, Icons.Filled.Style, stringResource(config.style.label), trailing = chevron),
            settingsBlock(stringResource(R.string.widget_settings_format), Icons.Filled.Schedule) {
                ChoiceChips(
                    options = HourFormat.entries,
                    selected = config.hourFormat,
                    label = {
                        stringResource(
                            when (it) {
                                HourFormat.SYSTEM -> R.string.widget_clock_hour_system
                                HourFormat.TWELVE -> R.string.widget_clock_hour_12
                                HourFormat.TWENTY_FOUR -> R.string.widget_clock_hour_24
                            },
                        )
                    },
                    onSelect = { choice -> update { it.copy(hourFormat = choice) } },
                )
            },
            settingsItem(stringResource(R.string.widget_settings_clock_app), { open(ClockConfigDialog.CLOCK_APP) }, Icons.Filled.Apps, clockAppLabel(config.clockPackage), trailing = chevron),
        ),
    )
}

/** The date, the upcoming events and the calendar app. */
@Composable
internal fun ColumnScope.CalendarPage(
    config: ClockWidgetConfig,
    lastDatePattern: String,
    calendarGranted: Boolean,
    update: ConfigUpdate,
    open: (ClockConfigDialog) -> Unit,
    openAgenda: () -> Unit,
    askCalendar: () -> Unit,
) {
    val now = Date()
    val custom = "\u0000custom"
    val dateOptions: List<String?> = ClockWidgetConfig.DATE_CHOICES + listOf(custom)
    val currentDate = config.datePattern?.let { if (it in ClockWidgetConfig.DATE_CHOICES) it else custom }
    SettingsSectionLabel(stringResource(R.string.widget_settings_display))
    SettingsGroup(
        buildList<SettingsRow> {
            add(settingsSwitch(stringResource(R.string.widget_settings_date), config.datePattern != null, { on -> update { it.copy(datePattern = if (on) lastDatePattern else null) } }, Icons.Filled.Today))
            if (config.datePattern != null) {
                add(
                    settingsBlock(stringResource(R.string.widget_settings_date_format)) {
                        ChoiceChips(
                            options = dateOptions,
                            selected = currentDate,
                            label = { pattern ->
                                if (pattern == custom) stringResource(R.string.widget_clock_date_custom_chip)
                                else runCatching { DateFormat.format(pattern, now).toString() }.getOrDefault(pattern.orEmpty())
                            },
                            onSelect = { pattern -> if (pattern == custom) open(ClockConfigDialog.CUSTOM_DATE) else update { it.copy(datePattern = pattern) } },
                        )
                    },
                )
            }
            add(
                settingsSwitch(stringResource(R.string.widget_settings_events), config.showAgenda, { on ->
                    if (on && !calendarGranted) askCalendar() else update { it.copy(showAgenda = on) }
                }, Icons.AutoMirrored.Filled.EventNote),
            )
        },
    )
    if (config.showAgenda) {
        SettingsGroup(
            buildList<SettingsRow> {
                if (calendarGranted) {
                    add(
                        settingsItem(
                            stringResource(R.string.clock_calendars), { open(ClockConfigDialog.CALENDARS) }, Icons.Filled.FilterList,
                            stringResource(if (config.excludedCalendars.isEmpty()) R.string.widget_settings_calendar_all else R.string.widget_settings_calendar_selected),
                            trailing = chevron,
                        ),
                    )
                }
                add(
                    settingsBlock(stringResource(R.string.widget_settings_time_labels)) {
                        ChoiceChips(
                            options = listOf(true, false),
                            selected = config.agendaRelative,
                            label = { stringResource(if (it) R.string.widget_settings_relative else R.string.widget_settings_absolute) },
                            onSelect = { relative -> update { it.copy(agendaRelative = relative) } },
                        )
                    },
                )
                add(
                    settingsBlock(stringResource(R.string.widget_settings_event_count)) {
                        ChoiceChips(
                            options = ClockWidgetConfig.AGENDA_COUNT_RANGE.toList(),
                            selected = config.agendaCount,
                            label = { it.toString() },
                            onSelect = { count -> update { it.copy(agendaCount = count) } },
                        )
                    },
                )
                add(
                    configNumber(
                        stringResource(R.string.widget_settings_lookahead), config.agendaLookaheadHours, ClockWidgetConfig.AGENDA_HOURS_RANGE,
                        stringResource(R.string.widget_settings_hours, config.agendaLookaheadHours),
                    ) { hours -> update { it.copy(agendaLookaheadHours = hours) } },
                )
            },
        )
    }
    SettingsGroup(
        listOf(
            settingsItem(stringResource(R.string.widget_settings_calendar_app), { open(ClockConfigDialog.CALENDAR_APP) }, Icons.Filled.Apps, clockAppLabel(config.calendarPackage), trailing = chevron),
            settingsItem(stringResource(R.string.widget_settings_agenda_open), openAgenda, Icons.AutoMirrored.Filled.OpenInNew),
        ),
    )
}
