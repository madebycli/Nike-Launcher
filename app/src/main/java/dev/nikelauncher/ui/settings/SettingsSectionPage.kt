// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.ui.kit.orion.OrionPageScaffold
import dev.nikelauncher.ui.kit.orion.orionSharedBounds
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel

/**
 * One section's page: its hero in the section's hue with back above the icon, then its rows, each labelled group apart.
 * The page grows out of the section's hub tile and, on back, shrinks into it again.
 */
@Composable
internal fun SettingsSectionPage(
    section: SettingsSection,
    entries: List<SettingsEntry>,
    onBack: () -> Unit,
    containerColor: Color,
) {
    val groups = entries.groupBy { it.group }.toList()
    OrionPageScaffold(
        title = stringResource(section.labelRes),
        desc = stringResource(section.descRes),
        icon = section.icon,
        accent = section.accent,
        shape = section.shape,
        onBack = onBack,
        modifier = Modifier.orionSharedBounds(section.name),
        containerColor = containerColor,
    ) {
        groups.forEach { (label, rows) ->
            label?.let { SettingsSectionLabel(stringResource(it)) }
            SettingsGroup(rows.map { it.row })
        }
    }
}
