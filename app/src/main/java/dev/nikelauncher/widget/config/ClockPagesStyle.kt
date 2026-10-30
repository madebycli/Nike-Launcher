// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.widget.clockAppLabel
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.widget.ClockStyle
import dev.nikelauncher.widget.ClockWidgetConfig

private val chevron: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }

/** Whether weather shows, in which unit, from which app, and whether any has arrived. */
@Composable
internal fun ColumnScope.WeatherPage(
    config: ClockWidgetConfig,
    weatherArrived: Boolean,
    update: ConfigUpdate,
    open: (ClockConfigDialog) -> Unit,
    openForecast: () -> Unit,
) {
    SettingsGroup(
        buildList<SettingsRow> {
            add(settingsSwitch(stringResource(R.string.widget_settings_weather_show), config.showWeather, { on -> update { it.copy(showWeather = on) } }, Icons.Filled.WbSunny))
            if (config.showWeather) {
                add(
                    settingsBlock(stringResource(R.string.widget_settings_unit), Icons.Filled.Thermostat) {
                        ChoiceChips(
                            options = listOf(false, true),
                            selected = config.weatherFahrenheit,
                            label = { stringResource(if (it) R.string.widget_clock_weather_fahrenheit else R.string.widget_clock_weather_celsius) },
                            onSelect = { fahrenheit -> update { it.copy(weatherFahrenheit = fahrenheit) } },
                        )
                    },
                )
                add(settingsItem(stringResource(R.string.widget_settings_weather_app), { open(ClockConfigDialog.WEATHER_APP) }, Icons.Filled.Apps,
                    config.weatherPackage?.let { clockAppLabel(it) } ?: stringResource(R.string.settings_quick_launch_none), trailing = chevron))
            }
        },
    )
    if (config.showWeather) {
        SettingsGroup(
            listOf(
                settingsItem(
                    stringResource(if (weatherArrived) R.string.widget_settings_weather_ready else R.string.widget_settings_weather_waiting),
                    {}, if (weatherArrived) Icons.Filled.CheckCircle else Icons.Filled.CloudQueue, enabled = false,
                ),
                settingsItem(stringResource(R.string.widget_settings_weather_open), openForecast, Icons.AutoMirrored.Filled.OpenInNew),
            ),
        )
    }
}

/** The text colour and, for each part that is showing, its size and opacity. */
@Composable
internal fun ColumnScope.AppearancePage(c: ClockWidgetConfig, update: ConfigUpdate, open: (ClockConfigDialog) -> Unit) {
    SettingsGroup(
        buildList<SettingsRow> {
            add(
                settingsItem(
                    stringResource(R.string.widget_settings_text_color), { open(ClockConfigDialog.TEXT_COLOR) }, Icons.Filled.Palette,
                    stringResource(R.string.widget_settings_colour_summary),
                    trailing = {
                        Surface(
                            shape = CircleShape,
                            color = Color(c.textColor),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(32.dp),
                        ) {}
                    },
                ),
            )
            if (c.textColor != Color.White.toArgb()) {
                add(settingsItem(stringResource(R.string.widget_clock_color_reset), { update { it.copy(textColor = Color.White.toArgb()) } }, Icons.Filled.RestartAlt))
            }
        },
    )
    val sizeTitle = stringResource(R.string.widget_settings_text_size)
    val opacityTitle = stringResource(R.string.widget_settings_opacity)
    @Composable
    fun part(title: String, size: Int, opacity: Int, range: IntRange, onSize: (Int) -> Unit, onOpacity: (Int) -> Unit) {
        SettingsSectionLabel(title)
        SettingsGroup(
            listOf(
                configNumber(sizeTitle, size, range, onChange = onSize),
                configNumber(opacityTitle, opacity, ClockWidgetConfig.OPACITY_RANGE, stringResource(R.string.widget_clock_battery_percent, opacity), onOpacity),
            ),
        )
    }
    if (c.style != ClockStyle.NONE) {
        part(stringResource(R.string.widget_settings_clock), c.timeSizeSp, c.timeOpacity, ClockWidgetConfig.SIZE_RANGE,
            { v -> update { it.copy(timeSizeSp = v) } }, { v -> update { it.copy(timeOpacity = v) } })
        SettingsSectionLabel(stringResource(R.string.widget_settings_spacing))
        SettingsGroup(listOf(configNumber(stringResource(R.string.widget_clock_data_gap), c.clockDataGapDp, ClockWidgetConfig.CLOCK_DATA_GAP_RANGE, stringResource(R.string.edit_value_dp, c.clockDataGapDp)) { v -> update { it.copy(clockDataGapDp = v) } }))
    }
    SettingsSectionLabel(stringResource(R.string.widget_text_width))
    SettingsGroup(listOf(configNumber(stringResource(R.string.widget_text_width), c.textWidthPercent, ClockWidgetConfig.TEXT_WIDTH_RANGE, stringResource(R.string.widget_clock_battery_percent, c.textWidthPercent)) { v -> update { it.copy(textWidthPercent = v) } }))
    if (c.datePattern != null) part(stringResource(R.string.widget_settings_date_heading), c.dateSizeSp, c.dateOpacity, ClockWidgetConfig.DATE_SIZE_RANGE,
        { v -> update { it.copy(dateSizeSp = v) } }, { v -> update { it.copy(dateOpacity = v) } })
    if (c.showAgenda || c.showAlarm) part(stringResource(R.string.widget_settings_calendar), c.agendaSizeSp, c.agendaOpacity, ClockWidgetConfig.DATE_SIZE_RANGE,
        { v -> update { it.copy(agendaSizeSp = v) } }, { v -> update { it.copy(agendaOpacity = v) } })
    if (c.showBattery) part(stringResource(R.string.widget_settings_battery), c.batterySizeSp, c.batteryOpacity, ClockWidgetConfig.DATE_SIZE_RANGE,
        { v -> update { it.copy(batterySizeSp = v) } }, { v -> update { it.copy(batteryOpacity = v) } })
    if (c.showWeather) part(stringResource(R.string.widget_settings_weather), c.weatherSizeSp, c.weatherOpacity, ClockWidgetConfig.DATE_SIZE_RANGE,
        { v -> update { it.copy(weatherSizeSp = v) } }, { v -> update { it.copy(weatherOpacity = v) } })
    if (c.showNotifications) part(stringResource(R.string.widget_settings_notifications), c.notificationsSizeSp, c.notificationsOpacity, ClockWidgetConfig.DATE_SIZE_RANGE,
        { v -> update { it.copy(notificationsSizeSp = v) } }, { v -> update { it.copy(notificationsOpacity = v) } })
}
