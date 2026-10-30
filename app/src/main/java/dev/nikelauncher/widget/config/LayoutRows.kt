// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.widget.ClockWidgetConfig

/** The face of the clock, how its parts are spaced, and which apps and calendars it answers to. */
@Composable
internal fun layoutRows(
    config: ClockWidgetConfig,
    calendarGranted: Boolean,
    update: ConfigUpdate,
    open: (ClockConfigDialog) -> Unit,
): List<SettingsRow> {
    val styleTitle = stringResource(R.string.widget_settings_style_type)
    val gapTitle = stringResource(R.string.widget_clock_data_gap)
    val widthTitle = stringResource(R.string.widget_text_width)
    val clockApp = stringResource(R.string.widget_settings_clock_app)
    val calendarApp = stringResource(R.string.widget_settings_calendar_app)
    val calendars = stringResource(R.string.clock_calendars)
    return listOfNotNull(
        settingsItem(styleTitle, { open(ClockConfigDialog.STYLE) }, value = stringResource(config.style.label)),
        settingsBlock(gapTitle, value = "${config.clockDataGapDp} dp") {
            SettingsSlider(
                config.clockDataGapDp.toFloat(),
                ClockWidgetConfig.CLOCK_DATA_GAP_RANGE.first.toFloat()..ClockWidgetConfig.CLOCK_DATA_GAP_RANGE.last.toFloat(),
                { v -> update { it.copy(clockDataGapDp = v.toInt()) } },
            )
        },
        settingsBlock(widthTitle, value = "${config.textWidthPercent}%") {
            SettingsSlider(
                config.textWidthPercent.toFloat(),
                ClockWidgetConfig.TEXT_WIDTH_RANGE.first.toFloat()..ClockWidgetConfig.TEXT_WIDTH_RANGE.last.toFloat(),
                { v -> update { it.copy(textWidthPercent = v.toInt()) } },
            )
        },
        settingsItem(clockApp, { open(ClockConfigDialog.CLOCK_APP) }, value = config.clockPackage),
        settingsItem(calendarApp, { open(ClockConfigDialog.CALENDAR_APP) }, value = config.calendarPackage),
        // Only once calendar access exists: before that there is nothing to choose among.
        if (calendarGranted) settingsItem(calendars, { open(ClockConfigDialog.CALENDARS) }) else null,
    )
}
