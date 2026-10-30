// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.FREQUENT_RANGE
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.ui.settings.labelRes
import kotlin.math.roundToInt

private val SECTION = SettingsSection.HOME

@Composable
internal fun homeEntries(env: SettingsEnv): List<SettingsEntry> {
    val library = env.prefs.library
    val home = env.prefs.home
    val appearance = env.prefs.appearance
    val source by library.favoritesSource.collectAsState(FavoritesSource.MANUAL)
    val frequentCount by library.frequentCount.collectAsState(6)
    val closeFolder by home.closeFolderOnLaunch.collectAsState(false)
    val stacked by home.widgetsStacked.collectAsState(false)
    val hideStatusBar by appearance.hideStatusBar.collectAsState(false)
    val peekSeconds by appearance.statusBarPeekSeconds.collectAsState(5)
    val dimHome by appearance.dimHomeAlpha.collectAsState(0f)
    val rotationPref by home.allowRotation.collectAsState(null)
    val rotation = rotationPref ?: (LocalConfiguration.current.smallestScreenWidthDp >= 600)
    val compactTap by appearance.compactTapArea.collectAsState(false)

    return listOf(
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_favorites_source)) {
            Column {
                ChoiceChips(FavoritesSource.entries, source, { stringResource(it.labelRes()) }, { env.write { library.setFavoritesSource(it) } })
                if (source == FavoritesSource.FREQUENT) {
                    SettingsSlider(
                        frequentCount.toFloat(),
                        FREQUENT_RANGE.first.toFloat()..FREQUENT_RANGE.last.toFloat(),
                        { env.write { library.setFrequentCount(it.toInt()) } },
                    )
                }
            }
        }, "frequent most used manual"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_close_folder), closeFolder, { env.write { home.setCloseFolderOnLaunch(it) } }, value = stringResource(R.string.settings_close_folder_detail)), "folder"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_widgets_stacked), stacked, { env.write { home.setWidgetsStacked(it) } }, value = stringResource(R.string.settings_widgets_stacked_detail)), "widgets stack below carousel"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_hide_status_bar), hideStatusBar) { env.write { appearance.setHideStatusBar(it) } }, "status bar"),
        SettingsEntry(
            SECTION,
            settingsBlock(stringResource(R.string.settings_status_bar_timeout), value = "${peekSeconds}s") {
                SettingsSlider(peekSeconds.toFloat(), 0f..30f, { env.write { appearance.setStatusBarPeekSeconds(it.roundToInt()) } })
            },
            "status bar peek",
            visible = hideStatusBar,
        ),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_dim_home), value = "${(dimHome * 100).roundToInt()}%") {
            SettingsSlider(
                value = dimHome,
                range = 0f..0.85f,
                step = 0.01f,
                onValueChange = {
                    // Rounded to whole percent, so dragging lands where the buttons do.
                    val next = (it * 100).roundToInt() / 100f
                    env.previews.showDim(next)
                    env.write { appearance.setDimHomeAlpha(next) }
                },
            )
        }, "dim wallpaper"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_allow_rotation), rotation, { env.write { home.setAllowRotation(it) } }, value = stringResource(R.string.settings_allow_rotation_detail)), "landscape rotate"),
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_corner_button), env.nav.onCornerPicker, value = env.cornerButtonLabel ?: stringResource(R.string.settings_quick_launch_none)), "button shortcut corner"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_compact_tap), compactTap, { env.write { appearance.setCompactTapArea(it) } }, value = stringResource(R.string.settings_compact_tap_detail)), "tap area accidental launch empty"),
    )
}
