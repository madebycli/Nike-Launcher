// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.WidgetPageSide
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class WidgetPageEdgeScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }

    @Test fun bothEdgesOpenOnlyOnHomePage() {
        val app = rule.activity.application as NikeApp
        installFakeApps(app)
        runBlocking {
            app.prefs.home.setWelcomeSeen(true)
            app.prefs.pages.setEnabled(WidgetPageSide.LEFT, true)
            app.prefs.pages.setEnabled(WidgetPageSide.RIGHT, true)
            app.prefs.strip.setEdgeSide(EdgeSide.BOTH)
            app.prefs.strip.setEdgeZoneInsetAuto(false)
            app.prefs.strip.setEdgeZoneInsetDp(0)
            app.prefs.strip.setEdgeZoneBandOnly(false)
        }
        rule.activityRule.scenario.recreate(); pixel9Display(rule.activity); rule.waitForIdle()
        fun open() = ReflectionHelpers.getField<Boolean>(rule.activity, "currentAppListVisible")
        fun edge(right: Boolean) = rule.onRoot().performTouchInput {
            click(Offset(if (right) width - 8f else 8f, height * .5f))
        }
        fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        for (right in listOf(false, true)) {
            edge(right); rule.waitUntil(5000) { open() }; back(); rule.waitUntil(5000) { !open() }
        }
        for (side in listOf(WidgetPageSide.LEFT, WidgetPageSide.RIGHT)) {
            rule.onRoot().performTouchInput {
                val start = if (side == WidgetPageSide.LEFT) width * .4f else width * .6f
                val end = if (side == WidgetPageSide.LEFT) width * .85f else width * .15f
                swipe(Offset(start, height * .5f), Offset(end, height * .5f), 350)
            }
            rule.waitForIdle()
            rule.shoot(if (side == WidgetPageSide.LEFT) "widget-left-before-edge" else "widget-right-before-edge")
            val label = app.getString(if (side == WidgetPageSide.LEFT) R.string.widget_pages_left else R.string.widget_pages_right)
            rule.waitUntil(5000) {
                runCatching { rule.onNodeWithContentDescription(label).assertIsDisplayed() }.isSuccess
            }
            rule.waitForIdle()
            for (right in listOf(false, true)) { edge(right); rule.waitForIdle(); Assert.assertFalse(open()) }
            rule.shoot(if (side == WidgetPageSide.LEFT) "widget-left-edges" else "widget-right-edges")
            back()
        }
    }
}
