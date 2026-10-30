// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import dev.nikelauncher.ui.kit.settings.ColorPickerSheet
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
    val tint by list.tint.collectAsState(null)
    val cloud by list.textCloud.collectAsState(0)
    var pickingTint by remember { mutableStateOf(false) }
    // The picker's grant lasts only as long as the import: what is kept is a copy in our own storage.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            importAppListBackground(context, uri)?.let { list.setBackgroundImage(it.toString()) }
        }
    }
    val blurTitle = stringResource(R.string.settings_app_list_effect_blur)
    val strengthTitle = stringResource(R.string.settings_app_list_effect_strength)
    val cloudTitle = stringResource(R.string.settings_app_list_text_cloud)
    val grainTitle = stringResource(R.string.settings_app_list_grain)
    val imageTitle = stringResource(R.string.settings_app_list_background_image)
    val edgesTitle = stringResource(R.string.settings_app_list_background_edges)
    val removeTitle = stringResource(R.string.action_remove)
    val none = stringResource(R.string.settings_quick_launch_none)
    val pixelTitle = stringResource(R.string.settings_app_list_effect_pixel)
    val tintTitle = stringResource(R.string.settings_app_list_tint)
    val resetTitle = stringResource(R.string.action_reset)
    if (pickingTint) {
        ColorPickerSheet(
            initial = Color(tint ?: 0xFF000000.toInt()),
            wallpaperColors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.tertiary, it.primaryContainer) },
            onPick = { color -> env.write { list.setTint(color.toArgb()) } },
            onDismiss = { pickingTint = false },
        )
    }
    return listOfNotNull(
        SettingsEntry(SECTION, settingsItem(tintTitle, { pickingTint = true }, value = tint?.let { "#%06X".format(it and 0xFFFFFF) }, icon = Icons.Filled.Palette), "tint dim color wallpaper"),
        if (tint != null) SettingsEntry(SECTION, settingsItem(resetTitle, { env.write { list.setTint(null) } }, icon = Icons.Filled.RestartAlt), "tint reset") else null,
        SettingsEntry(SECTION, settingsBlock(blurTitle, icon = Icons.Filled.BlurOn) {
            ChoiceChips(
                options = AppListBackgroundEffect.entries,
                selected = effect,
                label = { when (it) { AppListBackgroundEffect.BLUR -> blurTitle; AppListBackgroundEffect.PIXEL -> pixelTitle; AppListBackgroundEffect.NONE -> none } },
                onSelect = { env.write { list.setBackgroundEffect(it) } },
            )
        }, "blur wallpaper background"),
        if (effect != AppListBackgroundEffect.NONE) SettingsEntry(SECTION, settingsBlock(strengthTitle, value = "$strength%", icon = Icons.Filled.Tune) {
            SettingsSlider(strength.toFloat(), 0f..100f, { env.write { list.setBackgroundStrength(it.toInt()) } }, step = 5f)
        }, "blur strength") else null,
        SettingsEntry(SECTION, settingsBlock(cloudTitle, value = "$cloud%", icon = Icons.Filled.Cloud) {
            SettingsSlider(cloud.toFloat(), 0f..100f, { env.write { list.setTextCloud(it.toInt()) } }, step = 5f)
        }, "text contrast cloud halo"),
        SettingsEntry(SECTION, settingsBlock(grainTitle, value = "$grain%", icon = Icons.Filled.Grain) {
            SettingsSlider(grain.toFloat(), 0f..100f, { env.write { list.setGrain(it.toInt()) } }, step = 5f)
        }, "grain noise"),
        SettingsEntry(SECTION, settingsItem(imageTitle, { pick.launch("image/*") }, icon = Icons.Filled.Image), "image picture wallpaper background"),
        if (image != null) SettingsEntry(SECTION, settingsItem(removeTitle, { env.write { list.setBackgroundImage(null) } }, icon = Icons.Filled.Delete), "image remove") else null,
        if (image != null) SettingsEntry(SECTION, settingsSwitch(edgesTitle, edges, icon = Icons.Filled.Gradient) { env.write { list.setBackgroundEdges(it) } }, "image edges") else null,
    )
}
