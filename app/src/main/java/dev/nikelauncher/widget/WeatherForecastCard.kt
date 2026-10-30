// SPDX-License-Identifier: GPL-3.0-or-later
// Split from WeatherOverview.kt, adapted from Victoria Grid (GPL-3.0-or-later).
package dev.nikelauncher.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ForecastCard(
    title: String, icon: ImageVector, points: List<ForecastPoint>, config: ClockWidgetConfig,
    scroll: ScrollState, detailsTag: String? = null,
    onDetails: (() -> Unit)? = null, selected: Int = -1, onSelect: (Int) -> Unit,
) {
    var metric by rememberSaveable { mutableStateOf(ForecastMetric.TEMPERATURE) }
    val options = buildList {
        add(ForecastMetric.TEMPERATURE)
        if (points.any { it.metrics.windKph != null }) add(ForecastMetric.WIND)
        if (points.any { it.metrics.airQuality?.index != null }) add(ForecastMetric.AIR)
    }
    val active = metric.takeIf { it in options } ?: ForecastMetric.TEMPERATURE
    Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (onDetails != null)
                    TextButton(onClick = onDetails, enabled = points.isNotEmpty(), modifier = detailsTag?.let { Modifier.testTag(it) } ?: Modifier) { Text(stringResource(R.string.weather_details)) }
            }
            if (points.isEmpty()) ForecastUnavailable()
            else {
                if (options.size > 1) {
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { m ->
                            FilterChip(selected = active == m, onClick = { metric = m },
                                label = { Text(stringResource(when (m) {
                                    ForecastMetric.TEMPERATURE -> R.string.weather_conditions
                                    ForecastMetric.WIND -> R.string.weather_wind_chart
                                    ForecastMetric.AIR -> R.string.weather_air_quality
                                })) })
                        }
                    }
                }
                Crossfade(active, animationSpec = AppMotion.effects(), label = "weather-chart-metric") { m ->
                    ForecastChart(points, config, scroll, selected, m, onSelect)
                }
            }
        }
    }
}

@Composable
internal fun TemperatureSummary(label: String, kelvin: Int, c: ClockWidgetConfig, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(ClockWidgetWeather.degrees(kelvin, c.weatherFahrenheit) + if (c.weatherFahrenheit) "F" else "C",
            style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
internal fun ForecastUnavailable() {
    Text(stringResource(R.string.clock_weather_source_hint), Modifier.fillMaxWidth().padding(20.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun weatherLabel(code: Int): Int = when (weatherCondition(code)) {
    WeatherCondition.THUNDER -> R.string.clock_weather_storm
    WeatherCondition.DRIZZLE -> R.string.weather_drizzle
    WeatherCondition.FREEZING_RAIN -> R.string.weather_freezing_rain
    WeatherCondition.RAIN -> R.string.clock_weather_rain
    WeatherCondition.HAIL -> R.string.weather_hail
    WeatherCondition.SLEET -> R.string.weather_sleet
    WeatherCondition.SNOW -> R.string.clock_weather_snow
    WeatherCondition.HAZE -> R.string.weather_haze
    WeatherCondition.DUST -> R.string.weather_dust
    WeatherCondition.WIND -> R.string.weather_windy
    WeatherCondition.TORNADO -> R.string.weather_tornado
    WeatherCondition.FOG -> R.string.clock_weather_fog
    WeatherCondition.CLEAR -> R.string.clock_weather_clear
    WeatherCondition.FEW_CLOUDS -> R.string.weather_few_clouds
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.MOSTLY_CLOUDY -> R.string.weather_mostly_cloudy
    WeatherCondition.OVERCAST -> R.string.clock_weather_cloudy
    WeatherCondition.UNKNOWN -> R.string.clock_weather_unknown
}

