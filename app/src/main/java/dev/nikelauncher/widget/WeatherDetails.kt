// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Chance over the named forecast period; never a measured amount or a current rain gauge. */
@Composable
internal fun RainChanceCard(chance: Int?, period: String, config: ClockWidgetConfig, hours: List<WeatherHour> = emptyList(),
    zone: ZoneId = ZoneId.systemDefault()) {
    val context = LocalContext.current
    val upcoming = hours.take(12)
    if (chance == null && upcoming.none { it.rain != null }) return
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Surface(Modifier.fillMaxWidth().testTag("weather-rain-card"), MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.weather_precipitation), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
            }
            if (chance != null) {
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(period, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.weather_percent, chance), style = MaterialTheme.typography.headlineMedium)
                }
                LinearProgressIndicator(progress = { chance / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
            if (upcoming.any { it.rain != null }) {
                Text(stringResource(R.string.weather_next_hours), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming.forEach { hour ->
                        val time = weatherClock(context, hour.at, config.hourFormat, zone)
                        val value = hour.rain?.let { stringResource(R.string.weather_percent, it) } ?: "—"
                        val description = stringResource(R.string.weather_rain_hour, time, value)
                        val barColor = MaterialTheme.colorScheme.primary
                        val track = MaterialTheme.colorScheme.surfaceContainerHighest
                        Column(Modifier.width(64.dp * scale).semantics(mergeDescendants = true) {
                            contentDescription = description
                        }, horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(time, style = MaterialTheme.typography.labelMedium)
                            Canvas(Modifier.width(16.dp).height(48.dp)) {
                                drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2))
                                hour.rain?.takeIf { it > 0 }?.let { rain ->
                                    val height = size.height * rain / 100f
                                    drawRoundRect(barColor, topLeft = Offset(0f, size.height - height),
                                        size = Size(size.width, height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(minOf(size.width / 2, height / 2)))
                                }
                            }
                            Text(value, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeatherMetricGrid(metrics: WeatherMetrics, config: ClockWidgetConfig) {
    val values = buildList<Pair<String, String>> {
        metrics.feelsLikeK?.let { add(stringResource(R.string.weather_feels_like) to ClockWidgetWeather.degrees(it, config.weatherFahrenheit)) }
        metrics.windKph?.let { add(stringResource(R.string.weather_wind) to stringResource(R.string.weather_wind_value, it.roundToInt())) }
        metrics.windDirection?.let { add(stringResource(R.string.weather_wind_direction) to stringResource(R.string.weather_direction_value, it)) }
        metrics.humidity?.let { add(stringResource(R.string.weather_humidity) to stringResource(R.string.weather_percent, it)) }
        metrics.uv?.let { add(stringResource(R.string.weather_uv) to String.format(LocalConfiguration.current.locales[0], "%.1f", it)) }
        metrics.pressureHpa?.let { add(stringResource(R.string.weather_pressure) to stringResource(R.string.weather_pressure_value, it.roundToInt())) }
        metrics.cloudCover?.let { add(stringResource(R.string.weather_cloud_cover) to stringResource(R.string.weather_percent, it)) }
        metrics.dewPointK?.let { add(stringResource(R.string.weather_dew_point) to ClockWidgetWeather.degrees(it, config.weatherFahrenheit)) }
        metrics.visibilityM?.let { add(stringResource(R.string.weather_visibility) to stringResource(R.string.weather_visibility_value, it.roundToInt())) }
        metrics.airQuality?.index?.let { add(stringResource(R.string.weather_air_quality) to it.toString()) }
    }
    if (values.isNotEmpty()) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            values.forEach { (label, value) ->
                Surface(Modifier.weight(1f).widthIn(min = 136.dp), MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
    metrics.airQuality?.let { aq ->
        if (aq.pollutants.isNotEmpty() || aq.indices.isNotEmpty()) {
            if (values.isNotEmpty()) Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.weather_air_quality), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.weather_aqi_source), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    (aq.pollutants.keys + aq.indices.keys).distinct().forEach { key ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(key.uppercase(java.util.Locale.ROOT).replace("PM25", "PM2.5"), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            aq.pollutants[key]?.let {
                                Text(stringResource(R.string.weather_pollutant_value, it),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            aq.indices[key]?.let {
                                Text(stringResource(R.string.weather_aqi_value, it),
                                    style = MaterialTheme.typography.bodyMedium)
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
internal fun SunCycleCard(
    rise: Instant?, set: Instant?, moonrise: Instant?, moonset: Instant?, phase: Int?,
    now: Instant, config: ClockWidgetConfig, onDetails: (() -> Unit)? = null,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val context = LocalContext.current
    val progress = daylightProgress(now, rise, set)
    val night = sunWindowNight(now, rise, set, zone)
    val displayedDate = rise?.atZone(zone)?.toLocalDate()
    val minimumTimeWidth = 132.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    // Starts at the actual value. Scrolling a card back into view never replays an entrance.
    val animated by animateFloatAsState(progress ?: 0f, AppMotion.effects(), label = "sun-cycle")
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val description = listOfNotNull(
        rise?.let { stringResource(R.string.clock_weather_sunrise) + " " + weatherClock(context, it, config.hourFormat, zone) },
        set?.let { stringResource(R.string.clock_weather_sunset) + " " + weatherClock(context, it, config.hourFormat, zone) },
    ).joinToString(", ")
    Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbTwilight, null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.weather_day_night), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                if (onDetails != null) TextButton(onClick = onDetails) {
                    Text(stringResource(R.string.weather_details))
                }
            }
            if (night != null || displayedDate != null && displayedDate != now.atZone(zone).toLocalDate()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (night != null) {
                        Surface(shape = MaterialTheme.shapes.large,
                            color = if (night) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (night) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (night) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny,
                                    null, Modifier.size(18.dp))
                                Text(stringResource(if (night) R.string.weather_nighttime else R.string.weather_daytime),
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else if (displayedDate != null) {
                        // A forecast day's path is not the sun's position right now.
                        Text(AgendaText.date(context, displayedDate), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (progress != null && rise != null && set != null) {
                Canvas(Modifier.fillMaxWidth().height(144.dp).semantics { contentDescription = description }) {
                    val radius = minOf(size.width / 2f - 16.dp.toPx(), size.height - 20.dp.toPx())
                    val center = Offset(size.width / 2f, size.height - 14.dp.toPx())
                    val origin = Offset(center.x - radius, center.y - radius)
                    val diameter = Size(radius * 2f, radius * 2f)
                    drawArc(Brush.verticalGradient(listOf(primary.copy(alpha = .13f), Color.Transparent),
                        startY = origin.y, endY = center.y), 180f, 180f, true, origin, diameter)
                    drawArc(muted, 180f, 180f, false, origin, diameter, style = Stroke(2.dp.toPx()))
                    drawLine(muted, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
                    drawArc(primary, 180f, animated * 180f, false, origin, diameter,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(muted, 3.dp.toPx(), Offset(center.x - radius, center.y))
                    drawCircle(muted, 3.dp.toPx(), Offset(center.x + radius, center.y))
                    if (night == false && now >= rise && now < set) {
                        val angle = Math.PI * (1f - animated)
                        val point = Offset(center.x + radius * cos(angle).toFloat(), center.y - radius * sin(angle).toFloat())
                        drawCircle(primary.copy(alpha = .12f), 14.dp.toPx(), point)
                        drawCircle(surface, 8.dp.toPx(), point)
                        drawCircle(primary, 6.dp.toPx(), point)
                    }
                }
                val duration = Duration.between(rise, set).seconds
                Text(stringResource(R.string.weather_daylight_duration, (duration / 3600).toInt(), (duration % 3600 / 60).toInt()),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
            if (rise != null || set != null) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                    AstroTime(stringResource(R.string.clock_weather_sunrise), rise, config,
                        Icons.Outlined.ArrowUpward, Modifier.weight(1f).widthIn(min = minimumTimeWidth), zone)
                    AstroTime(stringResource(R.string.clock_weather_sunset), set, config,
                        Icons.Outlined.ArrowDownward, Modifier.weight(1f).widthIn(min = minimumTimeWidth), zone)
                }
            } else Text(stringResource(R.string.weather_sun_unavailable), style = MaterialTheme.typography.bodyMedium)
            if (moonrise != null || moonset != null || phase != null) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NightsStay, null)
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.weather_moon), style = MaterialTheme.typography.titleMedium)
                        phase?.let {
                            Text(stringArrayResource(R.array.weather_moon_phases)[moonPhaseSector(it)],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (moonrise != null || moonset != null) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                        AstroTime(stringResource(R.string.weather_moonrise), moonrise, config,
                            Icons.Outlined.ArrowUpward, Modifier.weight(1f).widthIn(min = minimumTimeWidth), zone)
                        AstroTime(stringResource(R.string.weather_moonset), moonset, config,
                            Icons.Outlined.ArrowDownward, Modifier.weight(1f).widthIn(min = minimumTimeWidth), zone)
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroTime(title: String, at: Instant?, config: ClockWidgetConfig, icon: ImageVector, modifier: Modifier, zone: ZoneId) {
    val context = LocalContext.current
    val time = at?.let { weatherClock(context, it, config.hourFormat, zone) } ?: "—"
    Surface(modifier.semantics(mergeDescendants = true) {
        // Keep the full sunrise/sunset/moonrise/moonset label available to TalkBack.
        contentDescription = "$title: $time"
    }, MaterialTheme.shapes.largeIncreased, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(time, style = if (time.length > 5) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}
