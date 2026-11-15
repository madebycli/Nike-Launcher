// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.sheet.ViewportSheet
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

/** What the notification sheet does when its last notification goes, and when it opens with none. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class NotificationSheetBehaviourTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun display() = pixel9Display(rule.activity)

    private var closed = 0

    private fun show(controller: FakeNotificationController) = rule.setContent {
        NikeTheme {
            ViewportSheet({}, rememberModalBottomSheetState(skipPartiallyExpanded = true), "notification-panel", fillHeight = false) {
                NotificationSheetContent(controller.items, null, controller, { closed++ })
            }
        }
    }

    @Test fun clearingEverythingClosesAtOnceWithoutAnEmptyPage() {
        val controller = FakeNotificationController(fakeNotifications(rule.activity))
        show(controller)
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.notification_clear_all)).performClick()
        rule.waitForIdle()
        assertEquals(0, controller.items.size)
        assertEquals(1, closed)
        // The rows stay while the sheet slides away; the empty page is never shown.
        rule.onNodeWithText(rule.activity.getString(R.string.notification_none)).assertDoesNotExist()
    }

    @Test fun openedEmptyItStaysOpenAndSaysSo() {
        show(FakeNotificationController(emptyList()))
        rule.waitForIdle()
        assertEquals(0, closed)
        rule.onNodeWithText(rule.activity.getString(R.string.notification_none)).assertIsDisplayed()
    }
}
