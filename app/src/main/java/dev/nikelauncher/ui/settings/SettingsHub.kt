// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import dev.nikelauncher.ui.kit.orion.orionReveal
import dev.nikelauncher.ui.kit.search.SearchField
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsScaffold
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel

/** How tall a hub tile is: room for the icon, a title that may take a second line's worth of size, and two lines below. */
private val TileHeight = 156.dp

/**
 * The first settings screen: the search box, then a tile for each section, two to a row. While something is typed, the
 * matching rows of every section take the tiles' place, grouped under their section.
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
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        containerColor = containerColor,
    ) {
        item(key = "search") { SearchField(query, onQueryChange, stringResource(R.string.settings_search)) }
        if (query.isBlank()) {
            itemsIndexed(SettingsSection.entries.chunked(2), key = { _, row -> row.first().name }) { index, row ->
                Row(
                    Modifier.fillMaxWidth().height(TileHeight).orionReveal(index),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { section ->
                        OrionBento(
                            title = stringResource(section.labelRes),
                            desc = stringResource(section.descRes),
                            icon = section.icon,
                            accent = section.accent,
                            shape = section.shape,
                            onClick = { onOpen(section) },
                            modifier = Modifier.weight(1f).testTag("settings-tile-${section.name}"),
                            sharedKey = section.name,
                        )
                    }
                }
            }
        } else {
            items(results, key = { it.first.name }) { (section, group) ->
                SettingsSectionLabel(stringResource(section.labelRes), Modifier.padding(bottom = 8.dp))
                SettingsGroup(group.map { it.row })
            }
            if (results.isEmpty()) {
                item(key = "none") {
                    Text(stringResource(R.string.settings_search_none), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
