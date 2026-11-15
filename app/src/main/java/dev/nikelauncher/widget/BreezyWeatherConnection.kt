// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class BreezyWeatherConnection(
    val result: BreezyWeatherProvider.Result?,
    val loading: Boolean,
    val error: BreezyWeatherProvider.Reason?,
    val refresh: () -> Unit,
    val connect: () -> Unit,
    val openApp: () -> Unit,
)

/** Read local Breezy data only while this weather surface exists. Android owns the optional grant. */
@Composable
internal fun rememberBreezyWeatherConnection(
    configuredPackage: String?,
    matchingLocation: String?,
    onOpenApp: () -> Unit,
): BreezyWeatherConnection {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val location by rememberUpdatedState(matchingLocation)
    val fallbackOpen by rememberUpdatedState(onOpenApp)
    val requests = remember(configuredPackage, owner) { Channel<Boolean>(Channel.CONFLATED) }
    var result by remember(configuredPackage) { mutableStateOf<BreezyWeatherProvider.Result?>(null) }
    var loading by remember(configuredPackage) { mutableStateOf(false) }
    var error by remember(configuredPackage) { mutableStateOf<BreezyWeatherProvider.Reason?>(null) }
    var requestedPermission by rememberSaveable(configuredPackage) { mutableStateOf<String?>(null) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requests.trySend(false)
    }
    val openApp: () -> Unit = {
        val packageName = when (val current = result) {
            is BreezyWeatherProvider.Result.Ready -> current.packageName
            is BreezyWeatherProvider.Result.PermissionRequired -> current.packageName
            else -> configuredPackage
        }
        val intent = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent != null) runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        else fallbackOpen()
    }
    val currentOpenApp by rememberUpdatedState(openApp)
    DisposableEffect(owner, requests) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) requests.trySend(false)
        }
        owner.lifecycle.addObserver(observer)
        val prefs = context.getSharedPreferences("clock_widget_weather", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Provider writes also update the cache. Only a new bridge payload needs enriching.
            if (key == "forecast" && ClockWidgetWeather.source(context) == WeatherPayloadSource.BRIDGE)
                requests.trySend(false)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        requests.trySend(false)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            requests.close()
        }
    }
    LaunchedEffect(requests) {
        for (manual in requests) {
            loading = true
            val reading = withContext(Dispatchers.IO) {
                val incoming = BreezyWeatherProvider.read(context, configuredPackage, location)
                if (incoming is BreezyWeatherProvider.Result.Ready) {
                    // A valid provider forecast can be older than the current bridge cache.
                    // Rejecting that cache replacement does not mean the provider disconnected.
                    ClockWidgetWeather.storePayload(context, incoming.payload, WeatherPayloadSource.BREEZY_PROVIDER)
                }
                incoming
            }
            result = reading
            // An automatic incompatibility/failure must not silently look like an active
            // original-data connection while the coarser bridge forecast is displayed.
            error = (reading as? BreezyWeatherProvider.Result.Unavailable)?.reason
            loading = false
            if (manual && reading == BreezyWeatherProvider.Result.Unsupported) currentOpenApp()
        }
    }
    return BreezyWeatherConnection(result, loading, error,
        refresh = { requests.trySend(true) },
        connect = {
            (result as? BreezyWeatherProvider.Result.PermissionRequired)?.let { required ->
                val activity = context.activity()
                if (requestedPermission == required.permission && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, required.permission)) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } else {
                    requestedPermission = required.permission
                    ask.launch(required.permission)
                }
            }
        }, openApp = openApp)
}

private fun Context.activity(): Activity? {
    var candidate = this
    while (candidate is ContextWrapper) {
        if (candidate is Activity) return candidate
        val wrapped = candidate.baseContext
        if (wrapped === candidate) return null
        candidate = wrapped
    }
    return candidate as? Activity
}

/** Forecast hours belong to the weather location, independently of the phone's current zone. */
internal fun weatherClock(context: Context, at: Instant, format: ClockWidgetConfig.HourFormat, zone: ZoneId): String {
    val twentyFour = when (format) {
        ClockWidgetConfig.HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockWidgetConfig.HourFormat.TWENTY_FOUR -> true
        ClockWidgetConfig.HourFormat.TWELVE -> false
    }
    val locale = context.resources.configuration.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, if (twentyFour) "HHmm" else "hm")
    return DateTimeFormatter.ofPattern(pattern, locale).format(at.atZone(zone))
}
