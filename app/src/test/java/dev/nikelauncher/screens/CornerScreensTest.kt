// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.CornerColorMode
import dev.nikelauncher.ui.home.corner.CornerSettingsSheet
import dev.nikelauncher.ui.theme.NikeTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-420dpi")
class CornerScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }
    @Test fun germanLargePreviewAndControls() {
        val prefs = (rule.activity.application as NikeApp).prefs.corner
        runBlocking { prefs.setColorMode(CornerColorMode.CUSTOM); prefs.setAutomaticForeground(false) }
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                NikeTheme { CornerSettingsSheet(null, Color.White, {}, {}) }
            }
        }
        rule.waitUntil(5000) { rule.onAllNodesWithText(rule.activity.getString(dev.nikelauncher.R.string.corner_title)).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("corner-de-large")
        repeat(7) { index ->
            rule.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 700f) }
            rule.onNodeWithText(rule.activity.getString(dev.nikelauncher.R.string.corner_title)).assertIsDisplayed()
            rule.shoot("corner-de-large-${index + 2}")
        }
    }
}
