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
import dev.nikelauncher.data.prefs.SearchDisplayMode
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
    val mode by env.prefs.search.mode.collectAsState(SearchDisplayMode.BUTTON)
    val floating = mode == SearchDisplayMode.BUTTON
    val atBottom by env.prefs.search.buttonAtBottom.collectAsState(true)
    val hide by corner.hideSearchWhileScrolling.collectAsState(true)
    val appearance by corner.searchAppearance.collectAsState(CornerAppearance())
    var picking by remember { mutableStateOf<Boolean?>(null) }
    val hideTitle = stringResource(R.string.search_button_hide_scrolling)
    val colorsTitle = stringResource(R.string.corner_colors)
    val backgroundTitle = stringResource(R.string.corner_background_color)
    val opacityTitle = stringResource(R.string.corner_background_opacity)
    val classic = stringResource(R.string.corner_color_classic)
    val material = stringResource(R.string.corner_color_material)
    val custom = stringResource(R.string.corner_color_custom)
    val automaticTitle = stringResource(R.string.corner_auto_icon_color)
    val foregroundTitle = stringResource(R.string.corner_icon_color)

    if (picking != null) {
        val background = picking == true
        ColorPickerSheet(
            initial = Color(if (background) appearance.background else appearance.foreground),
            wallpaperColors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.tertiary, it.primaryContainer) },
            onPick = { color -> env.write { if (background) corner.setSearchBackground(color.toArgb()) else corner.setSearchForeground(color.toArgb()) } },
            onDismiss = { picking = null },
        )
    }
    return listOfNotNull(
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.search_display_mode), icon = Icons.Filled.Search) {
            ChoiceChips(SearchDisplayMode.entries, mode, { choice -> stringResource(when (choice) {
                SearchDisplayMode.NONE -> R.string.search_mode_none
                SearchDisplayMode.BUTTON -> R.string.search_mode_button
                SearchDisplayMode.BAR -> R.string.search_mode_bar
            }) }, { choice -> env.write { env.prefs.search.setMode(choice) } })
        }, "search bar button none"),
        if (floating) SettingsEntry(SECTION, settingsBlock(stringResource(R.string.search_button_position)) {
            ChoiceChips(listOf(false, true), atBottom, { stringResource(if (it) R.string.search_position_bottom else R.string.search_position_top) },
                { env.write { env.prefs.search.setButtonAtBottom(it) } })
        }, "search button position") else null,
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
        if (floating && appearance.colorMode == CornerColorMode.CUSTOM) SettingsEntry(
            SECTION, settingsSwitch(automaticTitle, appearance.automaticForeground, icon = Icons.Filled.Palette) {
                env.write { corner.setSearchAutomaticForeground(it) }
            }, "search button foreground",
        ) else null,
        if (floating && appearance.colorMode == CornerColorMode.CUSTOM && !appearance.automaticForeground) SettingsEntry(
            SECTION, settingsItem(foregroundTitle, { picking = false }, value = "#%06X".format(appearance.foreground and 0xFFFFFF), icon = Icons.Filled.Palette),
            "search button foreground",
        ) else null,
        if (floating) SettingsEntry(SECTION, settingsBlock(opacityTitle, value = "${appearance.backgroundOpacity}%", icon = Icons.Filled.Opacity) {
            SettingsSlider(appearance.backgroundOpacity.toFloat(), 0f..100f, { v -> env.write { corner.setSearchBackgroundOpacity(v.toInt()) } }, step = 5f)
        }, "search button opacity") else null,
    )
}
