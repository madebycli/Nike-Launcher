// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection

private val SECTION = SettingsSection.APPS

@Composable
internal fun appsEntries(env: SettingsEnv): List<SettingsEntry> {
    val library = env.prefs.library
    val source by library.favoritesSource.collectAsState(FavoritesSource.MANUAL)
    val excluded by library.frequentExcluded.collectAsState(emptySet())

    return listOf(
        SettingsEntry(SECTION, settingsItem(stringResource(R.string.settings_favorites), env.nav.onFavorites, value = stringResource(R.string.settings_favorites_detail)), "manage"),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.settings_default_launcher),
                env.actions.onOpenHomeSettings,
                value = stringResource(if (env.isDefaultLauncher) R.string.settings_default_launcher_yes else R.string.settings_default_launcher_no),
            ),
            "home default launcher set",
        ),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.settings_frequent_excluded),
                env.nav.onFrequentExcluded,
                value = if (excluded.isEmpty()) {
                    stringResource(R.string.settings_frequent_excluded_none)
                } else {
                    pluralStringResource(R.plurals.settings_frequent_excluded_count, excluded.size, excluded.size)
                },
            ),
            "most used favorites exclude block",
            visible = source == FavoritesSource.FREQUENT,
        ),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.settings_hidden_apps),
                env.nav.onHiddenApps,
                value = if (env.hiddenCount == 0) stringResource(R.string.settings_hidden_none)
                else stringResource(R.string.settings_hidden_count, env.hiddenCount),
            ),
            "hide",
        ),
    )
}
