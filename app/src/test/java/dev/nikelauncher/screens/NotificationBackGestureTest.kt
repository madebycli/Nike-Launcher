// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.nikelauncher.R
import dev.nikelauncher.ui.notifications.NotificationSheetContent
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A notification's page follows a held back gesture, springs back when it is cancelled and leaves when it is let go. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class NotificationBackGestureTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun display() = pixel9Display(rule.activity)

    private fun width() = rule.onNodeWithText("Weekend plans").fetchSemanticsNode().boundsInRoot.width

    @Test fun thePageFollowsTheBackGesture() {
        val controller = FakeNotificationController(fakeNotifications(rule.activity))
        rule.setContent { NikeTheme { Surface { NotificationSheetContent(controller.items, null, controller, {}) } } }
        rule.onNodeWithText("Weekend plans").performClick()
        rule.mainClock.advanceTimeBy(2_000)
        val full = width()
        val dispatcher = rule.activity.onBackPressedDispatcher
        val clearAll = rule.activity.getString(R.string.notification_clear_all)

        rule.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 1200f, .5f, BackEventCompat.EDGE_LEFT)) }
        rule.waitForIdle()
        assertEquals("half a gesture shrinks the page by a twentieth", full * .95f, width(), 2f)
        rule.shoot("notification-back-gesture-half")

        rule.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals("a cancelled gesture springs back", full, width(), 1f)
        rule.onNodeWithContentDescription(clearAll).assertDoesNotExist()

        rule.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 1200f, .6f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.onNodeWithContentDescription(clearAll).assertIsDisplayed()
    }
}
