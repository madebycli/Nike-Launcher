// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import dev.nikelauncher.R
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.config.ClockConfigScreen
import dev.nikelauncher.widget.clockStyleGroups
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real clock settings, including every page and the editable number/date dialogs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class ClockConfigScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }

    @Test fun pages() = walkThrough("clock", 1f)

    @Test
    @Config(qualifiers = "de-w411dp-h923dp-420dpi")
    fun germanLargeText() = walkThrough("clock-de-large", 1.5f)

    private fun walkThrough(prefix: String, fontScale: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        installFakeCalendars(context as android.app.Application)
        installFakeApps(context)
        val initial = ClockWidgetConfig.read(context, 42).copy(
            showBattery = true, showWeather = true, showAgenda = true,
            showAlarm = true, showNotifications = true, datePattern = "dd.MM.yyyy",
        )
        var saved: ClockWidgetConfig? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NikeTheme { ClockConfigScreen(42, initial, { saved = it }, {}) }
            }
        }
        fun text(id: Int) = context.getString(id)
        fun open(id: Int) {
            rule.onNodeWithText(text(id)).performScrollTo().performClick()
            rule.waitForIdle()
        }
        fun back() = rule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        fun scrollShots(name: String, count: Int) {
            rule.shoot("$prefix-$name")
            repeat(count) { index ->
                rule.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 700f) }
                rule.shoot("$prefix-$name-${index + 2}")
            }
        }
        scrollShots("home", 1)
        open(R.string.widget_settings_clock)
        scrollShots("time", 1)
        open(R.string.widget_settings_clock_app)
        rule.shoot("$prefix-clock-app")
        rule.onAllNodesWithText(text(R.string.widget_settings_done)).onLast().performClick()
        open(R.string.clock_style)
        rule.shoot("$prefix-styles")
        clockStyleGroups.flatMap { it.second }.chunked(2).forEachIndexed { index, styles ->
            rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(
                hasText(text(styles.first().label)) and hasClickAction(),
            )
            rule.shoot("$prefix-styles-${index + 2}")
        }
        back()
        back()
        open(R.string.widget_settings_calendar)
        repeat(2) { rule.onNodeWithText(text(R.string.widget_settings_date)).performClick() }
        scrollShots("calendar", 3)
        open(R.string.clock_calendars)
        rule.shoot("$prefix-calendars")
        rule.onNodeWithText(text(android.R.string.cancel)).performClick()
        open(R.string.widget_settings_calendar_app)
        rule.shoot("$prefix-calendar-app")
        rule.onAllNodesWithText(text(R.string.widget_settings_done)).onLast().performClick()
        rule.onNodeWithText(text(R.string.widget_clock_date_custom_chip)).performScrollTo()
        rule.onNodeWithText(text(R.string.widget_clock_date_custom_chip)).performClick()
        rule.shoot("$prefix-custom-date")
        rule.onNodeWithText(text(R.string.action_cancel)).performClick()
        back()
        open(R.string.widget_settings_weather)
        scrollShots("weather", 2)
        open(R.string.widget_settings_weather_app)
        rule.shoot("$prefix-weather-app")
        rule.onAllNodesWithText(text(R.string.settings_quick_launch_none)).onLast().performClick()
        back()
        open(R.string.widget_settings_appearance)
        open(R.string.widget_settings_text_color)
        rule.shoot("$prefix-color")
        rule.onNodeWithText(text(R.string.action_cancel)).performClick()
        rule.shoot("$prefix-appearance")
        rule.onAllNodesWithText("60").onFirst().performScrollTo()
        rule.onAllNodesWithText("60").onFirst().performClick()
        rule.shoot("$prefix-exact-number")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("999")
        rule.onAllNodesWithText(text(R.string.widget_settings_done)).onLast().assertIsNotEnabled()
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("72")
        rule.onAllNodesWithText(text(R.string.widget_settings_done)).onLast().performClick()
        scrollShots("appearance", 9)
        restoration.emulateSavedInstanceStateRestore()
        rule.shoot("$prefix-restored")
        assertNull(saved)
        rule.onNodeWithText(text(R.string.widget_settings_done)).performClick()
        assertEquals(72, saved?.timeSizeSp)
        assertEquals(initial.datePattern, saved?.datePattern)
    }
}
