// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsHub
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.ui.settings.SettingsSectionPage
import dev.nikelauncher.ui.settings.SettingsNavigation
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The settings hub and one section page, with invented rows: what the tiles and the hero look like. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class SettingsScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }

    @Test fun light() = walk("settings")

    @Test @Config(qualifiers = "de-w411dp-h923dp-night-420dpi")
    fun germanDark() = walk("settings-de-dark")

    private fun walk(prefix: String) {
        rule.setContent {
            NikeTheme {
                var open by remember { mutableStateOf<SettingsSection?>(null) }
                var on by remember { mutableStateOf(true) }
                val rows = listOf(
                    SettingsEntry(SettingsSection.APPEARANCE, settingsSwitch("Icons on favourites", on, { on = it }, Icons.Filled.Apps)),
                    SettingsEntry(SettingsSection.APPEARANCE, settingsItem(title = "Icon pack", onClick = {}, value = "System")),
                    SettingsEntry(SettingsSection.APPEARANCE, settingsItem(title = "Font", onClick = {}, icon = Icons.Filled.Apps, value = "Google Sans Flex")).inGroup(dev.nikelauncher.R.string.settings_group_background),
                )
                val page = open
                if (page == null) {
                    SettingsHub("", {}, emptyList(), { open = it }, {}, MaterialTheme.colorScheme.surface)
                } else {
                    SettingsSectionPage(page, rows, { open = null }, MaterialTheme.colorScheme.surface)
                }
            }
        }
        rule.onNodeWithTag("settings-tile-APPEARANCE").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(2_000)
        rule.shoot("$prefix-hub")
        rule.onNodeWithTag("settings-tile-APPEARANCE").performClick()
        rule.onNodeWithText("Icon pack").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(2_000)
        rule.shoot("$prefix-page")
    }

    /** Opening a section far down the hub and going back: the page shrinks into its tile, and the hub is where it was. */
    @Test fun backReturnsToTheTile() {
        var open by mutableStateOf<SettingsSection?>(null)
        rule.setContent {
            NikeTheme {
                SettingsNavigation(
                    open = open,
                    onClose = { open = null },
                    hub = { SettingsHub("", {}, emptyList(), { open = it }, {}, MaterialTheme.colorScheme.surface) },
                    page = { section -> SettingsSectionPage(section, emptyList(), { open = null }, MaterialTheme.colorScheme.surface) },
                )
            }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.onNodeWithTag("settings-tile-ABOUT").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(2_000)
        rule.onNodeWithTag("settings-tile-ABOUT").assertDoesNotExist()
        val dispatcher = rule.activity.onBackPressedDispatcher
        // A held back gesture, half way: the page is part way back into its tile.
        rule.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 1200f, .5f, BackEventCompat.EDGE_LEFT)) }
        rule.mainClock.advanceTimeBy(500)
        rule.shoot("settings-back-gesture-half")
        // Let go: the hub is back, scrolled where it was, with the tile on screen.
        rule.runOnIdle { dispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(2_000)
        Assert.assertNull(open)
        rule.onNodeWithTag("settings-tile-ABOUT").assertIsDisplayed()
        rule.shoot("settings-back-returned")
        // A gesture drawn back leaves the page open.
        rule.onNodeWithTag("settings-tile-BACKUP").performClick()
        rule.mainClock.advanceTimeBy(2_000)
        rule.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 1200f, .3f, BackEventCompat.EDGE_LEFT)) }
        rule.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        rule.mainClock.advanceTimeBy(2_000)
        Assert.assertEquals(SettingsSection.BACKUP, open)
    }
}
