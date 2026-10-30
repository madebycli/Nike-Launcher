// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.media.NowPlayingWidget
import dev.nikelauncher.media.openNowPlayingApp
import dev.nikelauncher.ui.notifications.appNotification
import dev.nikelauncher.ui.notifications.NotificationSheet
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.AppShortcutMenu
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.common.swipeForShortcuts
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.kit.menu.TouchMenu
import dev.nikelauncher.widget.WidgetSlotActions

/** What an open folder can do with its members. */
internal class FolderMemberActions(
    val displayName: (AppInfo) -> String,
    val onOpenApp: (AppInfo) -> Unit,
    val onRemoveApp: (AppInfo) -> Unit,
    val onAppInfo: (AppInfo) -> Unit,
    val onUnpin: (AppInfo) -> Unit,
    val onEditIconName: (AppInfo) -> Unit,
    val onManage: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/** What an open folder holds: its widgets, Now Playing if it was put there, then its apps. */
@Composable
internal fun FolderContents(
    folder: Folder,
    members: List<AppInfo>,
    style: RowStyle,
    controlsInset: Modifier,
    widgetActions: WidgetSlotActions,
    actions: FolderMemberActions,
) {
    val alignment = style.alignment
    val density = LocalDensity.current
    // Which member is showing its menu, and where the press that opened it landed.
    var menuFor by remember { mutableStateOf<String?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val touch = remember { mutableStateOf(Offset.Zero) }
    var shortcutsFor by remember { mutableStateOf<String?>(null) }

    Column {
        // Above the apps, so a folder's widget is what it opens onto.
        folder.widgets.forEach { widget ->
            key(widget.id) { FolderWidgetBlock(folder.id, widget, widgetActions, controlsInset) }
        }
        if (folder.nowPlaying) {
            // Shown even when silent: it was put here on purpose, and an Audio folder that opens
            // without it looks like it lost it.
            val context = LocalContext.current
            NowPlayingWidget(
                contentColor = style.contentColor,
                alignment = alignment,
                showPlaceholder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(controlsInset)
                    .padding(horizontal = style.sidePaddingDp.dp)
                    .clickable { if (!openNowPlayingApp(context)) context.toast(R.string.now_playing_open_failed) },
            )
        }
        members.forEach { member ->
            key(member.key) {
                var notificationOpen by remember { mutableStateOf(false) }
                val preview = appNotification(member)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Members sit one indent inside their folder, on whichever side the
                            // folder's own name is on.
                            .padding(
                                start = if (alignment == HomeAlignment.RIGHT) style.sidePaddingDp.dp else (style.sidePaddingDp + 24).dp,
                                end = if (alignment == HomeAlignment.RIGHT) (style.sidePaddingDp + 24).dp else style.sidePaddingDp.dp,
                            )
                            .recordTouchPosition(touch)
                            .swipeForShortcuts(style.shortcutSwipe, member.kind == EntryKind.APP,
                                onNotifications = if (preview == null) null else ({ notificationOpen = true })) { shortcutsFor = member.key }
                            .combinedClickable(
                                onClick = { actions.onOpenApp(member) },
                                onLongClick = {
                                    menuOffset = with(density) { DpOffset(touch.value.x.toDp(), touch.value.y.toDp()) }
                                    menuFor = member.key
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = alignment.arrangement(),
                    ) {
                        val iconDp = (style.iconSizeDp * 0.8f).toInt()
                        AlignedIconLabel(
                            alignment = alignment,
                            iconSide = style.iconSide,
                            showLabel = style.showLabels,
                            iconWidth = iconDp.dp,
                            icon = { AppIcon(app = member, sizeDp = iconDp) },
                        ) { labelModifier ->
                            Column(labelModifier, horizontalAlignment = alignment.columnAlignment()) {
                                Text(
                                    actions.displayName(member),
                                    color = if (member.disabled) style.contentColor.copy(alpha = 0.5f) else style.contentColor,
                                    fontSize = style.labelSizeSp.sp,
                                    textAlign = alignment.textAlign(),
                                )
                                NotificationLines(
                                    preview, alignment, style.contentColor,
                                )
                            }
                        }
                    }

                    if (notificationOpen) NotificationSheet(member, onDismiss = { notificationOpen = false })
                    AppShortcutMenu(member, shortcutsFor == member.key) { shortcutsFor = null }

                    TouchMenu(expanded = menuFor == member.key, offset = menuOffset, onDismissRequest = { menuFor = null }) {
                        // A folder member is an app like any other row, so it offers what the app
                        // publishes the same way the rows outside a folder do.
                        if (member.kind == EntryKind.APP) {
                            item(stringResource(R.string.action_app_info), { menuFor = null; actions.onAppInfo(member) }, leadingIcon = Icons.Filled.Info)
                        }
                        if (member.kind == EntryKind.SHORTCUT) {
                            item(stringResource(R.string.action_remove_shortcut), { menuFor = null; actions.onUnpin(member) }, leadingIcon = Icons.Filled.LinkOff)
                        }
                        item(stringResource(R.string.action_edit_icon_and_name), { menuFor = null; actions.onEditIconName(member) }, leadingIcon = Icons.Filled.Tune)
                        item(stringResource(R.string.home_folder_choose_apps), { menuFor = null; actions.onManage() }, leadingIcon = Icons.Filled.Checklist)
                        gap()
                        item(stringResource(R.string.home_folder_remove_app), { menuFor = null; actions.onRemoveApp(member) }, leadingIcon = Icons.Filled.Delete, destructive = true)
                        item(stringResource(R.string.action_open_settings), { menuFor = null; actions.onOpenSettings() }, leadingIcon = Icons.Filled.Settings)
                    }
                }
            }
        }
        if (members.isEmpty()) {
            Text(
                stringResource(R.string.home_folder_empty),
                color = style.contentColor.copy(alpha = 0.6f),
                fontSize = (style.labelSizeSp - 3).coerceAtLeast(10).sp,
                modifier = Modifier.padding(start = (style.sidePaddingDp + 24).dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
