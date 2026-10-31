// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.applist.AppListSearchSlot
import dev.nikelauncher.ui.kit.motion.*
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.WidgetMenu
import dev.nikelauncher.widget.WidgetSlotActions
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Render the production menu and measure intermediate positions with the real Material motion specs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-420dpi")
class MenuMotionScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun widgetMenuAndDirectionalMotion() {
        var page by mutableIntStateOf(0)
        var search by mutableStateOf(false)
        var bottom by mutableStateOf(false)
        var menu by mutableStateOf(false)
        val actions = WidgetSlotActions({}, {}, {}, {}, {}, {})
        rule.setContent { NikeTheme {
            Surface(Modifier.fillMaxSize()) {
                val spatial = AppMotion.offset()
                val effects = AppMotion.effects<Float>()
                Box {
                    AnimatedContent(page, transitionSpec = {
                        sharedAxisEnter(targetState > initialState, spatial, effects) togetherWith
                            sharedAxisExit(targetState > initialState, spatial, effects)
                    }) { target -> Box(Modifier.fillMaxSize().testTag("page-$target")) { Text(if (target == 0) "Home" else "Settings", style = MaterialTheme.typography.headlineLarge) } }
                    Column(Modifier.fillMaxSize()) {
                        if (bottom) Spacer(Modifier.weight(1f))
                        AppListSearchSlot(search, bottom) { TextField("", {}, modifier = Modifier.fillMaxWidth().testTag("search-field")) }
                    }
                    WidgetMenu(menu, DpOffset(20.dp, 150.dp), 42, actions, {}, { menu = false })
                }
            }
        } }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        fun motionFrames() { repeat(6) { rule.mainClock.advanceTimeByFrame(); rule.waitForIdle() } }
        rule.runOnIdle { page = 1 }
        motionFrames()
        val entering = rule.onNodeWithTag("page-1").getUnclippedBoundsInRoot().left
        Assert.assertTrue(entering.value > 0)
        rule.shoot("motion-settings-forward")
        rule.mainClock.advanceTimeBy(4000)
        rule.runOnIdle { page = 0 }
        motionFrames()
        Assert.assertTrue(rule.onNodeWithTag("page-0").getUnclippedBoundsInRoot().left.value < 0)
        rule.mainClock.advanceTimeBy(4000)
        rule.runOnIdle { search = true }
        motionFrames()
        val enteringTop = rule.onNodeWithTag("search-field").getUnclippedBoundsInRoot().top
        rule.mainClock.advanceTimeBy(4000)
        val settledTop = rule.onNodeWithTag("search-field").getUnclippedBoundsInRoot().top
        Assert.assertTrue(enteringTop < settledTop)
        rule.runOnIdle { search = false }; motionFrames(); rule.mainClock.advanceTimeBy(4000); rule.waitForIdle()
        rule.runOnIdle { bottom = true }; motionFrames()
        rule.runOnIdle { search = true }; motionFrames()
        val enteringBottom = rule.onNodeWithTag("search-field").getUnclippedBoundsInRoot().top
        rule.mainClock.advanceTimeBy(4000)
        val settledBottom = rule.onNodeWithTag("search-field").getUnclippedBoundsInRoot().top
        Assert.assertTrue(enteringBottom > settledBottom)
        rule.runOnIdle { search = false }; motionFrames()
        Assert.assertTrue(rule.onNodeWithTag("search-field").getUnclippedBoundsInRoot().top > settledBottom)
        rule.mainClock.advanceTimeBy(4000); rule.waitForIdle()
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { search = false; menu = true }
        rule.shoot("widget-expressive-menu-de")
    }
}
