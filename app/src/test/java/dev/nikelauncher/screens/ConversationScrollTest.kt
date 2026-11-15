// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import dev.nikelauncher.media.PreviewMessage
import dev.nikelauncher.ui.kit.sheet.ViewportSheet
import dev.nikelauncher.ui.notifications.NotificationSheetContent
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A long conversation in the sheet must scroll back through its older messages with a real swipe. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class ConversationScrollTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun display() = pixel9Display(rule.activity)

    @Test fun longConversationScrollsBackToItsOldestMessage() {
        val now = System.currentTimeMillis()
        val messages = (1..40).map { PreviewMessage("Alex", "Message %02d".format(it), now - (41 - it) * 60_000L) }
        val preview = fakeNotifications(rule.activity).first().copy(messages = messages)
        val controller = FakeNotificationController(listOf(preview))
        rule.setContent {
            NikeTheme {
                ViewportSheet({}, rememberModalBottomSheetState(skipPartiallyExpanded = true), "notification-panel", fillHeight = false) {
                    NotificationSheetContent(controller.items, preview.key, controller, {})
                }
            }
        }
        rule.onNodeWithText("Message 40").assertIsDisplayed()
        Assert.assertFalse("the oldest message starts out of view", shown("Message 01"))
        repeat(12) {
            if (!shown("Message 01")) {
                rule.onNodeWithTag("conversation-history").performTouchInput { swipeDown(durationMillis = 250) }
                rule.waitForIdle()
            }
        }
        rule.onNodeWithText("Message 01").assertIsDisplayed()
    }

    /** Off-screen nodes stay in the tree of a scroll column, so "displayed" is what counts here. */
    private fun shown(text: String): Boolean =
        runCatching { rule.onNodeWithText(text).assertIsDisplayed() }.isSuccess
}
