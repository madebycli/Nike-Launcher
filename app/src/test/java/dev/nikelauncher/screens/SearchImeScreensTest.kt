// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.applist.AppListSearchSlot
import dev.nikelauncher.ui.applist.SearchField
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** Real search field and production inset padding, with a controlled keyboard inset. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-420dpi")
class SearchImeScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun bottomFieldTracksEveryInsetInBothDirections() {
        var insetPx by mutableIntStateOf(0)
        var query by mutableStateOf("")
        rule.setContent { NikeTheme { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(bottom = insetPx))) {
                Spacer(Modifier.weight(1f))
                AppListSearchSlot(visible = true, bottom = true) {
                    SearchField(query, { query = it }, Color.Black, EdgeSide.RIGHT, false, 0.dp, {},
                        atBottom = true, modifier = Modifier.testTag("real-search"))
                }
            }
        } } }
        rule.waitForIdle()
        val base = rule.onNodeWithTag("real-search").getUnclippedBoundsInRoot().bottom.value
        val density = rule.density.density
        fun check(inset: Int) {
            rule.runOnIdle { insetPx = inset }
            rule.waitForIdle()
            val actual = rule.onNodeWithTag("real-search").getUnclippedBoundsInRoot().bottom.value
            Assert.assertTrue("inset=$inset base=$base actual=$actual", abs((base - actual) * density - inset) < 2.5f)
        }
        for (inset in 0..720 step 30) check(inset)
        rule.shoot("search-bottom-simulated-ime")
        rule.runOnIdle { query = "test" }
        for (inset in 720 downTo 0 step 30) check(inset)
        rule.onNodeWithText("test").assertExists()
        rule.shoot("search-bottom-simulated-closed")
    }
}
