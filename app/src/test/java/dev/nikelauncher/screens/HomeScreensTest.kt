// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.QuickLaunchSlot
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home with favorites, edit mode, the edit-app sheet and the app list. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h923dp-420dpi")
class HomeScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    // The preferences store lives as long as the test process, so these walk-throughs each need a
    // process of their own: run one class at a time with NIKE_SCREENS=1 (see docs/SCREENS.md).
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }

    @Test fun homeEditAndSheets() {
        installFakeApps(ApplicationProvider.getApplicationContext())
        val prefs = (rule.activity.application as NikeApp).prefs
        runBlocking {
            prefs.home.setWelcomeSeen(true)
            SAMPLE_APPS.take(3).forEach { label ->
                val pkg = "fake." + label.lowercase()
                prefs.library.addFavorite("$pkg/$pkg.Main")
            }
        }
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitForIdle()

        rule.shoot("h1-home")
        rule.onAllNodesWithText("Calendar").onFirst().performTouchInput { longClick() }
        rule.waitForIdle()
        rule.shoot("h1b-app-menu")
        rule.onAllNodesWithText("Edit icon & name").onFirst().performClick()
        rule.waitForIdle()
        rule.shoot("h1c-edit-app")
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Edit app").fetchSemanticsNodes().isEmpty() }
        rule.onRoot().performTouchInput { longClick(Offset(width / 2f, height * 0.85f)) }
        rule.onAllNodesWithText("Edit layout").onFirst().performClick()
        rule.waitForIdle()
        rule.shoot("h2-edit")
        runBlocking {
            prefs.home.setSwipeUpOpensList(true)
            prefs.library.setQuickLaunch(QuickLaunchSlot.LEFT, "fake.camera/fake.camera.Main")
            prefs.library.setQuickLaunch(QuickLaunchSlot.RIGHT, "fake.messages/fake.messages.Main")
            prefs.strip.setEdgeSide(EdgeSide.BOTH)
            prefs.strip.setEdgeZoneBandOnly(false)
        }
        rule.waitForIdle()
        rule.shoot("h2-zones-full")
        runBlocking { prefs.strip.setEdgeZoneBandOnly(true) }
        rule.waitForIdle()
        rule.shoot("h2-zones-band")
        rule.onAllNodesWithText("Favorites").onFirst().performClick()
        rule.waitForIdle()
        rule.shoot("h2b-edit-selected")
        repeat(12) { rule.onNodeWithContentDescription("Move right").performClick() }
        repeat(30) { rule.onNodeWithContentDescription("Move up").performClick() }
        rule.waitForIdle()
        rule.shoot("h2c-edit-moved")
        // Still there a moment later: the layout must not snap back on its own.
        rule.mainClock.advanceTimeBy(2_000)
        rule.shoot("h3-edit-later")
        rule.onAllNodesWithContentDescription("Done").onLast().performClick()
        rule.waitForIdle()
        rule.shoot("h4-home-after-edit")
    }
}
