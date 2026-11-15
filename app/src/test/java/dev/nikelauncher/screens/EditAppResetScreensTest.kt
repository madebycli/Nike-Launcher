// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.MainActivity
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.prefs.SearchDisplayMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class EditAppResetScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun homeNameAndIconResetsPersistAfterNavigation() {
        val app = rule.activity.application as NikeApp
        val key = "fake.messages/fake.messages.Main"
        installFakeApps(app)
        val iconFile = java.io.File(app.cacheDir, "test-reset-icon.png")
        iconFile.outputStream().use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val iconUri = Uri.fromFile(iconFile).toString()
        runBlocking {
            app.prefs.home.setWelcomeSeen(true)
            app.prefs.library.addFavorite(key)
            app.prefs.library.setNameOverride(key, "Custom messages")
            app.prefs.library.setIconOverride(key, iconUri)
            app.prefs.corner.setEnabled(true)
            app.prefs.corner.setTap(CornerAction.SEARCH)
            app.prefs.search.setMode(SearchDisplayMode.BUTTON)
        }
        rule.activityRule.scenario.recreate(); pixel9Display(rule.activity)
        fun text(id: Int) = app.getString(id)
        fun openEdit(label: String) {
            rule.waitUntil(5000) { rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            rule.onAllNodesWithText(label).onFirst().performTouchInput { longClick() }
            rule.onNodeWithText(text(R.string.action_edit_icon_and_name)).performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithText(text(R.string.app_edit_reset_name)).fetchSemanticsNodes().isNotEmpty() }
        }
        openEdit("Custom messages")
        rule.shoot("reset-app-name-sheet")
        rule.onNodeWithText(text(R.string.app_edit_reset_name)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.library.nameOverrides.first()[key] == null } }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Messages").fetchSemanticsNodes().isNotEmpty() }
        openEdit("Messages")
        rule.onNodeWithContentDescription(text(R.string.action_change_icon)).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText(text(R.string.icon_picker_reset)).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("reset-app-icon-picker")
        rule.onNodeWithText(text(R.string.icon_picker_reset)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.library.iconOverrides.first()[key] == null } }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Messages").fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate(); pixel9Display(rule.activity)
        Assert.assertNull(runBlocking { app.prefs.library.nameOverrides.first()[key] })
        Assert.assertNull(runBlocking { app.prefs.library.iconOverrides.first()[key] })
        runBlocking {
            app.prefs.library.setNameOverride(key, "List messages")
            app.prefs.library.setIconOverride(key, iconUri)
        }
        rule.waitUntil(5000) { rule.onAllNodesWithText("List messages").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription(text(R.string.applist_search)).performClick()
        rule.waitUntil(5000) { rule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("List messages"))
        rule.onAllNodesWithText("List messages").onLast().performTouchInput { longClick() }
        rule.onNodeWithText(text(R.string.action_edit_icon_and_name)).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText(text(R.string.app_edit_reset_name)).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("reset-list-name-sheet")
        rule.onNodeWithText(text(R.string.app_edit_reset_name)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.library.nameOverrides.first()[key] == null } }
        rule.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText("Messages"))
        rule.onAllNodesWithText("Messages").onLast().performTouchInput { longClick() }
        rule.onNodeWithText(text(R.string.action_edit_icon_and_name)).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText(text(R.string.app_edit_reset_name)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription(text(R.string.action_change_icon)).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText(text(R.string.icon_picker_reset)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(text(R.string.icon_picker_reset)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.library.iconOverrides.first()[key] == null } }
    }
}
