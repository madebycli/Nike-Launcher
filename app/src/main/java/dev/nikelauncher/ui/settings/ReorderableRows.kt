// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.service.HapticUtil
import sh.calvin.reorderable.ReorderableColumn

/** The library owns drag measurement and motion; buttons provide one-place reordering. */
@Composable
fun ReorderableRows(keys: List<String>, onReorder: (List<String>) -> Unit, row: @Composable RowScope.(key: String) -> Unit) {
    val view = LocalView.current
    ReorderableColumn(list = keys, modifier = Modifier.fillMaxWidth(),
        onSettle = { from, to -> onReorder(moveRow(keys, from, to)) },
        onMove = { HapticUtil.tick(view, true) }) { index, key, _ ->
        ReorderableItem {
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    row(key)
                    Column {
                        IconButton(onClick = { onReorder(moveRow(keys, index, index - 1)) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, stringResource(R.string.edit_move_up))
                        }
                        IconButton(onClick = { onReorder(moveRow(keys, index, index + 1)) }, enabled = index < keys.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, stringResource(R.string.edit_move_down))
                        }
                    }
                    Icon(Icons.Filled.DragHandle, stringResource(R.string.home_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp).longPressDraggableHandle(onDragStarted = { HapticUtil.tick(view, true) }))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
