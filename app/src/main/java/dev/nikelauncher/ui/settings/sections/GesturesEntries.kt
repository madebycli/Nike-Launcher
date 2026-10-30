// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.QuickLaunchSlot
import dev.nikelauncher.data.ShortcutSwipe
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.ui.settings.labelRes

private val SECTION = SettingsSection.GESTURES

@Composable
internal fun gesturesEntries(env: SettingsEnv): List<SettingsEntry> {
    val home = env.prefs.home
    val haptics by env.prefs.strip.hapticsEnabled.collectAsState(true)
    val swipeUp by home.swipeUpOpensList.collectAsState(false)
    val shortcutSwipe by home.shortcutSwipe.collectAsState(ShortcutSwipe.RIGHT)
    val doubleTap by home.doubleTapToLock.collectAsState(false)

    val none = stringResource(R.string.settings_quick_launch_none)
    return listOf(
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_haptics), haptics, { env.write { env.prefs.strip.setHapticsEnabled(it) } }, value = stringResource(R.string.settings_haptics_detail)), "vibrate"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_swipe_up_list), swipeUp, { env.write { home.setSwipeUpOpensList(it) } }, value = stringResource(R.string.settings_swipe_up_list_detail)), "swipe up"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_swipe_shortcuts), value = stringResource(R.string.settings_swipe_shortcuts_detail)) {
            ChoiceChips(ShortcutSwipe.entries, shortcutSwipe, { stringResource(it.labelRes()) }, { env.write { home.setShortcutSwipe(it) } })
        }, "swipe shortcuts"),
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_quick_launch_left), { env.nav.onQuickLaunchPicker(QuickLaunchSlot.LEFT) }, value = env.quickLaunchLeftLabel ?: none), "swipe launch"),
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_quick_launch_right), { env.nav.onQuickLaunchPicker(QuickLaunchSlot.RIGHT) }, value = env.quickLaunchRightLabel ?: none), "swipe launch"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_double_tap_lock), doubleTap, { env.write { home.setDoubleTapToLock(it) } }, value = stringResource(R.string.settings_double_tap_lock_detail)), "lock screen"),
        // Switching it on does nothing at all without the permission, so the way to grant it
        // belongs right here rather than in a toast later.
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_lock_needs_accessibility)) { AccessibilityButtons(env) }, "lock accessibility", visible = doubleTap && !env.lockGestureReady),
        SettingsEntry(
            SECTION,
            settingsBlock(
                stringResource(R.string.settings_shade_gesture),
                value = stringResource(if (env.shadeGestureReady) R.string.settings_shade_ready else R.string.settings_shade_not_ready),
            ) { if (!env.shadeGestureReady) AccessibilityButtons(env) },
            "notification shade pull",
        ),
    )
}

/**
 * Getting to the accessibility toggle, and to the screen that unblocks it: Android 13 and later
 * refuse to switch it on for an app installed outside a store until it is allowed from the app's
 * own info screen.
 */
@Composable
private fun AccessibilityButtons(env: SettingsEnv) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = env.actions.onOpenAppInfo) { Text(stringResource(R.string.settings_app_info)) }
        Button(onClick = env.actions.onOpenAccessibilitySettings) { Text(stringResource(R.string.settings_enable)) }
    }
}
