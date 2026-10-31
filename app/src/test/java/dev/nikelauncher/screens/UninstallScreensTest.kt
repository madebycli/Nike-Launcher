// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.prefs.SearchDisplayMode
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercise both menu entry points; Android's uninstall confirmation itself is not rendered. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class UninstallScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun bothMenusRequestTheSelectedApp() {
        val app = rule.activity.application as NikeApp
        installFakeApps(app)
        runBlocking {
            app.prefs.home.setWelcomeSeen(true)
            app.prefs.library.addFavorite("fake.messages/fake.messages.Main")
            app.prefs.corner.setEnabled(true)
            app.prefs.corner.setTap(CornerAction.SEARCH)
            app.prefs.search.setMode(SearchDisplayMode.BUTTON)
        }
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitUntil(5000) { rule.onAllNodesWithText("Messages").fetchSemanticsNodes().isNotEmpty() }
        val uninstall = app.getString(R.string.action_uninstall)
        rule.onAllNodesWithText("Messages").onFirst().performTouchInput { longClick() }
        rule.onNodeWithText(uninstall).assertIsDisplayed()
        rule.shoot("uninstall-home-menu")
        rule.onNodeWithText(uninstall).performClick()
        val homeIntent = shadowOf(rule.activity).nextStartedActivity
        Assert.assertEquals(Intent.ACTION_DELETE, homeIntent.action)
        Assert.assertEquals("package:fake.messages", homeIntent.data.toString())
        // Starting the platform intent pauses this activity in Robolectric; return to it first.
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitUntil(5000) { rule.onAllNodesWithText("Messages").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription(app.getString(R.string.applist_search)).performClick()
        rule.waitUntil(5000) { rule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("Messages"))
        rule.onAllNodesWithText("Messages").onLast().performTouchInput { longClick() }
        rule.onNodeWithText(uninstall).assertIsDisplayed()
        rule.shoot("uninstall-app-list-menu")
        rule.onNodeWithText(uninstall).performClick()
        val listIntent = shadowOf(rule.activity).nextStartedActivity
        Assert.assertEquals(Intent.ACTION_DELETE, listIntent.action)
        Assert.assertEquals("package:fake.messages", listIntent.data.toString())
    }
}
