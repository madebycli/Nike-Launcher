// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.PRIVATE_SPACE_UNLOCKED_CLASS
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.kit.menu.TouchMenu

/** The long-press menu of an app row. */
@Composable
internal fun AppRowMenu(
    app: AppInfo,
    expanded: Boolean,
    offset: DpOffset,
    favoritesEditable: Boolean,
    isFavorite: Boolean,
    isHidden: Boolean,
    actions: AppRowActions,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    TouchMenu(expanded = expanded, offset = offset, onDismissRequest = onDismiss) {
        // Not offered while the favorites keep themselves: starring an app wrote to a list nothing
        // was drawing, so the app never appeared and nothing said why.
        if (favoritesEditable) {
            item(
                stringResource(if (isFavorite) R.string.applist_remove_favorite else R.string.applist_add_favorite),
                { onDismiss(); actions.onSetFavorite(!isFavorite) },
                leadingIcon = if (isFavorite) Icons.Filled.StarBorder else Icons.Filled.Star,
            )
        }
        item(stringResource(R.string.action_edit_icon_and_name), { onDismiss(); actions.onEdit() }, leadingIcon = Icons.Filled.Tune)
        // Only an installed app has a settings screen to open; a row that stands for something else,
        // a shortcut or the private space, would send the system looking for a package that is not there.
        if (app.kind == EntryKind.APP) {
            item(stringResource(R.string.action_app_info), { onDismiss(); actions.onAppInfo() }, leadingIcon = Icons.Filled.Info)
        }
        if (app.kind == EntryKind.PRIVATE_SPACE && app.componentName.className == PRIVATE_SPACE_UNLOCKED_CLASS) {
            item(
                stringResource(R.string.private_space_install_apps),
                {
                    onDismiss()
                    val repo = (context.applicationContext as NikeApp).appRepository
                    if (!repo.openPrivateSpaceStore(context)) context.toast(R.string.private_space_install_failed)
                },
                leadingIcon = Icons.Filled.Add,
            )
        }
        if (app.kind == EntryKind.PRIVATE_SPACE) {
            item(
                stringResource(R.string.private_space_settings),
                {
                    onDismiss()
                    val repo = (context.applicationContext as NikeApp).appRepository
                    if (!repo.openPrivateSpaceSettings(context)) context.toast(R.string.private_space_settings_failed)
                },
                leadingIcon = Icons.Filled.Settings,
            )
        }
        // Hands the shortcut back to the app that pinned it, which is the only way one ever leaves.
        if (app.kind == EntryKind.SHORTCUT) {
            item(stringResource(R.string.action_remove_shortcut), { onDismiss(); actions.onUnpin() }, leadingIcon = Icons.Filled.LinkOff)
        }
        item(stringResource(R.string.action_move_to_folder), { onDismiss(); actions.onMoveToFolder() }, leadingIcon = Icons.Filled.Folder)
        actions.onRemoveFromFolder?.let { remove ->
            item(stringResource(R.string.home_folder_remove_app), { onDismiss(); remove() }, leadingIcon = Icons.Filled.LinkOff)
        }
        item(
            stringResource(if (isHidden) R.string.applist_unhide else R.string.applist_hide),
            { onDismiss(); actions.onHide() },
            leadingIcon = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
        )
    }
}
