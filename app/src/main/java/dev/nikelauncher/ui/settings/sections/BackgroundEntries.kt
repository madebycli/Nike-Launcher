// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.AppListBackgroundEffect
import dev.nikelauncher.data.importAppListBackground
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import kotlinx.coroutines.launch

private val SECTION = SettingsSection.APP_LIST

/** What the wallpaper does behind the list: blurred by the system or by a picture of the user's choice, and grain over it. */
@Composable
internal fun backgroundEntries(env: SettingsEnv): List<SettingsEntry> {
    val list = env.prefs.appList
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val effect by list.backgroundEffect.collectAsState(AppListBackgroundEffect.NONE)
    val strength by list.backgroundStrength.collectAsState(50)
    val grain by list.grain.collectAsState(0)
    val edges by list.backgroundEdges.collectAsState(false)
    val image by list.backgroundImage.collectAsState(null)
    // The picker's grant lasts only as long as the import: what is kept is a copy in our own storage.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            importAppListBackground(context, uri)?.let { list.setBackgroundImage(it.toString()) }
        }
    }
    val blurTitle = stringResource(R.string.settings_app_list_effect_blur)
    val strengthTitle = stringResource(R.string.settings_app_list_effect_strength)
    val grainTitle = stringResource(R.string.settings_app_list_grain)
    val imageTitle = stringResource(R.string.settings_app_list_background_image)
    val edgesTitle = stringResource(R.string.settings_app_list_background_edges)
    val removeTitle = stringResource(R.string.action_remove)
    val none = stringResource(R.string.settings_quick_launch_none)
    return listOfNotNull(
        SettingsEntry(SECTION, settingsBlock(blurTitle) {
            ChoiceChips(
                options = listOf(AppListBackgroundEffect.NONE, AppListBackgroundEffect.BLUR),
                selected = effect,
                label = { if (it == AppListBackgroundEffect.BLUR) blurTitle else none },
                onSelect = { env.write { list.setBackgroundEffect(it) } },
            )
        }, "blur wallpaper background"),
        if (effect == AppListBackgroundEffect.BLUR) SettingsEntry(SECTION, settingsBlock(strengthTitle, value = "$strength%") {
            SettingsSlider(strength.toFloat(), 0f..100f, { env.write { list.setBackgroundStrength(it.toInt()) } }, step = 5f)
        }, "blur strength") else null,
        SettingsEntry(SECTION, settingsBlock(grainTitle, value = "$grain%") {
            SettingsSlider(grain.toFloat(), 0f..100f, { env.write { list.setGrain(it.toInt()) } }, step = 5f)
        }, "grain noise"),
        SettingsEntry(SECTION, settingsItem(imageTitle, { pick.launch("image/*") }), "image picture wallpaper background"),
        if (image != null) SettingsEntry(SECTION, settingsItem(removeTitle, { env.write { list.setBackgroundImage(null) } }), "image remove") else null,
        if (image != null) SettingsEntry(SECTION, settingsSwitch(edgesTitle, edges) { env.write { list.setBackgroundEdges(it) } }, "image edges") else null,
    )
}
