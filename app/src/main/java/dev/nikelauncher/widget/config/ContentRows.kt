// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.widget.ClockWidgetConfig

/**
 * The rows that bring other information to the clock: weather, notification count, agenda, alarm.
 *
 * The calendar permission is asked for from here, at the moment the agenda is switched on, so
 * nothing is demanded of someone who only wanted a clock.
 */
@Composable
internal fun contentRows(
    config: ClockWidgetConfig,
    calendarGranted: Boolean,
    update: ConfigUpdate,
    open: (ClockConfigDialog) -> Unit,
    askCalendar: () -> Unit,
): List<SettingsRow> = buildList {
    add(settingsSwitch(stringResource(R.string.widget_clock_weather), config.showWeather) { on -> update { it.copy(showWeather = on) } })
    if (config.showWeather) {
        add(
            settingsBlock(stringResource(R.string.widget_clock_weather_unit)) {
                ChoiceChips(
                    options = listOf(false, true),
                    selected = config.weatherFahrenheit,
                    label = { stringResource(if (it) R.string.widget_clock_weather_fahrenheit else R.string.widget_clock_weather_celsius) },
                    onSelect = { fahrenheit -> update { it.copy(weatherFahrenheit = fahrenheit) } },
                )
            },
        )
        add(settingsItem(stringResource(R.string.widget_clock_weather_opens), { open(ClockConfigDialog.WEATHER_APP) }))
    }
    add(settingsSwitch(stringResource(R.string.widget_clock_notifications), config.showNotifications) { on -> update { it.copy(showNotifications = on) } })
    add(
        settingsSwitch(stringResource(R.string.widget_clock_agenda), config.showAgenda) { on ->
            if (on && !calendarGranted) askCalendar() else update { it.copy(showAgenda = on) }
        },
    )
    if (config.showAgenda) {
        add(
            settingsBlock(stringResource(R.string.widget_clock_agenda_style)) {
                ChoiceChips(
                    options = listOf(true, false),
                    selected = config.agendaRelative,
                    label = { stringResource(if (it) R.string.widget_clock_agenda_relative else R.string.widget_clock_agenda_absolute) },
                    onSelect = { relative -> update { it.copy(agendaRelative = relative) } },
                )
            },
        )
        add(
            settingsBlock(stringResource(R.string.widget_clock_agenda_count), value = config.agendaCount.toString()) {
                val range = ClockWidgetConfig.AGENDA_COUNT_RANGE
                SettingsSlider(config.agendaCount.toFloat(), range.first.toFloat()..range.last.toFloat(), { value -> update { it.copy(agendaCount = value.toInt()) } })
            },
        )
        add(
            settingsBlock(stringResource(R.string.widget_clock_agenda_hours), value = config.agendaLookaheadHours.toString()) {
                val range = ClockWidgetConfig.AGENDA_HOURS_RANGE
                SettingsSlider(config.agendaLookaheadHours.toFloat(), range.first.toFloat()..range.last.toFloat(), { value -> update { it.copy(agendaLookaheadHours = value.toInt()) } })
            },
        )
    }
    add(settingsSwitch(stringResource(R.string.widget_clock_alarm_show), config.showAlarm) { on -> update { it.copy(showAlarm = on) } })
}
