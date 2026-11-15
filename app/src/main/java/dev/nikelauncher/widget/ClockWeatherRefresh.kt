// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val weatherRefreshLock = Mutex()

/** Load an already-authorized local provider when the visible clock has no fresh reading. */
@Composable
internal fun RefreshClockWeather(enabled: Boolean, configuredPackage: String?) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(enabled, configuredPackage) {
        if (!enabled) return@LaunchedEffect
        while (isActive) {
            withContext(Dispatchers.IO) {
                weatherRefreshLock.withLock {
                    if (refreshClockWeatherIfMissing(context, configuredPackage)) {
                        ClockWidgetProvider.requestRenderAll(context)
                    }
                }
            }
            delay(15 * 60 * 1000L)
        }
    }
}

/** Run on an IO dispatcher; a denied or unavailable provider leaves the cache untouched. */
internal fun refreshClockWeatherIfMissing(
    context: android.content.Context,
    configuredPackage: String?,
    read: (android.content.Context, String?) -> BreezyWeatherProvider.Result = { ctx, pkg ->
        BreezyWeatherProvider.read(ctx, pkg)
    },
): Boolean {
    if (ClockWidgetWeather.current(context) != null) return false
    val result = read(context, configuredPackage)
    return result is BreezyWeatherProvider.Result.Ready &&
        ClockWidgetWeather.storePayload(context, result.payload, WeatherPayloadSource.BREEZY_PROVIDER)
}
