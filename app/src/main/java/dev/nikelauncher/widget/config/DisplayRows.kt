// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetConfig.HourFormat
import java.util.Date

internal typealias ConfigUpdate = ((ClockWidgetConfig) -> ClockWidgetConfig) -> Unit

/** How the time and date are written, whether the battery shows, and the colour of it all. */
@Composable
internal fun displayRows(
    config: ClockWidgetConfig,
    update: ConfigUpdate,
    open: (ClockConfigDialog) -> Unit,
): List<SettingsRow> {
    val now = Date()
    // A choice is labelled with today's date in that shape, since a pattern like "EEE, d MMM"
    // tells nobody what they are picking.
    val dateChoices: List<String?> = ClockWidgetConfig.DATE_CHOICES + listOf(CUSTOM, null)
    val currentDate: String? = config.datePattern?.takeIf { it in ClockWidgetConfig.DATE_CHOICES }
        ?: config.datePattern?.let { CUSTOM }
    return listOf(
        settingsBlock(stringResource(R.string.widget_clock_hour)) {
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
        settingsBlock(stringResource(R.string.widget_clock_date_format)) {
            ChoiceChips(
                options = dateChoices,
                selected = currentDate,
                label = { pattern ->
                    when (pattern) {
                        null -> stringResource(R.string.widget_clock_date_none)
                        CUSTOM -> stringResource(R.string.widget_clock_date_custom_chip)
                        else -> runCatching { DateFormat.format(pattern, now).toString() }.getOrDefault(pattern)
                    }
                },
                onSelect = { pattern ->
                    if (pattern == CUSTOM) open(ClockConfigDialog.CUSTOM_DATE) else update { it.copy(datePattern = pattern) }
                },
            )
        },
        settingsSwitch(stringResource(R.string.widget_clock_battery), config.showBattery) { on ->
            update { it.copy(showBattery = on) }
        },
        settingsItem(
            title = stringResource(R.string.widget_clock_color),
            onClick = { open(ClockConfigDialog.TEXT_COLOR) },
            trailing = {
                Surface(
                    shape = CircleShape,
                    color = Color(config.textColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(32.dp),
                ) {}
            },
        ),
    ) + listOfNotNull(
        // White is where the clock starts, and a colour picked by eye is hard to get back to.
        if (config.textColor != Color.White.toArgb()) settingsItem(
            title = stringResource(R.string.widget_clock_color_reset),
            onClick = { update { it.copy(textColor = Color.White.toArgb()) } },
            icon = Icons.Filled.RestartAlt,
        ) else null,
    )
}

/** Stands for "a pattern typed by hand" among the date choices; never a real pattern. */
private const val CUSTOM = "\u0000custom"
