// SPDX-License-Identifier: GPL-3.0-or-later
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

internal enum class WeatherPage { OVERVIEW, HOURLY, DAILY, DAYLIGHT }

/** The existing weather bridge, optionally enriched from the same Breezy app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeatherOverview(
    state: ClockLiveState,
    page: WeatherPage,
    onPage: (WeatherPage) -> Unit,
    onOpenApp: () -> Unit,
) {
    val rootScroll = rememberLazyListState()
    val dailyScroll = rememberLazyListState()
    val hourlyScroll = rememberLazyListState()
    val rootHoursScroll = rememberScrollState()
    val rootDaysScroll = rememberScrollState()
    val dayHoursScroll = rememberScrollState()
    val hourlyChartScroll = rememberScrollState()
    val detailDaysScroll = rememberScrollState()
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedHour by rememberSaveable { mutableStateOf<Long?>(null) }
    var revealHour by remember { mutableStateOf(false) }
    var revealDate by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val zone = state.weather?.zone ?: ZoneId.systemDefault()
    val c = state.config
    val connection = rememberBreezyWeatherConnection(c.weatherPackage, state.weather?.location, onOpenApp)
    val today = state.now.atZone(zone).toLocalDate()
    val hours = state.weather?.hours.orEmpty().filter { it.at >= state.now.minusSeconds(3600) }
    val days = state.weather?.days.orEmpty().filter { it.date >= today }
    val day = days.firstOrNull { it.date.toString() == selectedDate } ?: days.firstOrNull()
    val hour = hours.firstOrNull { it.at.epochSecond == selectedHour } ?: hours.firstOrNull()
    val todayTitle = stringResource(R.string.weather_today)
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cellPx = with(LocalDensity.current) { (88.dp * fontScale).toPx() }
    LaunchedEffect(page, revealHour) {
        if (page == WeatherPage.HOURLY && revealHour) {
            val index = hours.indexOfFirst { it.at.epochSecond == selectedHour }.coerceAtLeast(0)
            hourlyChartScroll.scrollTo((cellPx * index).toInt())
            revealHour = false
        }
    }
    LaunchedEffect(page) {
        if (page != WeatherPage.DAILY) revealDate = null
    }
    val dayPoints = days.map {
        ForecastPoint(if (it.date == today) todayTitle else it.date.format(dayFormat),
            it.date.format(dateFormat), it.maxK, it.minK, it.code, it.rain, it.metrics, date = it.date)
    }
    fun hourPoints(items: List<WeatherHour>) = items.map {
        val astro = days.firstOrNull { d -> d.date == it.at.atZone(zone).toLocalDate() }
        val night = it.providerNight ?: sunWindowNight(it.at, astro?.sunrise ?: state.weather?.sunrise,
            astro?.sunset ?: state.weather?.sunset, zone) ?: it.night
        val isNight = night == true
        ForecastPoint(weatherClock(context, it.at, c.hourFormat, zone),
            it.at.atZone(zone).toLocalDate().format(dateFormat),
            it.tempK, null, it.code, it.rain, it.metrics, at = it.at, night = isNight)
    }
    val effectsSpec = AppMotion.effects<Float>()
    val offsetSpec = AppMotion.offset()
    Column(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                val direction = if (targetState == WeatherPage.OVERVIEW) -1 else 1
                ((fadeIn(effectsSpec) +
                    slideInHorizontally(offsetSpec) { it / 8 * direction }) togetherWith
                    (fadeOut(effectsSpec) +
                        slideOutHorizontally(offsetSpec) { -it / 8 * direction })).using(null)
            }, label = "weather-page",
        ) { target ->
            when (target) {
                WeatherPage.OVERVIEW -> LazyColumn(
                    Modifier.fillMaxSize().testTag("weather-overview-list"), state = rootScroll,
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item("current") { CurrentWeather(state, connection) }
                    item("daily") {
                        ForecastCard(stringResource(R.string.clock_weather_daily), Icons.Outlined.CalendarMonth,
                            dayPoints, c, rootDaysScroll, "weather-open-daily",
                            selected = days.indexOf(day),
                            onDetails = { onPage(WeatherPage.DAILY) },
                            onSelect = {
                                selectedDate = days[it].date.toString()
                                revealDate = selectedDate
                                // An explicit selection reveals its date strip even if the
                                // previous detail page was scrolled down. Ordinary return via
                                // Details keeps both existing scroll positions.
                                dailyScroll.requestScrollToItem(0)
                                onPage(WeatherPage.DAILY)
                            })
                    }
                    item("hourly") {
                        ForecastCard(stringResource(R.string.clock_weather_hourly), Icons.Outlined.Schedule,
                            hourPoints(hours.take(24)), c, rootHoursScroll, "weather-open-hourly",
                            selected = hours.take(24).indexOf(hour),
                            onDetails = { onPage(WeatherPage.HOURLY) },
                            onSelect = { selectedHour = hours[it].at.epochSecond; revealHour = true; onPage(WeatherPage.HOURLY) })
                    }
                    state.weather?.let { w ->
                        if (w.sunrise != null && w.sunset != null) item("sun") {
                            SunCycleCard(w.sunrise, w.sunset, w.moonrise, w.moonset, w.moonPhase,
                                state.now, c, onDetails = { onPage(WeatherPage.DAYLIGHT) }, zone = zone)
                        }
                        val rainHours = hours.filter { it.at >= state.now }.take(12)
                        if (w.rain != null || rainHours.any { it.rain != null }) item("rain") {
                            val forecastDate = w.updatedAt.atZone(zone).toLocalDate()
                            val period = if (forecastDate == today) todayTitle else AgendaText.date(context, forecastDate)
                            RainChanceCard(w.rain, period, c, rainHours, zone)
                        }
                        item("metrics") { WeatherMetricGrid(w.metrics, c) }
                    }
                }
                WeatherPage.DAILY -> LazyColumn(
                    Modifier.fillMaxSize().testTag("weather-daily-list"), state = dailyScroll,
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (day == null) item("empty") { ForecastUnavailable() }
                    else {
                        item("dates") {
                            Row(Modifier.testTag("weather-date-strip").horizontalScroll(detailDaysScroll),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayPoints.forEach { p ->
                                    key(p.date) {
                                        val requester = remember { BringIntoViewRequester() }
                                        var placed by remember { mutableStateOf(false) }
                                        LaunchedEffect(page, revealDate, placed) {
                                            if (page == WeatherPage.DAILY && revealDate == p.date.toString() && placed) {
                                                requester.bringIntoView()
                                                revealDate = null
                                            }
                                        }
                                        FilterChip(selected = p.date == day.date,
                                            modifier = Modifier.testTag("weather-day-" + p.date)
                                                .bringIntoViewRequester(requester)
                                                .onGloballyPositioned { placed = true },
                                            onClick = { selectedDate = p.date.toString() },
                                            label = {
                                                Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(p.title, style = MaterialTheme.typography.labelLarge)
                                                    Text(p.subtitle, style = MaterialTheme.typography.titleMedium)
                                                }
                                            })
                                    }
                                }
                            }
                        }
                        item("summary") {
                            Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(AgendaText.date(context, day.date), style = MaterialTheme.typography.titleLarge)
                                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = 2) {
                                        TemperatureSummary(stringResource(R.string.weather_high), day.maxK, c,
                                            Modifier.weight(1f).widthIn(min = 112.dp * fontScale))
                                        TemperatureSummary(stringResource(R.string.weather_low), day.minK, c,
                                            Modifier.weight(1f).widthIn(min = 112.dp * fontScale))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        WeatherConditionIcon(day.code, modifier = Modifier.size(40.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(stringResource(weatherLabel(day.code)), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                        item("day-hours") {
                            val onDay = hours.filter { it.at.atZone(zone).toLocalDate() == day.date }
                            ForecastCard(stringResource(R.string.clock_weather_hourly), Icons.Outlined.Schedule,
                                hourPoints(onDay), c, dayHoursScroll,
                                selected = onDay.indexOf(hour),
                                onSelect = { selectedHour = onDay[it].at.epochSecond; revealHour = true; onPage(WeatherPage.HOURLY) })
                        }
                        if (day.rain != null) item("rain") {
                            RainChanceCard(day.rain, AgendaText.date(context, day.date), c, zone = zone)
                        }
                        item("metrics") { WeatherMetricGrid(day.metrics, c) }
                        if (day.sunrise != null && day.sunset != null) item("sun") {
                            SunCycleCard(day.sunrise, day.sunset, day.moonrise, day.moonset, day.moonPhase, state.now, c, zone = zone)
                        }
                    }
                }
                WeatherPage.HOURLY -> LazyColumn(
                    Modifier.fillMaxSize().testTag("weather-hourly-list"), state = hourlyScroll,
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (hour == null) item("empty") { ForecastUnavailable() }
                    else {
                        item("summary") {
                            val selectedPoint = hourPoints(listOf(hour)).single()
                            Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(AgendaText.date(context, hour.at.atZone(zone).toLocalDate()) + " · " +
                                        weatherClock(context, hour.at, c.hourFormat, zone), modifier = Modifier.testTag("weather-hour-selected-time"),
                                        style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        WeatherConditionIcon(hour.code, selectedPoint.night, Modifier.size(48.dp))
                                        Text(ClockWidgetWeather.degrees(hour.tempK, c.weatherFahrenheit) +
                                            if (c.weatherFahrenheit) "F" else "C",
                                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.displayMedium)
                                    }
                                    Text(stringResource(weatherLabel(hour.code)), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        item("chart") {
                            val upcoming = hours
                            ForecastCard(stringResource(R.string.clock_weather_hourly), Icons.Outlined.Schedule,
                                hourPoints(upcoming), c, hourlyChartScroll,
                                selected = upcoming.indexOf(hour),
                                onSelect = { selectedHour = upcoming[it].at.epochSecond })
                        }
                        if (hour.rain != null) item("rain") {
                            RainChanceCard(hour.rain, AgendaText.date(context, hour.at.atZone(zone).toLocalDate()) + " · " +
                                weatherClock(context, hour.at, c.hourFormat, zone), c, zone = zone)
                        }
                        item("metrics") { WeatherMetricGrid(hour.metrics, c) }
                    }
                }
                WeatherPage.DAYLIGHT -> LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item("sun") {
                        val w = state.weather
                        SunCycleCard(w?.sunrise, w?.sunset, w?.moonrise, w?.moonset, w?.moonPhase, state.now, c, zone = zone)
                    }
                }
            }
        }
        if (c.weatherPackage != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            TextButton(onClick = onOpenApp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.clock_weather_open_app))
            }
        }
    }
}

@Composable
private fun CurrentWeather(state: ClockLiveState, connection: BreezyWeatherConnection) {
    val w = state.weather
    val c = state.config
    val current = state.weatherReading ?: w?.let { ClockWidgetWeather.Reading(it.tempK, it.condition) }
    val zone = w?.zone ?: ZoneId.systemDefault()
    val currentNight = w?.hours?.minByOrNull { kotlin.math.abs(it.at.epochSecond - state.now.epochSecond) }
        ?.takeIf { kotlin.math.abs(it.at.epochSecond - state.now.epochSecond) <= 3600 }
        ?.providerNight ?: sunWindowNight(state.now, w?.sunrise, w?.sunset, zone)
    Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherConditionIcon(w?.code ?: 3200, currentNight == true,
                    Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Text(w?.location?.takeIf { it.isNotBlank() } ?: stringResource(R.string.clock_weather_current),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (c.weatherPackage != null || connection.result != null &&
                    connection.result != BreezyWeatherProvider.Result.Unsupported) {
                    IconButton(onClick = connection.refresh, enabled = !connection.loading,
                        modifier = Modifier.testTag("weather-refresh-in-app")) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.weather_refresh_local))
                    }
                }
            }
            if (current != null) {
                Text(ClockWidgetWeather.degrees(current.tempK, c.weatherFahrenheit) +
                    if (c.weatherFahrenheit) "F" else "C", style = MaterialTheme.typography.displayMedium)
                Text(current.condition.ifBlank { stringResource(weatherLabel(w?.code ?: 3200)) },
                    style = MaterialTheme.typography.titleMedium)
            } else Text(stringResource(R.string.clock_weather_stale))
            w?.let {
                Text(stringResource(R.string.weather_data_updated,
                    buildString {
                        if (it.updatedAt.atZone(zone).toLocalDate() !=
                            state.now.atZone(zone).toLocalDate())
                            append(AgendaText.date(LocalContext.current, it.updatedAt.atZone(zone).toLocalDate()) + " · ")
                        append(weatherClock(LocalContext.current, it.updatedAt, c.hourFormat, zone))
                    }),
                    style = MaterialTheme.typography.bodySmall)
            }
            if (connection.result is BreezyWeatherProvider.Result.PermissionRequired) {
                TextButton(onClick = connection.connect, modifier = Modifier.testTag("weather-breezy-connect")) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.weather_breezy_connect))
                }
            }
            connection.error?.let { reason ->
                TextButton(onClick = connection.openApp) {
                    Text(stringResource(if (reason == BreezyWeatherProvider.Reason.LOCATION_AMBIGUOUS ||
                        reason == BreezyWeatherProvider.Reason.NO_LOCATIONS)
                        R.string.weather_breezy_location else R.string.weather_breezy_unavailable))
                }
            }
        }
    }
}
