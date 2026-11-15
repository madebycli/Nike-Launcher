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
        fun waitPage(id: Int) { rule.waitUntil(5000) { rule.onAllNodesWithText(rule.activity.getString(id)).fetchSemanticsNodes().isNotEmpty() }; rule.waitForIdle() }
        rule.onNodeWithText("Get started").performClick()
        waitPage(R.string.setup_access)
        rule.shoot("setup-1-access")
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.onNodeWithText("Get started").assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.settings_default_launcher)).assertExists()
        rule.shoot("setup-1-access-restored")
        listOf(R.string.widget_settings_clock_app, R.string.widget_settings_calendar_app, R.string.widget_settings_weather_app).forEachIndexed { index, id ->
            rule.onNodeWithText(rule.activity.getString(id)).performScrollTo().performClick()
            rule.shoot("setup-1-app-$index")
            rule.onAllNodesWithText("Done").onLast().performClick()
        }
        rule.onNodeWithText("Next").performClick()
        waitPage(R.string.setup_style)
        rule.shoot("setup-2-style")
        rule.onNodeWithText("Next").performClick()
        waitPage(R.string.favorites_title)
        val prefs = (rule.activity.application as NikeApp).prefs
        fun favorites() = runBlocking { prefs.library.favorites.first() }
        repeat(3) { index ->
            rule.onAllNodesWithContentDescription("Add to favorites").onFirst().performClick()
            rule.waitUntil(5000) {
                favorites().size == index + 1 && rule.onAllNodesWithContentDescription(
                    rule.activity.getString(R.string.favorites_remove),
                ).fetchSemanticsNodes().size == index + 1
            }
            rule.waitForIdle()
        }
        rule.waitForIdle()
        rule.shoot("setup-3-favorites")
        val original = favorites()
        rule.onNodeWithText(rule.activity.getString(R.string.favorites_count, 3)).assertDoesNotExist()
        rule.onAllNodesWithContentDescription("Move down").assertCountEquals(0)
        val selectedHeight = rule.onAllNodesWithText("Calendar").onLast().fetchSemanticsNode().boundsInRoot.height
        Assert.assertEquals(selectedHeight, rule.onAllNodesWithText("Contacts").onLast().fetchSemanticsNode().boundsInRoot.height, 1f)
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
        Assert.assertNotEquals(original, favorites())
        Assert.assertEquals(original.toSet(), favorites().toSet())
        rule.onNodeWithText("Next").performClick()
        waitPage(R.string.setup_ready)
        rule.shoot("setup-4-done")
    }
}
