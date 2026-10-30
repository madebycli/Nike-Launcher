// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.applist.AppListRow
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.home.rows.FolderIcon
import dev.nikelauncher.ui.kit.menu.TouchMenu

/** What a folder row's long-press menu can do. */
internal class FolderListActions(
    val onToggle: () -> Unit,
    val onManage: () -> Unit,
    val onSetOnHome: (Boolean) -> Unit,
    val onDelete: () -> Unit,
)

/** A folder filed in the list. A tap opens it in place, as it does on the home screen. */
@Composable
internal fun FolderListRow(
    row: AppListRow.FolderRow,
    style: ListRowStyle,
    onHome: Boolean,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    touchPosition: MutableState<Offset>,
    actions: FolderListActions,
    onLongPress: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
) {
    val density = LocalDensity.current
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .recordTouchPosition(touchPosition)
                .padding(horizontal = 20.dp)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    indication = ripple(color = style.contentColor),
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = actions.onToggle,
                    onLongClick = {
                        onLongPress(with(density) { DpOffset(touchPosition.value.x.toDp(), touchPosition.value.y.toDp()) })
                    },
                )
                .heightIn(min = MIN_ROW_HEIGHT)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = style.alignment.rowArrangement(),
        ) {
            val gap = if (LocalIconConfig.current.showIcons) 16.dp else 0.dp
            val icon: @Composable () -> Unit = { FolderIcon(row.members, style.iconSizeDp, style.contentColor, row.folder.icon) }
            val text: @Composable () -> Unit = {
                Text(row.folder.name, color = style.contentColor, fontSize = style.labelSizeSp.sp, modifier = Modifier.weight(1f, fill = false))
            }
            if (style.iconSide == IconSide.RIGHT) {
                text(); Spacer(Modifier.width(gap)); icon()
            } else {
                icon(); Spacer(Modifier.width(gap)); text()
            }
        }

        TouchMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = onDismissMenu) {
            item(stringResource(R.string.home_folder_choose_apps), actions.onManage, leadingIcon = Icons.Filled.Tune)
            item(
                stringResource(if (onHome) R.string.folder_remove_from_home else R.string.folder_add_to_home),
                { actions.onSetOnHome(!onHome) },
                leadingIcon = if (onHome) Icons.Filled.StarBorder else Icons.Filled.Star,
            )
            item(stringResource(R.string.home_folder_delete), actions.onDelete, leadingIcon = Icons.Filled.Delete, destructive = true)
        }
    }
}
