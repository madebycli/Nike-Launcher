// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerSettings

/** The live button preview stays visible while its controls scroll beneath it. */
@Composable
internal fun CornerSettingsPreview(settings: CornerSettings, overWallpaper: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 38.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        CornerButtonFace(settings.shape, settings.sizeDp, settings.tap, null, overWallpaper, appearance = settings.appearance)
        Text(stringResource(R.string.corner_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
    }
}
