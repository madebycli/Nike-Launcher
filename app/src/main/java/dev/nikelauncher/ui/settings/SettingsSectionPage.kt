// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.orion.OrionPageHero
import dev.nikelauncher.ui.kit.orion.OrionPageScaffold
import dev.nikelauncher.ui.kit.orion.orionSharedBounds
import dev.nikelauncher.ui.kit.settings.SettingsGroup

/** One section's page: its hero in the section's hue, then its rows. The page grows out of the section's hub tile. */
@Composable
internal fun SettingsSectionPage(
    section: SettingsSection,
    entries: List<SettingsEntry>,
    onBack: () -> Unit,
    containerColor: Color,
) {
    val title = stringResource(section.labelRes)
    OrionPageScaffold(
        title = title,
        onBack = onBack,
        modifier = Modifier.orionSharedBounds(section.name),
        containerColor = containerColor,
        hero = { topInset ->
            OrionPageHero(
                icon = section.icon,
                title = title,
                desc = stringResource(section.descRes),
                accent = section.accent,
                shape = section.shape,
                background = containerColor,
                top = { Spacer(Modifier.height(topInset)) },
            )
        },
    ) {
        item(key = "rows") {
            SettingsGroup(entries.map { it.row }, Modifier.padding(horizontal = 16.dp))
        }
    }
}
