// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.notifications.NotificationSheetContent
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class NotificationScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }
    @Test fun light() = walkThrough("notifications", 1f)
    @Test @Config(qualifiers = "de-w411dp-h923dp-night-420dpi")
    fun germanLargeDark() = walkThrough("notifications-de-large-dark", 1.5f)

    private fun walkThrough(prefix: String, fontScale: Float) {
        val controller = FakeNotificationController(fakeNotifications(rule.activity))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NikeTheme {
                    dev.nikelauncher.ui.kit.sheet.ViewportSheet({}, rememberModalBottomSheetState(skipPartiallyExpanded = true), "notification-panel") {
                        NotificationSheetContent(controller.items, null, controller, {})
                    }
                }
            }
        }
        fun text(id: Int) = rule.activity.getString(id)
        rule.shoot("$prefix-overview")
        rule.onNodeWithText("Weekend plans").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithContentDescription(text(R.string.notification_photo)).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("$prefix-conversation")
        val singleHeight = rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height
        rule.onNode(hasSetTextAction()).performTextReplacement("A longer reply that wraps onto several lines so the composer stays readable on a Pixel 9.")
        rule.shoot("$prefix-reply")
        val wrappedHeight = rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height
        Assert.assertTrue(wrappedHeight > singleHeight)
        rule.onNode(hasSetTextAction()).performTextReplacement(("Long reply on a Pixel 9. ").repeat(140))
        val cappedHeight = rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height
        rule.onNode(hasSetTextAction()).performTextReplacement(("Long reply on a Pixel 9. ").repeat(160))
        Assert.assertEquals(cappedHeight, rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height)
        rule.shoot("$prefix-reply-long")
        val chat = controller.items.first()
        val replacement = chat.actions.first().copy(inputs = listOf(android.app.RemoteInput.Builder("replacement").setLabel("Reply").build()))
        rule.runOnIdle { controller.items[0] = chat.copy(actions = listOf(replacement) + chat.actions.drop(1)) }
        rule.onNodeWithContentDescription(text(R.string.notification_send)).assertIsNotEnabled()
        rule.onNodeWithText("Reply").performClick()
        rule.onNodeWithContentDescription(text(R.string.notification_send)).assertIsEnabled()
        rule.onNodeWithContentDescription(text(R.string.notification_send)).performClick()
        Assert.assertEquals(1, controller.replies.size)
        rule.onNodeWithText("Great. See you both there!").performScrollTo().performTouchInput { swipeLeft() }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Great. See you both there!").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("The view from the platform.").assertExists()
        rule.shoot("$prefix-hidden-message")
        rule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        rule.onNodeWithText("Alex shared a photo").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("notification-picture").fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("$prefix-photo")
        rule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        rule.onNodeWithText("Planning meeting for next week").performClick()
        rule.shoot("$prefix-plain")
        rule.onNodeWithContentDescription(text(R.string.notification_open)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.notification_open)).assertDoesNotExist()
        rule.onNodeWithContentDescription(text(R.string.settings_more)).performClick()
        rule.onNodeWithText(text(R.string.notification_open)).assertExists()
        rule.shoot("$prefix-actions")
        rule.onNodeWithText(text(R.string.notification_dismiss)).performClick()
        Assert.assertEquals(2, controller.items.size)
    }
}
