// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.RECENT_SECTION_RANGE
import dev.nikelauncher.data.SECTION_TOP_RANGE
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import kotlin.math.roundToInt

private val SECTION = SettingsSection.APP_LIST

@Composable
internal fun appListEntries(env: SettingsEnv): List<SettingsEntry> {
    val list = env.prefs.appList
    val appearance = env.prefs.appearance
    val library = env.prefs.library
    val home = env.prefs.home
    val mode by env.prefs.search.mode.collectAsState(dev.nikelauncher.data.prefs.SearchDisplayMode.BUTTON)
    val search = mode != dev.nikelauncher.data.prefs.SearchDisplayMode.NONE
    val autoKeyboard by list.autoKeyboard.collectAsState(false)
    val searchBottom by list.appListSearchBottom.collectAsState(false)
    val searchHidden by list.appListSearchHidden.collectAsState(false)
    val headers by list.showListHeaders.collectAsState(true)
    val settingsRow by home.showSettingsRow.collectAsState(true)
    val sortByUsage by library.sortByUsage.collectAsState(false)
    val webSearch by list.webSearchFallback.collectAsState(false)
    val swipeUpList by home.swipeUpOpensList.collectAsState(false)
    val swipeUpSearch by list.swipeUpOpensSearch.collectAsState(false)
    val stayPrivate by list.stayAfterPrivateUnlock.collectAsState(false)
    val privateSection by list.privateSpaceSection.collectAsState(false)
    val foldersInList by list.foldersInAppList.collectAsState(false)
    val recent by list.recentSection.collectAsState(false)
    val recentCount by list.recentSectionCount.collectAsState(5)
    val markWork by appearance.markWorkApps.collectAsState(true)
    val lastLetter by list.lastLetterToLine.collectAsState(false)
    val sectionTop by list.sectionTopPercent.collectAsState(26)
    val hideStatusBar by appearance.hideStatusBarAppList.collectAsState(false)
    val dim by appearance.dimWallpaperAlpha.collectAsState(0.35f)

    return listOf(
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_auto_keyboard), autoKeyboard, { env.write { list.setAutoKeyboard(it) } }, value = stringResource(R.string.settings_auto_keyboard_detail), icon = Icons.Filled.Keyboard), "keyboard search", visible = search),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_search_bar_bottom), searchBottom, icon = Icons.Filled.VerticalAlignBottom) { env.write { list.setAppListSearchBottom(it) } }, "search position", visible = mode == dev.nikelauncher.data.prefs.SearchDisplayMode.BAR),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_search_hidden), searchHidden, { env.write { list.setAppListSearchHidden(it) } }, value = stringResource(R.string.settings_search_hidden_detail), icon = Icons.Filled.VisibilityOff), "search hidden", visible = search),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_web_search), webSearch, { env.write { list.setWebSearchFallback(it) } }, value = stringResource(R.string.settings_web_search_detail), icon = Icons.Filled.Language), "search web browser"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_swipe_up_search), swipeUpSearch, { env.write { list.setSwipeUpOpensSearch(it) } }, value = stringResource(R.string.settings_swipe_up_search_detail), icon = Icons.Filled.SwipeUp), "swipe up search keyboard", visible = swipeUpList),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_list_headers), headers, { env.write { list.setShowListHeaders(it) } }, value = stringResource(R.string.settings_list_headers_detail), icon = Icons.Filled.Title), "letters headings"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_settings_row), settingsRow, { env.write { home.setShowSettingsRow(it) } }, value = stringResource(R.string.settings_settings_row_detail), icon = Icons.Filled.Settings), "settings shortcut row"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_sort_by_usage), sortByUsage, { env.write { library.setSortByUsage(it) } }, value = stringResource(R.string.settings_sort_by_usage_detail), icon = Icons.Filled.TrendingUp), "frequent order"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_stay_private_unlock), stayPrivate, { env.write { list.setStayAfterPrivateUnlock(it) } }, value = stringResource(R.string.settings_stay_private_unlock_detail), icon = Icons.Filled.Lock), "private space unlock stay list"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_private_space_section), privateSection, { env.write { list.setPrivateSpaceSection(it) } }, value = stringResource(R.string.settings_private_space_section_detail), icon = Icons.Filled.Lock), "private space profile section"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_folders_in_list), foldersInList, { env.write { list.setFoldersInAppList(it) } }, value = stringResource(R.string.settings_folders_in_list_detail), icon = Icons.Filled.Folder), "folders groups"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_recent_section), recent, { env.write { list.setRecentSection(it) } }, value = stringResource(R.string.settings_recent_section_detail), icon = Icons.Filled.History), "recently installed new apps"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_recent_section_count), value = recentCount.toString(), icon = Icons.Filled.Numbers) {
            SettingsSlider(recentCount.toFloat(), RECENT_SECTION_RANGE.first.toFloat()..RECENT_SECTION_RANGE.last.toFloat(), { env.write { list.setRecentSectionCount(it.toInt()) } })
        }, "recently installed new apps", visible = recent),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_mark_work_apps), markWork, { env.write { appearance.setMarkWorkApps(it) } }, value = stringResource(R.string.settings_mark_work_apps_detail), icon = Icons.Filled.Work), "work profile duplicate apps"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_last_letter), lastLetter, { env.write { list.setLastLetterToLine(it) } }, value = stringResource(R.string.settings_last_letter_detail), icon = Icons.Filled.TextFields), "letter lands end z"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_section_top), value = "$sectionTop%", icon = Icons.Filled.VerticalAlignTop) {
            SettingsSlider(sectionTop.toFloat(), SECTION_TOP_RANGE.first.toFloat()..SECTION_TOP_RANGE.last.toFloat(), {
                env.previews.showSection()
                env.write { list.setSectionTopPercent(it.toInt()) }
            })
        }, "letter lands scroll position"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_hide_status_bar_applist), hideStatusBar, icon = Icons.Filled.VisibilityOff) { env.write { appearance.setHideStatusBarAppList(it) } }, "status bar"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_dim_applist), value = "${(dim * 100).roundToInt()}%", icon = Icons.Filled.Contrast) {
            SettingsSlider(dim, 0f..0.85f, step = 0.01f, onValueChange = {
                val next = (it * 100).roundToInt() / 100f
                env.write { appearance.setDimWallpaperAlpha(next) }
            })
        }, "dim wallpaper"),
    )
}
