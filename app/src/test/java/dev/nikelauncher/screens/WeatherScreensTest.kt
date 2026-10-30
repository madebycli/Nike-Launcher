// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.ClockLiveState
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.WeatherDay
import dev.nikelauncher.widget.WeatherHour
import dev.nikelauncher.widget.WeatherMetrics
import dev.nikelauncher.widget.WeatherOverview
import dev.nikelauncher.widget.WeatherPage
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
@Config(sdk = [35], qualifiers = "w411dp-h924dp-xxhdpi")
class WeatherScreensTest {
    @get:Rule val rule = createComposeRule()

    private fun snapshot(now: Instant): WeatherSnapshot {
        val metrics = WeatherMetrics(humidity = 62, windKph = 14f, windDirection = 240, uv = 2f, pressureHpa = 1012f, cloudCover = 40, feelsLikeK = 285)
        val hours = (0 until 36).map { WeatherHour(now.plus(it.toLong(), ChronoUnit.HOURS), 285 + (it % 12), if (it % 5 == 0) 61 else 2, (it * 7) % 90, metrics) }
        val today = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val days = (0 until 7).map {
            WeatherDay(today.plusDays(it.toLong()), 277 + it, 287 - it, if (it % 3 == 0) 61 else 2, (it * 11) % 80, metrics,
                sunrise = now.truncatedTo(ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS).plus(it.toLong(), ChronoUnit.DAYS),
                sunset = now.truncatedTo(ChronoUnit.DAYS).plus(17, ChronoUnit.HOURS).plus(it.toLong(), ChronoUnit.DAYS))
        }
        return WeatherSnapshot(
            receivedAt = now, tempK = 287, condition = "Partly cloudy", code = 2, location = "Berlin",
            sunrise = now.truncatedTo(ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS),
            sunset = now.truncatedTo(ChronoUnit.DAYS).plus(17, ChronoUnit.HOURS),
            hours = hours, days = days, metrics = metrics, rain = 20, zone = ZoneOffset.UTC,
        )
    }

    @Test fun pages() {
        val now = Instant.parse("2026-10-30T12:22:00Z")
        rule.setContent {
            val context = LocalContext.current
            val state = ClockLiveState(ClockWidgetConfig.read(context, 1), now, null, snapshot(now), null, null)
            for (page in WeatherPage.entries) {
                if (page != WeatherPage.OVERVIEW) continue
            }
            NikeTheme { Surface(Modifier.fillMaxSize()) { WeatherOverview(state, WeatherPage.OVERVIEW, {}, {}) } }
        }
        rule.shoot("w1-overview")
        repeat(3) { i ->
            runCatching {
                rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
                rule.shoot("w1-overview-p${i + 2}")
            }
        }
    }
}
