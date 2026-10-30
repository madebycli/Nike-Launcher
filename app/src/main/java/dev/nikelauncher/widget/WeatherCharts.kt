// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

internal data class ForecastPoint(
    val title: String, val subtitle: String,
    val highK: Int, val lowK: Int?, val code: Int, val rain: Int?,
    val metrics: WeatherMetrics,
    val at: Instant? = null, val date: LocalDate? = null, val night: Boolean = false,
)

internal enum class ForecastMetric { TEMPERATURE, WIND, AIR }

/**
 * Compose presentation inspired by Breezy's Daily/Hourly trend cards.
 * All paths are drawn from received samples; no synthetic normal/day/night series.
 */
@Composable
internal fun ForecastChart(
    points: List<ForecastPoint>,
    config: ClockWidgetConfig,
    scroll: ScrollState,
    selected: Int = -1,
    metric: ForecastMetric = ForecastMetric.TEMPERATURE,
    onSelect: (Int) -> Unit,
) {
    if (points.isEmpty()) return
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cellWidth = 88.dp * fontScale
    val currentSelect by rememberUpdatedState(onSelect)
    val color = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    val foreground = MaterialTheme.colorScheme.onSurface
    val textStyle = MaterialTheme.typography.labelLarge.copy(color = color)
    val lowTextStyle = textStyle.copy(color = secondary)
    val measurer = rememberTextMeasurer()
    fun value(point: ForecastPoint): Float? = when (metric) {
        ForecastMetric.TEMPERATURE -> point.highK.toFloat()
        ForecastMetric.WIND -> point.metrics.windKph
        ForecastMetric.AIR -> point.metrics.airQuality?.index?.toFloat()
    }
    fun label(v: Float): String = when (metric) {
        ForecastMetric.TEMPERATURE -> ClockWidgetWeather.degrees(v.toInt(), config.weatherFahrenheit)
        ForecastMetric.WIND -> v.roundToInt().toString()
        ForecastMetric.AIR -> v.toInt().toString()
    }
    val values = points.map(::value)
    val lows = points.map { if (metric == ForecastMetric.TEMPERATURE) it.lowK?.toFloat() else null }
    val all = (values + lows).filterNotNull()
    val min = (all.minOrNull() ?: 0f) - 2f
    val max = (all.maxOrNull() ?: 1f) + 2f
    // Measure labels once per data/style change, not on each draw.
    val labels = remember(values, lows, config.weatherFahrenheit, metric, textStyle, lowTextStyle, measurer) {
        values.map { it?.let { v -> measurer.measure(label(v), style = textStyle) } } +
            lows.map { it?.let { v -> measurer.measure(label(v), style = lowTextStyle) } }
    }
    val descriptions = points.map { point ->
        val unavailable = stringResource(R.string.weather_chart_value_missing)
        val reading = when (metric) {
            ForecastMetric.TEMPERATURE -> {
                fun degrees(kelvin: Int) = ClockWidgetWeather.degrees(kelvin, config.weatherFahrenheit) +
                    if (config.weatherFahrenheit) "F" else "C"
                point.lowK?.let {
                    stringResource(R.string.weather_high) + " " + degrees(point.highK) + ", " +
                        stringResource(R.string.weather_low) + " " + degrees(it)
                } ?: degrees(point.highK)
            }
            ForecastMetric.WIND -> stringResource(R.string.weather_wind) + ", " +
                (point.metrics.windKph?.let { stringResource(R.string.weather_wind_value, it.roundToInt()) } ?: unavailable)
            ForecastMetric.AIR -> stringResource(R.string.weather_air_quality) + ", " +
                (point.metrics.airQuality?.index?.let { stringResource(R.string.weather_aqi_value, it) } ?: unavailable)
        }
        listOfNotNull(point.title, point.subtitle.takeIf { it.isNotEmpty() }, reading,
            stringResource(weatherLabel(point.code)),
            point.rain?.let { stringResource(R.string.clock_weather_rain_chance, it) })
            .joinToString(", ")
    }
    Column {
        ForecastLegend(metric, lows.any { it != null }, config)
        Column(Modifier.testTag("weather-forecast-chart").horizontalScroll(scroll)) {
            Box(Modifier.width(cellWidth * points.size)) {
                Row(Modifier.matchParentSize()) {
                    points.indices.forEach { i ->
                        Box(Modifier.width(cellWidth).fillMaxHeight().padding(horizontal = 4.dp, vertical = 4.dp)
                            .background(if (i == selected) color.copy(alpha = .12f) else Color.Transparent,
                                MaterialTheme.shapes.largeIncreased))
                    }
                }
                Column {
                    Row {
                        points.forEachIndexed { index, p ->
                            Column(
                                Modifier.width(cellWidth).testTag(p.at?.let { "weather-hour-" + it.epochSecond } ?: "weather-day-" + p.date)
                                    .clickable { onSelect(index) }
                                    .semantics {
                                        contentDescription = descriptions[index]
                                        this.selected = index == selected
                                    }
                                    .padding(top = 12.dp, bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(p.title, style = MaterialTheme.typography.titleSmall,
                                    color = if (index == selected) color else foreground,
                                    fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium)
                                if (p.subtitle.isNotEmpty())
                                    Text(p.subtitle, style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                WeatherConditionIcon(p.code, p.night, Modifier.size(36.dp), stringResource(weatherLabel(p.code)))
                            }
                        }
                    }
                    Canvas(Modifier.fillMaxWidth().height(152.dp * fontScale).pointerInput(points.size) {
                        detectTapGestures { position ->
                            val index = (position.x / (size.width.toFloat() / points.size)).toInt()
                                .coerceIn(0, points.lastIndex)
                            currentSelect(index)
                        }
                    }) {
                        val cell = size.width / points.size
                        val top = (26.dp * fontScale).toPx()
                        val bottom = size.height - (28.dp * fontScale).toPx()
                        fun y(v: Float) = bottom - (v - min) / (max - min) * (bottom - top)
                        for (i in 0..2) {
                            val lineY = top + (bottom - top) * i / 2
                            drawLine(grid, Offset(0f, lineY), Offset(size.width, lineY), 1.dp.toPx())
                        }
                        points.indices.forEach { i ->
                            val x = cell * (i + .5f)
                            drawLine(grid, Offset(x, top), Offset(x, bottom), 1.dp.toPx())
                        }
                        if (selected in points.indices) {
                            val x = cell * (selected + .5f)
                            drawLine(color.copy(alpha = .35f), Offset(x, top), Offset(x, bottom), 1.5.dp.toPx())
                        }
                        fun series(samples: List<Float?>, tint: Color, labelOffset: Int, below: Boolean) {
                            var start = 0
                            while (start < samples.size) {
                                while (start < samples.size && samples[start] == null) { start++ }
                                if (start >= samples.size) { break }
                                var end = start
                                while (end + 1 < samples.size && samples[end + 1] != null) { end++ }
                                val path = Path()
                                for (i in start..end) {
                                    val x = cell * (i + .5f)
                                    val py = y(checkNotNull(samples[i]))
                                    if (i == start) path.moveTo(x, py) else {
                                        val previousX = cell * (i - .5f)
                                        val previousY = y(checkNotNull(samples[i - 1]))
                                        val middleX = (previousX + x) / 2f
                                        // Both controls remain between the real endpoints. The
                                        // curve passes through every sample without overshooting
                                        // their range; null samples still split the path above.
                                        path.cubicTo(middleX, previousY, middleX, py, x, py)
                                    }
                                }
                                val area = Path().apply {
                                    addPath(path)
                                    lineTo(cell * (end + .5f), bottom)
                                    lineTo(cell * (start + .5f), bottom)
                                    close()
                                }
                                drawPath(area, Brush.verticalGradient(listOf(tint.copy(alpha = .22f), Color.Transparent)))
                                drawPath(path, tint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                                for (i in start..end) {
                                    val x = cell * (i + .5f)
                                    val py = y(checkNotNull(samples[i]))
                                    val radius = if (i == selected) 5.dp.toPx() else 2.5.dp.toPx()
                                    if (below) drawCircle(tint, radius, Offset(x, py), style = Stroke(2.dp.toPx()))
                                    else drawCircle(tint, radius, Offset(x, py))
                                    labels[i + labelOffset]?.let { layout ->
                                        val textY = if (below) py + 6.dp.toPx() else py - layout.size.height - 6.dp.toPx()
                                        drawText(layout, topLeft = Offset(x - layout.size.width / 2f,
                                            textY.coerceIn(0f, size.height - layout.size.height)))
                                    }
                                }
                                start = end + 1
                            }
                        }
                        series(values, color, 0, false)
                        series(lows, secondary, points.size, true)
                    }
                    Row {
                        points.forEachIndexed { i, point ->
                            val chance = point.rain?.let { stringResource(R.string.weather_percent, it) }
                            Column(
                                Modifier.width(cellWidth).clickable { onSelect(i) }
                                    .semantics {
                                        contentDescription = descriptions[i]
                                        this.selected = i == selected
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.WaterDrop, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(chance ?: "—", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ForecastLegend(metric: ForecastMetric, hasLow: Boolean, config: ClockWidgetConfig) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (metric == ForecastMetric.TEMPERATURE) {
            ForecastLegendItem(stringResource(if (hasLow) R.string.weather_high else R.string.weather_temperature),
                MaterialTheme.colorScheme.primary)
            if (hasLow) ForecastLegendItem(stringResource(R.string.weather_low),
                MaterialTheme.colorScheme.tertiary, outlined = true)
            Text(stringResource(if (config.weatherFahrenheit) R.string.widget_clock_weather_fahrenheit else R.string.widget_clock_weather_celsius),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        } else {
            ForecastLegendItem(stringResource(if (metric == ForecastMetric.WIND) R.string.weather_wind_chart else R.string.weather_air_quality),
                MaterialTheme.colorScheme.primary)
            if (metric == ForecastMetric.AIR) Text(stringResource(R.string.weather_aqi_unit),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ForecastLegendItem(label: String, color: Color, outlined: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(8.dp)) {
            if (outlined) drawCircle(color, style = Stroke(1.5.dp.toPx())) else drawCircle(color)
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
