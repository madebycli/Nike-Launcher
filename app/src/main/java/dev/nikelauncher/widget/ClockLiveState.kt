// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.ui.res.stringResource
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class ClockLiveState(
    val config: ClockWidgetConfig,
    val now: Instant,
    val calendar: CalendarLoad?,
    val weather: WeatherSnapshot?,
    val weatherReading: ClockWidgetWeather.Reading?,
    val battery: String?,
)

/** Observe only while visible and resumed. Calendar titles never leave memory. */
@Composable
fun rememberClockLiveState(
    widgetId: Int,
    enabled: Boolean = true,
    override: ClockWidgetConfig? = null,
    requireCalendar: Boolean = false,
    loadCalendar: Boolean = true,
    loadWeather: Boolean = true,
): ClockLiveState {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var calendarRevision by remember { mutableIntStateOf(0) }
    var resumed by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) {
                now = Instant.now()
                revision++
                calendarRevision++
            }
        }
        owner.lifecycle.addObserver(observer)
        resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val active = enabled && resumed
    DisposableEffect(active, widgetId) {
        if (!active) return@DisposableEffect onDispose {}
        val clockPrefs = context.getSharedPreferences("clock_widget", Context.MODE_PRIVATE)
        val weatherPrefs =
            context.getSharedPreferences("clock_widget_weather", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        clockPrefs.registerOnSharedPreferenceChangeListener(listener)
        weatherPrefs.registerOnSharedPreferenceChangeListener(listener)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    now = Instant.now()
                    revision++
                    if (intent.action != Intent.ACTION_BATTERY_CHANGED) calendarRevision++
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            clockPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            weatherPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            context.unregisterReceiver(receiver)
        }
    }
    val config =
        remember(revision, override, widgetId) {
            override ?: ClockWidgetConfig.read(context, widgetId)
        }
    RefreshClockWeather(active && loadWeather && config.showWeather, config.weatherPackage)
    val calendarRequested = loadCalendar && (config.showAgenda || requireCalendar)
    val wantsCalendar = active && calendarRequested
    // Access is independent of visibility. A page switch pauses work, not its content.
    val calendarAccess = ClockWidgetAgenda.hasCalendarPermission(context)
    val date = now.atZone(ZoneId.systemDefault()).toLocalDate()
    var calendar by remember(widgetId, calendarRequested, calendarAccess, config.excludedCalendars) {
        mutableStateOf<CalendarLoad?>(
            if (calendarRequested && override == null)
                CalendarSnapshotCache.shared.read(widgetId, config.excludedCalendars, calendarAccess)
            else null
        )
    }
    LaunchedEffect(widgetId, wantsCalendar, calendarAccess, config.excludedCalendars, date, calendarRevision) {
        if (!wantsCalendar) return@LaunchedEffect
        if (!calendarAccess) {
            calendar = CalendarLoad.PermissionRequired
            return@LaunchedEffect
        }
        // Conflate bursts, but finish each query: some providers notify while expanding
        // Instances. Cancelling/restarting every query can otherwise leave loading forever.
        val refresh = Channel<Unit>(Channel.CONFLATED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { refresh.trySend(Unit) }
        }
        val resolver = context.contentResolver
        val registered = runCatching {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        }.isSuccess
        try {
            refresh.trySend(Unit)
            while (isActive) {
                refresh.receive()
                val result = withContext(Dispatchers.IO) {
                    ClockWidgetAgenda.load(context, Instant.now(), 30, config.excludedCalendars)
                }
                calendar = if (override == null) {
                    CalendarSnapshotCache.shared.complete(widgetId, config.excludedCalendars,
                        ClockWidgetAgenda.hasCalendarPermission(context), result)
                } else if (result == CalendarLoad.Unavailable && calendar is CalendarLoad.Ready) {
                    calendar
                } else result
                delay(150) // Merge provider bursts without clearing the visible agenda.
            }
        } finally {
            if (registered) resolver.unregisterContentObserver(observer)
            refresh.close()
        }
    }
    val events = (calendar as? CalendarLoad.Ready)?.events.orEmpty()
    LaunchedEffect(active, events, config.agendaLookaheadHours) {
        if (active)
            while (true) {
                now = Instant.now()
                val next =
                    AgendaRules.nextUpdate(
                        now,
                        events,
                        ZoneId.systemDefault(),
                        config.agendaLookaheadHours,
                    )
                delay(Duration.between(now, next).toMillis().coerceIn(16, 60_000))
            }
    }
    val weather =
        remember(revision, now, config.showWeather, loadWeather) {
            if (config.showWeather && loadWeather) ClockWidgetWeather.snapshot(context) else null
        }
    val reading =
        remember(revision, now, config.showWeather, loadWeather) {
            if (config.showWeather && loadWeather) ClockWidgetWeather.current(context) else null
        }
    val batteryReading =
        remember(revision, config.showBattery) {
            if (config.showBattery)
                ClockWidgetBattery.percent(context)?.let { percent ->
                    val status =
                        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val charging =
                        (status?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) ?: 0) > 0
                    percent to charging
                }
            else null
        }
    // Worded here rather than inside the remember, so a change of language is followed.
    val battery =
        batteryReading?.let { (percent, charging) ->
            stringResource(
                if (charging) dev.nikelauncher.R.string.clock_battery_charging
                else dev.nikelauncher.R.string.widget_clock_battery_percent,
                percent,
            )
        }
    return ClockLiveState(config, now, calendar, weather, reading, battery)
}
