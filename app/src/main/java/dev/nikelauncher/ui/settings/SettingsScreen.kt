// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.kit.search.SearchField
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsScaffold
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.settings.sections.aboutEntries
import dev.nikelauncher.ui.settings.sections.appListEntries
import dev.nikelauncher.ui.settings.sections.backgroundEntries
import dev.nikelauncher.ui.settings.sections.widgetPageEntries
import dev.nikelauncher.ui.settings.sections.searchButtonEntries
import dev.nikelauncher.ui.settings.sections.appearanceEntries
import dev.nikelauncher.ui.settings.sections.appsEntries
import dev.nikelauncher.ui.settings.sections.backupEntries
import dev.nikelauncher.ui.settings.sections.gesturesEntries
import dev.nikelauncher.ui.settings.sections.homeEntries
import dev.nikelauncher.ui.settings.sections.notificationEntries
import dev.nikelauncher.ui.settings.sections.stripEntries

/**
 * The settings: a list of sections, one section at a time, and a search box over all of them.
 *
 * The open section is saved, not just remembered: settings that change the window (the rotation
 * lock) rebuild the activity, and a plain remember would drop whoever was three rows into a
 * section back at the top menu.
 */
@Composable
fun SettingsScreen(env: SettingsEnv, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val open = openName?.let { name -> SettingsSection.entries.firstOrNull { it.name == name } }
    BackHandler(enabled = open != null || query.isNotBlank()) {
        if (query.isNotBlank()) query = "" else openName = null
    }

    val entries = (
        appearanceEntries(env) + homeEntries(env) + widgetPageEntries(env) + appListEntries(env) + backgroundEntries(env) + searchButtonEntries(env) + stripEntries(env) +
            gesturesEntries(env) + notificationEntries(env) + appsEntries(env) +
            backupEntries(env) + aboutEntries(env)
        ).filter { it.visible }
    val shown = remember(entries, query, open) {
        val matching = when {
            query.isNotBlank() -> entries.filter { it.matches(query) }
            open != null -> entries.filter { it.section == open }
            else -> emptyList()
        }
        matching.groupBy { it.section }.toList()
    }

    val dimReveal = env.previews.dimReveal()
    val dimColor by env.prefs.appearance.dimColor.collectAsState(0xFF000000.toInt())
    val edgeSide by env.prefs.strip.edgeSide.collectAsState(EdgeSide.RIGHT)
    val edgeWidth by env.prefs.strip.edgeZoneWidthDp.collectAsState(56)
    val sectionTop by env.prefs.appList.sectionTopPercent.collectAsState(26)

    Box(modifier.fillMaxSize()) {
        DimPreview(env.previews, Color(dimColor), dimReveal)
        SettingsScaffold(
            title = stringResource(open?.labelRes ?: R.string.settings_title),
            onBack = { if (open != null) openName = null else onBack() },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f - dimReveal),
        ) {
            item {
                SearchField(query, { query = it }, stringResource(R.string.settings_search))
            }
            if (open == null && query.isBlank()) {
                item {
                    SettingsGroup(
                        SettingsSection.entries.map { section ->
                            settingsItem(
                                title = stringResource(section.labelRes),
                                onClick = { openName = section.name },
                                icon = section.icon,
                                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            )
                        },
                    )
                }
            }
            items(shown, key = { it.first.name }) { (section, group) ->
                SettingsSectionLabel(stringResource(section.labelRes), Modifier.padding(bottom = 8.dp))
                SettingsGroup(group.map { it.row })
            }
            if (query.isNotBlank() && shown.isEmpty()) {
                item { Text(stringResource(R.string.settings_search_none), style = MaterialTheme.typography.bodyMedium) }
            }
        }
        PreviewOverlays(env.previews, edgeSide, edgeWidth, sectionTop)
    }
}
