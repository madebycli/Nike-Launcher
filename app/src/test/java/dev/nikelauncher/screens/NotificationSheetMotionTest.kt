// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.sheet.ViewportSheet
import dev.nikelauncher.ui.notifications.NotificationSheetContent
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sheet's height between the list and a notification's page is one motion: it speeds up once and
 * slows down once. Two size animations stacked on each other restart each other and show as a second
 * push halfway through, which is what this measures frame by frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class NotificationSheetMotionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun display() = pixel9Display(rule.activity)

    private fun heightsAfter(action: () -> Unit): List<Float> {
        rule.mainClock.autoAdvance = false
        action()
        return List(90) {
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithTag("notification-panel").fetchSemanticsNode().boundsInRoot.height
        }.also { rule.mainClock.autoAdvance = true }
    }

    /** How often the speed picks up again after it had started to fall, by more than [slack] px a frame. */
    private fun secondPushes(heights: List<Float>, slack: Float): Int {
        val speed = heights.zipWithNext { a, b -> kotlin.math.abs(b - a) }
        var peak = 0f
        var falling = false
        var pushes = 0
        for (i in speed.indices) {
            if (!falling && speed[i] < peak - slack) falling = true
            if (falling && i > 0 && speed[i] > speed[i - 1] + slack) pushes++
            peak = maxOf(peak, speed[i])
        }
        return pushes
    }

    /** Whether the height, once it has held still for five frames, moves again by more than [slack] px. */
    private fun movesAfterSettling(heights: List<Float>, slack: Float): Boolean {
        val start = heights.indices.firstOrNull { i ->
            i > 0 && heights[i] != heights[0] && i + 5 < heights.size &&
                (i..i + 5).all { kotlin.math.abs(heights[it] - heights[i]) < 1f }
        } ?: return false
        return heights.drop(start).any { kotlin.math.abs(it - heights[start]) > slack }
    }

    @Test fun openingAndGoingBackAreEachOneMotion() {
        val controller = FakeNotificationController(fakeNotifications(rule.activity))
        rule.setContent {
            NikeTheme {
                ViewportSheet({}, rememberModalBottomSheetState(skipPartiallyExpanded = true), "notification-panel", fillHeight = false) {
                    NotificationSheetContent(controller.items, null, controller, {})
                }
            }
        }
        rule.waitForIdle()
        val open = heightsAfter { rule.onNodeWithText("Weekend plans").performClick() }
        rule.waitForIdle()
        val back = heightsAfter { rule.onNodeWithContentDescription(rule.activity.getString(R.string.action_back)).performClick() }
        println("open  " + open.joinToString(" ") { it.toInt().toString() })
        println("back  " + back.joinToString(" ") { it.toInt().toString() })
        val slack = 2f * rule.density.density
        assertTrue("opening speeds up again halfway: $open", secondPushes(open, slack) == 0)
        assertTrue("going back speeds up again halfway: $back", secondPushes(back, slack) == 0)
        assertTrue("opening moves again after settling: $open", !movesAfterSettling(open, 2f))
        assertTrue("going back moves again after settling: $back", !movesAfterSettling(back, 2f))
        // The sheet's edge stands over the home screen: it must not dip past its content and spring back.
        assertTrue("opening overshoots: $open", open.max() <= open.last() + 2f)
        assertTrue("going back overshoots: $back", back.min() >= back.last() - 2f)
    }
}
