// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import dev.nikelauncher.R
import dev.nikelauncher.widget.ClockOverviewSheet
import org.junit.Before
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.ClockLiveState
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.WeatherDay
import dev.nikelauncher.widget.WeatherHour
import dev.nikelauncher.widget.WeatherMetrics
import dev.nikelauncher.widget.WeatherSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** The weather pages with invented data, at Pixel 9 size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-420dpi")
class WeatherScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun display() = pixel9Display(rule.activity)

    private fun snapshot(now: Instant): WeatherSnapshot {
        val metrics = WeatherMetrics(humidity = 62, windKph = 14f, windDirection = 240, uv = 2f, pressureHpa = 1012f, cloudCover = 40, feelsLikeK = 285)
        val hours = (0 until 36).map { WeatherHour(now.plus(it.toLong(), ChronoUnit.HOURS), 285 + (it % 12), if (it % 5 == 0) 500 else 801, (it * 7) % 90, metrics) }
        val today = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val days = (0 until 7).map {
            WeatherDay(today.plusDays(it.toLong()), 277 + it, 287 - it, if (it % 3 == 0) 500 else 801, (it * 11) % 80, metrics,
                sunrise = now.truncatedTo(ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS).plus(it.toLong(), ChronoUnit.DAYS),
                sunset = now.truncatedTo(ChronoUnit.DAYS).plus(17, ChronoUnit.HOURS).plus(it.toLong(), ChronoUnit.DAYS))
        }
        return WeatherSnapshot(
            receivedAt = now, tempK = 287, condition = "Partly cloudy", code = 801, location = "Berlin",
            sunrise = now.truncatedTo(ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS),
            sunset = now.truncatedTo(ChronoUnit.DAYS).plus(17, ChronoUnit.HOURS),
            hours = hours, days = days, metrics = metrics, rain = 20, zone = ZoneOffset.UTC,
        )
    }

    @Test fun pages() {
        val now = Instant.parse("2026-10-30T12:22:00Z")
        rule.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            val state = ClockLiveState(ClockWidgetConfig.read(context, 1), now, null, snapshot(now), null, null)
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                NikeTheme { ClockOverviewSheet(1, "weather", {}, liveStateOverride = state) }
            }
        }
        rule.shoot("weather-de-large-overview")
        for ((tag, name) in listOf("weather-open-daily" to "daily", "weather-open-hourly" to "hourly")) {
            rule.onNodeWithTag("weather-overview-list").performScrollToNode(hasTestTag(tag))
            rule.onNodeWithTag(tag).performClick()
            rule.shoot("weather-de-large-" + name)
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.action_back)).performClick()
        }
    }
}
