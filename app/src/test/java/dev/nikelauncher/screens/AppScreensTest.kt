// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.CornerColorMode
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performSemanticsAction
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
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class AppScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    // The preferences store lives as long as the test process, so these walk-throughs each need a
    // process of their own: run one class at a time with NIKE_SCREENS=1 (see docs/SCREENS.md).
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }

    private fun click(text: String) = rule.onNodeWithText(text).performClick()

    @Test fun homeAndSettings() {
        installFakeApps(ApplicationProvider.getApplicationContext())
        val corner = (rule.activity.application as NikeApp).prefs.corner
        runBlocking {
            (rule.activity.application as NikeApp).prefs.home.setWelcomeSeen(true)
            corner.setSearchColorMode(CornerColorMode.CUSTOM)
            corner.setSearchAutomaticForeground(false)
            corner.setSearchForeground(0xFF223344.toInt())
        }
        rule.activityRule.scenario.recreate()
        pixel9Display(rule.activity)
        rule.waitForIdle()
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
            var page = 2
            while (page <= 24) {
                val scrollers = rule.onAllNodes(hasScrollAction())
                val node = scrollers.fetchSemanticsNodes().firstOrNull() ?: break
                val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
                if (range.value() >= range.maxValue()) break
                scrollers.onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 900f) }
                rule.shoot("$tag-p${page++}")
            }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
    }
}
