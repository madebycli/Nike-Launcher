// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.MainActivity
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The finished app at Pixel 9 size: home, then every page of the settings. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h924dp-xxhdpi")
class AppScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    // The preferences store lives as long as the test process, so these walk-throughs each need a
    // process of their own: run one class at a time with NIKE_SCREENS=1 (see docs/SCREENS.md).
    @Before fun optIn() = Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)

    private fun click(text: String) = rule.onNodeWithText(text).performClick()

    @Test fun homeAndSettings() {
        installFakeApps(ApplicationProvider.getApplicationContext())
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        click("Get started")
        repeat(3) { click("Next") }
        click("Done")
        // The flag that ends the setup is written on another thread; wait for the screen to follow it.
        rule.waitUntil(15_000) { rule.onAllNodesWithText("All set").fetchSemanticsNodes().isEmpty() }
        rule.shoot("home-0")
        rule.onRoot().performTouchInput { longClick(androidx.compose.ui.geometry.Offset(width / 2f, height * 0.85f)) }
        rule.shoot("home-1-menu")
        rule.onAllNodesWithText("Nike Launcher settings").onLast().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Appearance").fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("settings-0-top")
        listOf("Appearance", "Home screen", "App list", "A-Z strip", "Gestures", "Notifications", "Apps", "Backup", "About").forEachIndexed { index, name ->
            rule.waitUntil(10_000) { rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
            rule.onAllNodesWithText(name).onFirst().performClick()
            rule.waitForIdle()
            val tag = "settings-${index + 1}-" + name.lowercase().replace(Regex("[^a-z]+"), "-")
            rule.shoot(tag)
            repeat(4) { page ->
                runCatching {
                    rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
                    rule.shoot("$tag-p${page + 2}")
                }
            }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
    }
}
