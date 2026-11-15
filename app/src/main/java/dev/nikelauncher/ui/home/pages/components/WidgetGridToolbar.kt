// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R

/** Stock Material buttons retain their component shapes and grow with translated labels. */
@Composable
internal fun WidgetGridToolbar(editing: Boolean, selected: Boolean, onPrimary: () -> Unit, onDone: () -> Unit) {
    AnimatedVisibility(visible = editing) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = onPrimary, modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp)) {
                Icon(if (selected) Icons.Filled.Close else Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (selected) R.string.widget_pages_remove_short else R.string.widget_add),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val done: @Composable RowScope.() -> Unit = {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.widget_pages_done))
            }
            if (selected) OutlinedButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp), content = done)
            else FilledTonalButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp), content = done)
        }
    }
}
