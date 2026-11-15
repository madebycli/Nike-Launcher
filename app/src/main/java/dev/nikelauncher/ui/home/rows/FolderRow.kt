// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
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
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.home.edit.DragHandle
import dev.nikelauncher.ui.home.edit.DragHandleReserve
import dev.nikelauncher.ui.kit.menu.TouchMenu
import dev.nikelauncher.widget.WidgetSlotActions

/** What a folder row's long-press menu can do. */
internal class FolderMenuActions(
    val onManage: () -> Unit,
    val onEdit: () -> Unit,
    val onEditLayout: () -> Unit,
    val onDelete: () -> Unit,
    val onRemoveFromHome: (() -> Unit)?,
)

/** A folder row, which expands in place to show its apps. */
@Composable
internal fun FolderRow(
    folder: Folder,
    members: List<AppInfo>,
    style: RowStyle,
    expanded: Boolean,
    editMode: Boolean,
    /** Non-null in edit mode: the gesture that reorders, attached to this row's grab handle. */
    dragHandle: Modifier?,
    controlsInset: Modifier,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    touchPosition: MutableState<Offset>,
    widgetActions: WidgetSlotActions,
    memberActions: FolderMemberActions,
    menu: FolderMenuActions,
    onToggleExpanded: () -> Unit,
    onOpenMenu: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
) {
    val density = LocalDensity.current
    val alignment = style.alignment

    Column {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .then(if (style.compactTapArea) Modifier.align(alignment.boxAlignment()) else Modifier.fillMaxWidth())
                    .padding(horizontal = (style.sidePaddingDp - 8).coerceAtLeast(0).dp)
                    .clip(MaterialTheme.shapes.large)
                    .then(
                        if (editMode) Modifier else Modifier
                            .recordTouchPosition(touchPosition)
                            .combinedClickable(
                                indication = ripple(color = style.contentColor),
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                onClick = onToggleExpanded,
                                onLongClick = {
                                    onOpenMenu(with(density) { DpOffset(touchPosition.value.x.toDp(), touchPosition.value.y.toDp()) })
                                },
                            ),
                    )
                    .padding(start = 8.dp, end = if (dragHandle == null) 8.dp else DragHandleReserve, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = alignment.arrangement(),
            ) {
                AlignedIconLabel(
                    alignment = alignment,
                    showLabel = style.showLabels,
                    iconSide = style.iconSide,
                    iconWidth = style.iconSizeDp.dp,
                    icon = { FolderIcon(members, style.iconSizeDp, style.contentColor, folder.icon) },
                ) { labelModifier ->
                    Text(
                        folder.name,
                        color = style.contentColor,
                        fontSize = style.labelSizeSp.sp,
                        modifier = labelModifier,
                        textAlign = alignment.textAlign(),
                    )
                }
            }

            if (dragHandle != null) {
                DragHandle(Modifier.align(Alignment.CenterEnd).then(dragHandle), style.contentColor)
            }

            TouchMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = onDismissMenu) {
                item(stringResource(R.string.home_folder_choose_apps), menu.onManage, leadingIcon = Icons.Filled.Checklist)
                item(stringResource(R.string.widget_add), { onDismissMenu(); widgetActions.onAddWidgetToFolder(folder.id) }, leadingIcon = Icons.Filled.Widgets)
                item(
                    stringResource(if (folder.nowPlaying) R.string.folder_remove_now_playing else R.string.folder_add_now_playing),
                    { onDismissMenu(); widgetActions.onSetFolderNowPlaying(folder.id, !folder.nowPlaying) },
                    leadingIcon = Icons.Filled.PlayArrow,
                )
                item(stringResource(R.string.action_edit_icon_and_name), menu.onEdit, leadingIcon = Icons.Filled.Edit)
                item(stringResource(R.string.action_edit_layout), menu.onEditLayout, leadingIcon = Icons.Filled.Tune)
                gap()
                menu.onRemoveFromHome?.let { remove ->
                    item(stringResource(R.string.folder_remove_from_home), remove, leadingIcon = Icons.Filled.StarBorder)
                }
                item(stringResource(R.string.home_folder_delete), menu.onDelete, leadingIcon = Icons.Filled.Delete, destructive = true)
            }
        }

        // Members expand in place rather than opening a separate screen.
        AnimatedVisibility(visible = expanded && !editMode) {
            FolderContents(folder, members, style, controlsInset, widgetActions, memberActions)
        }
    }
}
