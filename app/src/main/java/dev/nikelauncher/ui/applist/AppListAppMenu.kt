// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.*
import dev.nikelauncher.ui.common.requestUninstall
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.kit.menu.TouchMenu

@Composable
internal fun AppListAppMenu(
    app: AppInfo,
    expanded: Boolean,
    offset: DpOffset,
    favoritesEditable: Boolean,
    isFavorite: Boolean,
    isHidden: Boolean,
    hasNotifications: Boolean,
    onNotifications: () -> Unit,
    onDismiss: () -> Unit,
    onSetFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAppInfo: () -> Unit,
    onUnpin: () -> Unit,
    onHide: () -> Unit,
    onMoveToFolder: () -> Unit,
    onRemoveFromFolder: (() -> Unit)?,
) {
    val context = LocalContext.current
    fun act(action: () -> Unit) {
        onDismiss()
        action()
    }
    TouchMenu(expanded, offset, onDismiss) {
        if (hasNotifications)
            item(
                stringResource(R.string.notifications_title),
                { act(onNotifications) },
                leadingIcon = Icons.Filled.Notifications,
            )
        if (favoritesEditable)
            item(
                stringResource(
                    if (isFavorite) R.string.applist_remove_favorite
                    else R.string.applist_add_favorite
                ),
                { act { onSetFavorite(!isFavorite) } },
                leadingIcon = if (isFavorite) Icons.Filled.StarBorder else Icons.Filled.Star,
            )
        item(
            stringResource(R.string.action_edit_icon_and_name),
            { act(onEdit) },
            leadingIcon = Icons.Filled.Tune,
        )
        if (app.kind == EntryKind.APP) {
            item(
                stringResource(R.string.action_app_info),
                { act(onAppInfo) },
                leadingIcon = Icons.Filled.Info,
            )
            item(
                stringResource(R.string.action_uninstall),
                {
                    act {
                        if (!context.requestUninstall(app))
                            context.toast(R.string.clock_action_unavailable)
                    }
                },
                leadingIcon = Icons.Filled.Delete,
                destructive = true,
            )
        }
        if (app.kind == EntryKind.PRIVATE_SPACE) {
            val repository = (context.applicationContext as NikeApp).appRepository
            if (app.componentName.className == PRIVATE_SPACE_UNLOCKED_CLASS)
                item(
                    stringResource(R.string.private_space_install_apps),
                    {
                        act {
                            if (!repository.openPrivateSpaceStore(context))
                                context.toast(R.string.private_space_install_failed)
                        }
                    },
                    leadingIcon = Icons.Filled.Add,
                )
            item(
                stringResource(R.string.private_space_settings),
                {
                    act {
                        if (!repository.openPrivateSpaceSettings(context))
                            context.toast(R.string.private_space_settings_failed)
                    }
                },
                leadingIcon = Icons.Filled.Settings,
            )
        }
        if (app.kind == EntryKind.SHORTCUT)
            item(
                stringResource(R.string.action_remove_shortcut),
                { act(onUnpin) },
                leadingIcon = Icons.Filled.LinkOff,
            )
        gap()
        item(
            stringResource(R.string.action_move_to_folder),
            { act(onMoveToFolder) },
            leadingIcon = Icons.Filled.Folder,
        )
        onRemoveFromFolder?.let { remove ->
            item(
                stringResource(R.string.home_folder_remove_app),
                { act(remove) },
                leadingIcon = Icons.Filled.LinkOff,
            )
        }
        item(
            stringResource(if (isHidden) R.string.applist_unhide else R.string.applist_hide),
            { act(onHide) },
            leadingIcon = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
        )
    }
}
