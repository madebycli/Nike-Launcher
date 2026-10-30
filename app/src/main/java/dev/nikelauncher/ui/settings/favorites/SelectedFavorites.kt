// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.*
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.settings.moveRow
import sh.calvin.reorderable.ReorderableColumn

@Composable
internal fun SelectedFavorites(keys: List<String>, apps: Map<String, AppInfo>, folders: Map<String, Folder>,
    names: Map<String, String>, iconSizeDp: Int, onRemove: (String) -> Unit, onReorder: (List<String>) -> Unit) {
    val up = stringResource(R.string.edit_move_up)
    val down = stringResource(R.string.edit_move_down)
    ReorderableColumn(list = keys, modifier = Modifier.fillMaxWidth(), onSettle = { from, to -> onReorder(moveRow(keys, from, to)) }) { index, key, _ ->
        ReorderableItem {
            val app = apps[key]
            val folder = folderIdFromToken(key)?.let(folders::get)
            SegmentedListItem(
                onClick = { onRemove(key) },
                shapes = ListItemDefaults.segmentedShapes(index, keys.size),
                colors = ListItemDefaults.segmentedColors(),
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = ListItemDefaults.SegmentedGap),
                leadingContent = {
                    if (app != null) AppIcon(app, minOf(iconSizeDp, 44))
                    else if (folder != null) Icon(Icons.Filled.Folder, null)
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(onClick = { onRemove(key) }) { Icon(Icons.Filled.Remove, stringResource(R.string.favorites_remove)) }
                        FilledTonalIconButton(onClick = {}, modifier = Modifier.longPressDraggableHandle().semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(up) { onReorder(moveRow(keys, index, index - 1)); true })
                                if (index < keys.lastIndex) add(CustomAccessibilityAction(down) { onReorder(moveRow(keys, index, index + 1)); true })
                            }
                        }) { Icon(Icons.Filled.OpenWith, stringResource(R.string.home_drag_handle)) }
                    }
                },
            ) { Text(folder?.name ?: app?.let { names[it.key] ?: it.label } ?: stringResource(R.string.folder_member_missing), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
