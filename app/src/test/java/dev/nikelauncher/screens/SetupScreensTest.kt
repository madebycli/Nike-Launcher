// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.nikelauncher.MainActivity
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h924dp-xxhdpi")
class SetupScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    // The preferences store lives as long as the test process, so these walk-throughs each need a
    // process of their own: run one class at a time with NIKE_SCREENS=1 (see docs/SCREENS.md).
    @Before fun optIn() = Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)

    @Test fun walkThroughSetup() {
        installFakeApps(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.shoot("setup-0-welcome")
        rule.onNodeWithText("Get started").performClick()
        rule.shoot("setup-1-access")
        rule.onNodeWithText("Next").performClick()
        rule.shoot("setup-2-style")
        rule.onNodeWithText("Next").performClick()
        rule.shoot("setup-3-favorites")
        rule.onNodeWithText("Next").performClick()
        rule.shoot("setup-4-done")
    }
}
