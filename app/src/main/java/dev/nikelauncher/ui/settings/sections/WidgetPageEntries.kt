// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.WidgetPageSide
import dev.nikelauncher.data.WidgetPagesConfig
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.HOME

/** The pages of widgets either side of the home screen, and whether they dim the wallpaper. */
@Composable
internal fun widgetPageEntries(env: SettingsEnv): List<SettingsEntry> {
    val pages = env.prefs.pages
    val config by pages.config.collectAsState(WidgetPagesConfig())
    val leftTitle = stringResource(R.string.widget_pages_left)
    val rightTitle = stringResource(R.string.widget_pages_right)
    val dimTitle = stringResource(R.string.widget_pages_dim)
    val strengthTitle = stringResource(R.string.widget_pages_dim_strength)
    val anyPage = config.leftEnabled || config.rightEnabled
    return listOfNotNull(
        SettingsEntry(SECTION, settingsSwitch(leftTitle, config.leftEnabled, icon = Icons.Filled.ViewCarousel) { env.write { pages.setEnabled(WidgetPageSide.LEFT, it) } }, "widget page left"),
        SettingsEntry(SECTION, settingsSwitch(rightTitle, config.rightEnabled, icon = Icons.Filled.ViewCarousel) { env.write { pages.setEnabled(WidgetPageSide.RIGHT, it) } }, "widget page right"),
        if (anyPage) SettingsEntry(SECTION, settingsSwitch(dimTitle, config.dimWallpaper, icon = Icons.Filled.Contrast) { env.write { pages.setDimEnabled(it) } }, "widget page dim wallpaper") else null,
        if (anyPage && config.dimWallpaper) SettingsEntry(SECTION, settingsBlock(strengthTitle, value = "${(config.dimAlpha * 100).toInt()}%", icon = Icons.Filled.Tune) {
            SettingsSlider(config.dimAlpha * 100f, 0f..85f, { v -> env.write { pages.setDimAlpha(v / 100f) } }, step = 5f)
        }, "widget page dim strength") else null,
    )
}
