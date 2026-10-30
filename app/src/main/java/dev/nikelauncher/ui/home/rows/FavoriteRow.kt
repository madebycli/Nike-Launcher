// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.ui.common.requestUninstall
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.AppShortcutMenu
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.common.swipeForShortcuts
import dev.nikelauncher.ui.kit.menu.TouchMenu
import dev.nikelauncher.media.NotificationPreviewBus
import dev.nikelauncher.ui.notifications.NotificationSheet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.nikelauncher.ui.home.edit.DragHandle
import dev.nikelauncher.ui.home.edit.DragHandleReserve

/** What a favorite's long-press menu can do. */
internal class FavoriteMenuActions(
    val onMoveToFolder: () -> Unit,
    val onEditLayout: () -> Unit,
    val onAppInfo: () -> Unit,
    val onUnpin: () -> Unit,
    val onRemove: () -> Unit,
    val onEditIconName: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/** One favorite: icon, optional name, a ripple for the press and its long-press menu. */
@Composable
internal fun FavoriteRow(
    app: AppInfo,
    label: String,
    style: RowStyle,
    editMode: Boolean,
    /** Non-null in edit mode: the gesture that reorders, attached to this row's grab handle. */
    dragHandle: Modifier?,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    touchPosition: MutableState<Offset>,
    onLaunch: () -> Unit,
    onOpenMenu: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
    menu: FavoriteMenuActions,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var shortcutMenu by remember { mutableStateOf(false) }
    var notificationSheet by remember { mutableStateOf(false) }
    val posted by NotificationPreviewBus.notifications.collectAsState()
    val hasNotifications = app.kind == EntryKind.APP && posted.any { it.packageName == app.packageName }
    val alignment = style.alignment

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                // Only the icon and name answer a tap when the row is narrowed to them; the rest
                // of the line is the home screen again, not a button nobody can see.
                .then(if (style.compactTapArea) Modifier.align(alignment.boxAlignment()) else Modifier.fillMaxWidth())
                .padding(horizontal = (style.sidePaddingDp - 8).coerceAtLeast(0).dp)
                .clip(MaterialTheme.shapes.large)
                .then(
                    // In edit mode the row must not claim the long press, or the reorder drag never starts.
                    if (editMode) Modifier else Modifier
                        .recordTouchPosition(touchPosition)
                        .swipeForShortcuts(style.shortcutSwipe, app.kind == EntryKind.APP) { shortcutMenu = true }
                        .combinedClickable(
                            indication = ripple(color = style.contentColor),
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = onLaunch,
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
                icon = { AppIcon(app = app, sizeDp = style.iconSizeDp) },
            ) { labelModifier ->
                val preview = notificationPreview(app)
                Column(labelModifier, horizontalAlignment = alignment.columnAlignment()) {
                    Text(
                        label,
                        // A shortcut its publisher has switched off is still listed, but tapping
                        // it only explains why it will not open.
                        color = if (app.disabled) style.contentColor.copy(alpha = 0.5f) else style.contentColor,
                        fontSize = style.labelSizeSp.sp,
                        textAlign = alignment.textAlign(),
                    )
                    NotificationLines(preview, alignment, style.contentColor, interactive = !editMode, onFallback = onLaunch)
                }
            }
        }

        // Outside the row's own layout so it sits against the screen edge rather than wherever the
        // alignment happens to leave the text.
        if (dragHandle != null) {
            DragHandle(Modifier.align(Alignment.CenterEnd).then(dragHandle), style.contentColor)
        }

        AppShortcutMenu(app, shortcutMenu) { shortcutMenu = false }
        if (notificationSheet) NotificationSheet(app = app, onDismiss = { notificationSheet = false })

        TouchMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = onDismissMenu) {
            // Only while this app has something on the shade, and only if previews are on.
            if (hasNotifications) {
                item(stringResource(R.string.notifications_title), { notificationSheet = true }, leadingIcon = Icons.Filled.Notifications)
            }
            item(stringResource(R.string.action_move_to_folder), menu.onMoveToFolder, leadingIcon = Icons.Filled.Folder)
            item(stringResource(R.string.action_edit_layout), menu.onEditLayout, leadingIcon = Icons.Filled.Edit)
            // Only an installed app has a settings screen to open; a row that stands for something
            // else, a shortcut or the private space, would send the system looking for a package
            // that is not there.
            if (app.kind == EntryKind.APP) {
                item(stringResource(R.string.action_app_info), menu.onAppInfo, leadingIcon = Icons.Filled.Info)
                item(
                    stringResource(R.string.action_uninstall),
                    { if (!context.requestUninstall(app)) context.toast(R.string.clock_action_unavailable) },
                    leadingIcon = Icons.Filled.Delete,
                    destructive = true,
                )
            }
            // Distinct from Remove below, which only takes it off the home screen: this hands it
            // back to the app that pinned it, and it leaves the launcher entirely.
            if (app.kind == EntryKind.SHORTCUT) {
                item(stringResource(R.string.action_remove_shortcut), menu.onUnpin, leadingIcon = Icons.Filled.LinkOff)
            }
            item(stringResource(R.string.action_remove), menu.onRemove, leadingIcon = Icons.Filled.RemoveCircleOutline)
            gap()
            item(stringResource(R.string.action_edit_icon_and_name), menu.onEditIconName, leadingIcon = Icons.Filled.Tune)
            item(stringResource(R.string.action_open_settings), menu.onOpenSettings, leadingIcon = Icons.Filled.Settings)
        }
    }
}
