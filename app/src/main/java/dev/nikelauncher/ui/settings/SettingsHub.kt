// SPDX-License-Identifier: GPL-3.0-or-later
/* Hub layout adapted from iris-android (GPL-3.0), presentation/settings/SettingsScreen.kt SettingsHub, itself after
 * Orion Store ui/Settings.kt Hub (RookieEnough and contributors, GPL-3.0, e561e91).
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nikelauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.orion.OrionBento
import dev.nikelauncher.ui.kit.orion.OrionPageScaffold
import dev.nikelauncher.ui.kit.orion.orionReveal
import dev.nikelauncher.ui.kit.search.SearchField
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel

/** The hub's three groups, each laid out as one bento: what it looks like, how it is used, and the data around it. */
private val HubGroups: List<Pair<Int, List<SettingsSection>>> = listOf(
    R.string.settings_group_look to listOf(SettingsSection.APPEARANCE, SettingsSection.HOME, SettingsSection.APP_LIST, SettingsSection.STRIP),
    R.string.settings_group_handling to listOf(SettingsSection.GESTURES, SettingsSection.NOTIFICATIONS, SettingsSection.APPS),
    R.string.settings_group_data to listOf(SettingsSection.BACKUP, SettingsSection.ABOUT),
)

/**
 * The first settings screen, as iris and Orion lay it out: a hero, the search box, then each group's tiles as a bento
 * (one wide tile; then a tall one beside two small ones, or two small ones, or one more wide one). While something is
 * typed, the matching rows of every section take the tiles' place, grouped under their section.
 */
@Composable
internal fun SettingsHub(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Pair<SettingsSection, List<SettingsEntry>>>,
    onOpen: (SettingsSection) -> Unit,
    onBack: () -> Unit,
    containerColor: Color,
) {
    OrionPageScaffold(
        title = stringResource(R.string.settings_title),
        desc = "",
        icon = Icons.Outlined.Settings,
        accent = MaterialTheme.colorScheme.primary,
        shape = MaterialShapes.Cookie9Sided,
        onBack = onBack,
        containerColor = containerColor,
    ) {
        item(key = "search") {
            SearchField(query, onQueryChange, stringResource(R.string.settings_search), Modifier.padding(horizontal = 16.dp))
        }
        if (query.isBlank()) {
            HubGroups.forEachIndexed { index, (label, sections) ->
                item(key = "group-$label") {
                    Column(Modifier.padding(horizontal = 16.dp).orionReveal(index + 1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSectionLabel(stringResource(label))
                        Bento(sections, onOpen)
                    }
                }
            }
        } else {
            items(results, key = { it.first.name }) { (section, group) ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionLabel(stringResource(section.labelRes))
                    SettingsGroup(group.map { it.row })
                }
            }
            if (results.isEmpty()) {
                item(key = "none") {
                    Text(
                        stringResource(R.string.settings_search_none),
                        Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One group's tiles, in iris' bento sizes. */
@Composable
private fun Bento(sections: List<SettingsSection>, onOpen: (SettingsSection) -> Unit) {
    @Composable
    fun Slot(section: SettingsSection, modifier: Modifier, compact: Boolean = false) = OrionBento(
        title = stringResource(section.labelRes),
        desc = stringResource(section.descRes),
        icon = section.icon,
        accent = section.accent,
        shape = section.shape,
        onClick = { onOpen(section) },
        modifier = modifier.testTag("settings-tile-${section.name}"),
        compact = compact,
        sharedKey = section.name,
    )
    Slot(sections[0], Modifier.fillMaxWidth().height(156.dp))
    when (sections.size) {
        1 -> Unit
        2 -> Slot(sections[1], Modifier.fillMaxWidth().height(140.dp))
        3 -> Row(Modifier.height(132.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Slot(sections[1], Modifier.weight(1f).fillMaxHeight(), compact = true)
            Slot(sections[2], Modifier.weight(1f).fillMaxHeight(), compact = true)
        }
        else -> {
            Row(Modifier.height(268.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Slot(sections[1], Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slot(sections[2], Modifier.fillMaxWidth().weight(1f), compact = true)
                    Slot(sections[3], Modifier.fillMaxWidth().weight(1f), compact = true)
                }
            }
            sections.drop(4).forEach { Slot(it, Modifier.fillMaxWidth().height(140.dp)) }
        }
    }
}

