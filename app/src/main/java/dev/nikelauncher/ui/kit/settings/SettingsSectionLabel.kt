// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.orion.LocalPageAccent
import dev.nikelauncher.ui.kit.orion.orionTonal

/** The label above a [SettingsGroup], in the tone of the page's accent (iris SettingsSectionLabel). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    val accent = LocalPageAccent.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = accent?.let { orionTonal(it).second } ?: MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp),
    )
}
