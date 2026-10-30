// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.IconPackRepository
import dev.nikelauncher.data.Prefs
import dev.nikelauncher.data.QuickLaunchSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Where a settings row can send the user. */
class SettingsNav(
    val onHiddenApps: () -> Unit,
    val onFrequentExcluded: () -> Unit,
    val onFavorites: () -> Unit,
    val onQuickLaunchPicker: (QuickLaunchSlot) -> Unit,
    val onCornerPicker: () -> Unit,
)

/** What a settings row can ask the rest of the app to do. */
class SettingsActions(
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenAppInfo: () -> Unit,
    val onOpenHomeSettings: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onPickFontFile: (android.net.Uri) -> Unit,
)

/** Everything the sections of the settings screen share, so each section function takes one argument. */
class SettingsEnv(
    val prefs: Prefs,
    private val scope: CoroutineScope,
    val previews: SettingsPreviews,
    val nav: SettingsNav,
    val actions: SettingsActions,
    val iconPacks: List<IconPackRepository.IconPackInfo>,
    val previewApp: AppInfo?,
    val hiddenCount: Int,
    val isDefaultLauncher: Boolean,
    val listenerEnabled: Boolean,
    val shadeGestureReady: Boolean,
    val lockGestureReady: Boolean,
    val quickLaunchLeftLabel: String?,
    val quickLaunchRightLabel: String?,
    val cornerButtonLabel: String?,
) {
    /** Writes a setting off the main thread. */
    fun write(block: suspend () -> Unit) {
        scope.launch { block() }
    }
}
