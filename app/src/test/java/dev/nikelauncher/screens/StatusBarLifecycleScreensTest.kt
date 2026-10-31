// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.service.StatusBarFader
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-night-420dpi")
@Suppress("DEPRECATION")
class StatusBarLifecycleScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun restoresHiddenHomeAcrossScreenOffAndRecreation() {
        val prefs = (rule.activity.application as NikeApp).prefs
        runBlocking {
            prefs.home.setWelcomeSeen(true)
            prefs.appearance.setHideStatusBar(true)
            prefs.appearance.setHideStatusBarAppList(false)
            prefs.appearance.setStatusBarPeekSeconds(0)
        }
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitUntil(5000) { ReflectionHelpers.getField<Boolean>(rule.activity, "statusBarInitialized") }
        Assert.assertEquals(false, ReflectionHelpers.getField<Boolean?>(rule.activity, "desiredStatusBarVisible"))
        Assert.assertNotEquals(0, rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        rule.shoot("home-statusbar-hidden")
        // An app-list preference may currently expose the bar. Screen-off still resets home.
        rule.runOnIdle { StatusBarFader.setVisible(rule.activity.window, visible = true, instant = true) }
        Assert.assertEquals(0, rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        rule.activity.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        rule.waitForIdle()
        Assert.assertNotEquals(0, rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitUntil(5000) { ReflectionHelpers.getField<Boolean>(rule.activity, "statusBarInitialized") }
        Assert.assertEquals(false, ReflectionHelpers.getField<Boolean?>(rule.activity, "desiredStatusBarVisible"))
        Assert.assertNotEquals(0, rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        rule.shoot("home-statusbar-after-recreate")
        runBlocking { prefs.appearance.setHideStatusBar(false) }
        rule.waitUntil(5000) { ReflectionHelpers.getField<Boolean?>(rule.activity, "desiredStatusBarVisible") == true }
        Assert.assertEquals(0, rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    @Test fun hiddenAppListDoesNotDisableKeyboardResize() {
        val app = rule.activity.application as NikeApp
        runBlocking {
            app.prefs.home.setWelcomeSeen(true)
            app.prefs.appearance.setHideStatusBar(true)
            app.prefs.appearance.setHideStatusBarAppList(true)
            app.prefs.appearance.setStatusBarPeekSeconds(0)
            app.prefs.corner.setEnabled(true)
            app.prefs.corner.setTap(CornerAction.SEARCH)
        }
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitUntil(5000) { ReflectionHelpers.getField<Boolean>(rule.activity, "statusBarInitialized") }
        rule.waitUntil(5000) { rule.onAllNodesWithContentDescription(app.getString(R.string.applist_search)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription(app.getString(R.string.applist_search)).performClick()
        rule.waitUntil(5000) { ReflectionHelpers.getField<Boolean>(rule.activity, "currentAppListVisible") }
        rule.waitUntil(5000) { rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0 }
        Assert.assertEquals(false, ReflectionHelpers.getField<Boolean?>(rule.activity, "desiredStatusBarVisible"))
        rule.shoot("app-list-statusbar-hidden")
        rule.activity.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        rule.waitUntil(5000) { rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0 }
    }

}
