// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.app.KeyguardManager
import android.content.ComponentName
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.*
import dev.nikelauncher.data.prefs.SearchDisplayMode
import dev.nikelauncher.media.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real navigation: disabled-button recovery, independent placement, both notification entry surfaces. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class UserFlowScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @After fun cleanUp() { NotificationPreviewBus.disconnect() }
    @Test fun buttonPlacementSearchAndNotificationSwipes() {
        val app = rule.activity.application as NikeApp
        val prefs = app.prefs
        installFakeApps(app)
        runBlocking {
            prefs.home.setWelcomeSeen(true)
            prefs.library.addFavorite("fake.messages/fake.messages.Main")
            prefs.library.addFavorite("fake.calendar/fake.calendar.Main")
            prefs.corner.setEnabled(false)
            prefs.corner.setTap(CornerAction.SEARCH)
            prefs.home.setShortcutSwipe(ShortcutSwipe.OFF)
            prefs.appList.setNotificationPreviews(true)
            prefs.pages.setEnabled(WidgetPageSide.RIGHT, true)
        }
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", ComponentName(app, NowPlayingListenerService::class.java).flattenToString())
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setIsDeviceLocked(false)
        rule.activityRule.scenario.recreate(); pixel9Display(rule.activity); rule.waitForIdle()
        fun text(id: Int) = app.getString(id)
        fun closeSheet() {
            rule.onAllNodesWithContentDescription(text(R.string.action_close)).onLast().performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithTag("notification-panel").fetchSemanticsNodes().isEmpty() }
            rule.waitForIdle()
        }
        fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        rule.onRoot().performTouchInput { longClick(Offset(width * .5f, height * .85f)) }
        rule.onAllNodesWithText(text(R.string.action_open_settings)).onLast().performClick()
        rule.onNodeWithText(text(R.string.settings_section_home)).performClick()
        rule.onNodeWithText(text(R.string.settings_corner_button)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.corner_enabled)).performClick()
        rule.waitUntil(5000) { runBlocking { prefs.corner.settings.first().enabled } }
        rule.shoot("flow-corner-restored")
        rule.onNodeWithText(text(R.string.corner_place)).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("corner-placement").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(text(R.string.corner_enabled)).assertDoesNotExist()
        rule.shoot("flow-corner-placement")
        rule.onNodeWithTag("corner-placement-button").performTouchInput { swipe(center, center + Offset(-350f, -1100f), 600) }
        Assert.assertNull(runBlocking { prefs.corner.settings.first().position })
        rule.shoot("flow-corner-draft")
        rule.onNodeWithContentDescription(text(R.string.corner_position_reset)).performClick()
        rule.onNodeWithTag("corner-placement-button").performTouchInput { swipe(center, center + Offset(-250f, -700f), 600) }
        rule.onNodeWithContentDescription(text(R.string.action_done)).performClick()
        rule.waitUntil(5000) { runBlocking { prefs.corner.settings.first().position != null } }
        rule.shoot("flow-corner-saved")
        back(); back(); back()
        val cancelled = mutableListOf<String>()
        rule.runOnIdle {
            NotificationPreviewBus.connect { cancelled += it }
            NotificationPreviewBus.updateSnapshotIfUnchanged(app, NotificationPreviewBus.revision, fakeNotifications(app))
        }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Weekend plans").fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("flow-home-notification")
        rule.onAllNodesWithText("Weekend plans").onFirst().performTouchInput { longClick() }
        rule.onNodeWithText(text(R.string.action_edit_layout)).assertIsDisplayed()
        rule.shoot("flow-home-app-menu")
        rule.onNodeWithText(text(R.string.notifications_title)).performClick()
        rule.onNodeWithContentDescription(text(R.string.notification_clear_all)).assertIsDisplayed()
        closeSheet()
        rule.onAllNodesWithText("Weekend plans").onFirst().performTouchInput { swipeRight() }
        rule.onNodeWithText(text(R.string.notifications_title)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.notification_clear_all)).assertIsDisplayed()
        Assert.assertTrue(cancelled.isEmpty())
        rule.shoot("flow-home-notification-overview")
        rule.onAllNodesWithText("Weekend plans").onLast().performClick()
        rule.shoot("flow-home-conversation")
        closeSheet()
        // Search through the restored corner also reveals a field, rather than only bumping a focus counter.
        rule.onNodeWithContentDescription(text(R.string.applist_search)).performClick()
        rule.waitUntil(5000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("flow-search-button-bottom")
        rule.onAllNodesWithText("Camera").onLast().performScrollTo().performTouchInput { longClick() }
        rule.shoot("flow-list-app-menu")
        rule.onNodeWithText(text(R.string.applist_add_favorite)).performClick()
        rule.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("Weekend plans"))
        rule.onAllNodesWithText("Weekend plans").onLast().performTouchInput { longClick() }
        rule.shoot("flow-list-notification-app-menu")
        rule.onNodeWithText(text(R.string.notifications_title)).performClick()
        closeSheet()
        rule.shoot("flow-list-notification")
        rule.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("Weekend plans"))
        rule.onAllNodesWithText("Weekend plans").onLast().performTouchInput { swipeRight() }
        rule.onNodeWithContentDescription(text(R.string.notification_clear_all)).assertIsDisplayed()
        Assert.assertTrue(cancelled.isEmpty())
        rule.shoot("flow-list-notification-overview")
        closeSheet()
        back()
        rule.waitUntil(5000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty() }
        runBlocking { prefs.search.setButtonAtBottom(false) }
        rule.onNodeWithContentDescription(text(R.string.applist_search)).performClick()
        rule.waitUntil(5000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        val top = rule.onNode(hasSetTextAction()).getUnclippedBoundsInRoot().top
        Assert.assertTrue(top.value < 150f)
        rule.shoot("flow-search-button-top")
        runBlocking { prefs.search.setMode(SearchDisplayMode.BAR) }
        rule.shoot("flow-search-bar")
    }
}
