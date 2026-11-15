// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClockWeatherRefreshTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before fun reset() { context.getSharedPreferences("clock_widget_weather", Context.MODE_PRIVATE).edit().clear().commit() }
    @After fun cleanup() = reset()

    @Test fun missingWeatherLoadsWithoutOpeningDetailsAndFreshCacheAvoidsAnotherRead() {
        var reads = 0
        val payload = """{"currentTemp":293,"timestamp":${System.currentTimeMillis() / 1000}}"""
        val read: (Context, String?) -> BreezyWeatherProvider.Result = { _, _ ->
            reads++
            BreezyWeatherProvider.Result.Ready(payload, "Home", "1", "org.breezyweather")
        }
        assertTrue(refreshClockWeatherIfMissing(context, "org.breezyweather", read))
        assertEquals(293, ClockWidgetWeather.current(context)?.tempK)
        assertFalse(refreshClockWeatherIfMissing(context, "org.breezyweather", read))
        assertEquals(1, reads)
    }

    @Test fun permissionRequiredDoesNotWriteCache() {
        val refreshed = refreshClockWeatherIfMissing(context, null) { _, _ ->
            BreezyWeatherProvider.Result.PermissionRequired("org.breezyweather", "permission")
        }
        assertFalse(refreshed)
        assertNull(ClockWidgetWeather.current(context))
    }
}
