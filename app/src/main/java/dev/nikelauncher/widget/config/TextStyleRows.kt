// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.widget.ClockWidgetAgenda
import dev.nikelauncher.widget.ClockWidgetBattery
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetWeather
import java.util.Date
import kotlin.math.roundToInt

/**
 * Size and opacity for each part that is showing, each with a preview of what the two numbers
 * make, since there is otherwise no way to tell what they mean.
 */
@Composable
internal fun textStyleRows(context: Context, config: ClockWidgetConfig, update: ConfigUpdate): List<SettingsRow> = buildList {
    val color = config.textColor
    addAll(
        textStyle(
            stringResource(R.string.widget_clock_size), stringResource(R.string.widget_clock_time_opacity),
            preview = stringResource(R.string.widget_clock_size_preview),
            size = config.timeSizeSp, sizeRange = ClockWidgetConfig.SIZE_RANGE, opacity = config.timeOpacity, color = color,
            onSize = { value -> update { it.copy(timeSizeSp = value) } },
            onOpacity = { value -> update { it.copy(timeOpacity = value) } },
        ),
    )
    config.datePattern?.let { pattern ->
        addAll(
            textStyle(
                stringResource(R.string.widget_clock_date_size), stringResource(R.string.widget_clock_date_opacity),
                preview = runCatching { DateFormat.format(pattern, Date()).toString() }.getOrDefault(pattern),
                size = config.dateSizeSp, sizeRange = ClockWidgetConfig.DATE_SIZE_RANGE, opacity = config.dateOpacity, color = color,
                onSize = { value -> update { it.copy(dateSizeSp = value) } },
                onOpacity = { value -> update { it.copy(dateOpacity = value) } },
            ),
        )
    }
    if (config.showBattery) {
        addAll(
            textStyle(
                stringResource(R.string.widget_clock_battery_size), stringResource(R.string.widget_clock_battery_opacity),
                preview = stringResource(R.string.widget_clock_battery_percent, ClockWidgetBattery.percent(context) ?: 100),
                size = config.batterySizeSp, sizeRange = ClockWidgetConfig.DATE_SIZE_RANGE, opacity = config.batteryOpacity, color = color,
                onSize = { value -> update { it.copy(batterySizeSp = value) } },
                onOpacity = { value -> update { it.copy(batteryOpacity = value) } },
            ),
        )
    }
    if (config.showWeather) {
        addAll(
            textStyle(
                stringResource(R.string.widget_clock_weather_size), stringResource(R.string.widget_clock_weather_opacity),
                preview = ClockWidgetWeather.current(context)
                    ?.let { ClockWidgetWeather.formatted(it, config.weatherFahrenheit) }
                    ?: stringResource(R.string.widget_clock_weather_waiting),
                size = config.weatherSizeSp, sizeRange = ClockWidgetConfig.DATE_SIZE_RANGE, opacity = config.weatherOpacity, color = color,
                onSize = { value -> update { it.copy(weatherSizeSp = value) } },
                onOpacity = { value -> update { it.copy(weatherOpacity = value) } },
            ),
        )
    }
    if (config.showAgenda || config.showAlarm) {
        addAll(
            textStyle(
                stringResource(R.string.widget_clock_agenda_size), stringResource(R.string.widget_clock_agenda_opacity),
                preview = ClockWidgetAgenda.events(context, 1, config.agendaLookaheadHours, config.agendaRelative)
                    .firstOrNull()?.text ?: stringResource(R.string.widget_clock_agenda_empty),
                size = config.agendaSizeSp, sizeRange = ClockWidgetConfig.DATE_SIZE_RANGE, opacity = config.agendaOpacity, color = color,
                onSize = { value -> update { it.copy(agendaSizeSp = value) } },
                onOpacity = { value -> update { it.copy(agendaOpacity = value) } },
            ),
        )
    }
    if (config.showNotifications) {
        addAll(
            textStyle(
                stringResource(R.string.widget_clock_notifications_size), stringResource(R.string.widget_clock_notifications_opacity),
                preview = pluralStringResource(R.plurals.widget_clock_notification_count, 3, 3),
                size = config.notificationsSizeSp, sizeRange = ClockWidgetConfig.DATE_SIZE_RANGE, opacity = config.notificationsOpacity, color = color,
                onSize = { value -> update { it.copy(notificationsSizeSp = value) } },
                onOpacity = { value -> update { it.copy(notificationsOpacity = value) } },
            ),
        )
    }
}

private fun textStyle(
    sizeLabel: String,
    opacityLabel: String,
    preview: String,
    size: Int,
    sizeRange: IntRange,
    opacity: Int,
    color: Int,
    onSize: (Int) -> Unit,
    onOpacity: (Int) -> Unit,
): List<SettingsRow> {
    val opacityRange = ClockWidgetConfig.OPACITY_RANGE
    return listOf(
        settingsBlock(sizeLabel, value = size.toString()) {
            SettingsSlider(size.toFloat(), sizeRange.first.toFloat()..sizeRange.last.toFloat(), { onSize(it.roundToInt().coerceIn(sizeRange)) })
        },
        settingsBlock(opacityLabel, value = "$opacity%") {
            SettingsSlider(opacity.toFloat(), opacityRange.first.toFloat()..opacityRange.last.toFloat(), { onOpacity(it.roundToInt().coerceIn(opacityRange)) })
        },
        settingsBlock(preview) {
            // Boxed at a fixed height so the screen does not jump as the slider moves, and
            // scrollable sideways because a large size runs off the edge.
            Box(Modifier.fillMaxWidth().height(72.dp).horizontalScroll(rememberScrollState()), contentAlignment = Alignment.CenterStart) {
                Text(
                    preview,
                    fontSize = size.sp,
                    lineHeight = size.sp,
                    maxLines = 1,
                    color = Color(ClockWidgetConfig.opacityOf(color, opacity)),
                )
            }
        },
    )
}
