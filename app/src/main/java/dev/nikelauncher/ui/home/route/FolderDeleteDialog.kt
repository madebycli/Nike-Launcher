// SPDX-License-Identifier: GPL-3.0-or-later
// Taken over from Victoria Launcher 0.93.1 (#148).
package dev.nikelauncher.ui.home.route

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.Folder
import kotlinx.coroutines.launch

/**
 * Asks before a folder goes: it sits at the bottom of the menu where the other menus keep
 * settings, and a folder's arrangement and widgets cannot be had back.
 */
@Composable
internal fun FolderDeleteDialog(app: NikeApp, folder: Folder, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.home_folder_delete_confirm, folder.name)) },
        text = { Text(stringResource(R.string.home_folder_delete_confirm_body)) },
        confirmButton = {
            TextButton(onClick = {
                // Its widgets go with it, released so their apps stop updating them.
                folder.widgets.forEach { runCatching { app.widgetHost.deleteAppWidgetId(it.id) } }
                scope.launch { app.prefs.library.deleteFolder(folder.id) }
                onDone()
            }) { Text(stringResource(R.string.home_folder_delete)) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.action_cancel)) } },
    )
}
