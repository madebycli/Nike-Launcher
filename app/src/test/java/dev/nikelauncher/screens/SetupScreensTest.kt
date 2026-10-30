// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class SetupScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }
    @Test fun walkThroughSetup() {
        installFakeApps(rule.activity)
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitForIdle()
        rule.shoot("setup-0-welcome")
        rule.onNodeWithText("Get started").performClick()
        rule.shoot("setup-1-access")
        listOf(R.string.widget_settings_clock_app, R.string.widget_settings_calendar_app, R.string.widget_settings_weather_app).forEachIndexed { index, id ->
            rule.onNodeWithText(rule.activity.getString(id)).performScrollTo().performClick()
            rule.shoot("setup-1-app-$index")
            rule.onAllNodesWithText("Done").onLast().performClick()
        }
        rule.onNodeWithText("Next").performClick()
        rule.shoot("setup-2-style")
        rule.onNodeWithText("Next").performClick()
        val prefs = (rule.activity.application as NikeApp).prefs
        fun favorites() = runBlocking { prefs.library.favorites.first() }
        repeat(3) { index ->
            rule.onAllNodesWithContentDescription("Add to favorites").onFirst().performClick()
            rule.waitUntil(5000) {
                favorites().size == index + 1 && rule.onAllNodesWithText(
                    rule.activity.getString(R.string.favorites_count, index + 1),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.waitForIdle()
        }
        rule.waitForIdle()
        rule.shoot("setup-3-favorites")
        val original = favorites()
        rule.onAllNodesWithContentDescription("Move down").onFirst().performClick()
        rule.waitUntil(5000) { favorites() == listOf(original[1], original[0], original[2]) }
        rule.shoot("setup-3-favorites-arrow")
        val handle = rule.onAllNodesWithContentDescription(rule.activity.getString(R.string.home_drag_handle)).onLast()
        rule.mainClock.autoAdvance = false
        handle.performTouchInput { down(center); advanceEventTime(700) }
        rule.mainClock.advanceTimeBy(700)
        handle.performTouchInput { moveBy(Offset(0f, -300f)) }
        rule.mainClock.advanceTimeBy(32)
        handle.performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.shoot("setup-3-favorites-drag")
        Assert.assertEquals(listOf(original[1], original[2], original[0]), favorites())
        rule.onNodeWithText("Next").performClick()
        rule.shoot("setup-4-done")
    }
}
