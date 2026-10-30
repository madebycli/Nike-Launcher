// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAppearance
import dev.nikelauncher.data.CornerColorMode
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.ColorPickerSheet
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.APP_LIST

/** The search button that floats in the list's corner, for a list whose own search field is off. */
@Composable
internal fun searchButtonEntries(env: SettingsEnv): List<SettingsEntry> {
    val corner = env.prefs.corner
    val floating by corner.floatingSearch.collectAsState(true)
    val hide by corner.hideSearchWhileScrolling.collectAsState(true)
    val appearance by corner.searchAppearance.collectAsState(CornerAppearance())
    var picking by remember { mutableStateOf(false) }
    val floatingTitle = stringResource(R.string.search_button_floating)
    val hideTitle = stringResource(R.string.search_button_hide_scrolling)
    val colorsTitle = stringResource(R.string.corner_colors)
    val backgroundTitle = stringResource(R.string.corner_background_color)
    val opacityTitle = stringResource(R.string.corner_background_opacity)
    val classic = stringResource(R.string.corner_color_classic)
    val material = stringResource(R.string.corner_color_material)
    val custom = stringResource(R.string.corner_color_custom)

    if (picking) {
        ColorPickerSheet(
            initial = Color(appearance.background),
            wallpaperColors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.tertiary, it.primaryContainer) },
            onPick = { color -> env.write { corner.setSearchBackground(color.toArgb()) } },
            onDismiss = { picking = false },
        )
    }
    return listOfNotNull(
        SettingsEntry(SECTION, settingsSwitch(floatingTitle, floating, icon = Icons.Filled.Search) { env.write { corner.setFloatingSearch(it) } }, "search button floating"),
        if (floating) SettingsEntry(SECTION, settingsSwitch(hideTitle, hide, icon = Icons.Filled.Tune) { env.write { corner.setHideSearchWhileScrolling(it) } }, "search button scrolling") else null,
        if (floating) SettingsEntry(SECTION, settingsBlock(colorsTitle, icon = Icons.Filled.Palette) {
            ChoiceChips(
                options = CornerColorMode.entries,
                selected = appearance.colorMode,
                label = { when (it) { CornerColorMode.CLASSIC -> classic; CornerColorMode.MATERIAL -> material; CornerColorMode.CUSTOM -> custom } },
                onSelect = { env.write { corner.setSearchColorMode(it) } },
            )
        }, "search button color") else null,
        if (floating && appearance.colorMode == CornerColorMode.CUSTOM) SettingsEntry(
            SECTION,
            settingsItem(backgroundTitle, { picking = true }, value = "#%06X".format(appearance.background and 0xFFFFFF), icon = Icons.Filled.Palette),
            "search button color",
        ) else null,
        if (floating) SettingsEntry(SECTION, settingsBlock(opacityTitle, value = "${appearance.backgroundOpacity}%", icon = Icons.Filled.Opacity) {
            SettingsSlider(appearance.backgroundOpacity.toFloat(), 0f..100f, { v -> env.write { corner.setSearchBackgroundOpacity(v.toInt()) } }, step = 5f)
        }, "search button opacity") else null,
    )
}
