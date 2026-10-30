// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeAlignment

/** Sets the settings shortcut apart from the last app above it. */
private val SETTINGS_ROW_GAP = 20.dp

/** Offered when a search turns up nothing: the words go to the browser instead. */
@Composable
internal fun WebSearchRow(term: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Text(
            stringResource(R.string.applist_search_web, term),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The settings shortcut, pinned after Z. Optional: it is the one row in a list of apps that is not
 * an app, and someone who knows the long press is there has no use for it.
 */
@Composable
internal fun SettingsShortcutRow(style: ListRowStyle, alpha: () -> Float, onClick: () -> Unit) {
    Spacer(Modifier.height(SETTINGS_ROW_GAP))
    val tint = style.contentColor.copy(alpha = 0.8f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha() }
            .clickable(onClick = onClick)
            .heightIn(min = MIN_ROW_HEIGHT)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = style.alignment.rowArrangement(),
    ) {
        val label: @Composable () -> Unit = {
            Text(stringResource(R.string.action_open_settings), color = tint, fontSize = style.labelSizeSp.sp)
        }
        val icon: @Composable () -> Unit = { Icon(Icons.Filled.Settings, contentDescription = null, tint = tint) }
        if (style.alignment == HomeAlignment.RIGHT) {
            label(); Spacer(Modifier.width(16.dp)); icon()
        } else {
            icon(); Spacer(Modifier.width(16.dp)); label()
        }
    }
}
